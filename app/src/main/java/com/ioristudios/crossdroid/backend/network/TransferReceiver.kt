package com.ioristudios.crossdroid.backend.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.CompletableDeferred

class TransferReceiver(
    private val context: Context,
    private val downloadsDir: File,
    private val transferDao: com.ioristudios.crossdroid.backend.database.TransferRecordDao
) {
    private val _activeTransfers = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val activeTransfers: StateFlow<Map<String, TransferProgress>> = _activeTransfers

    data class IncomingRequest(
        val offer: TransferOfferPayload,
        val deviceName: String,
        val remoteFingerprint: String,
        val deferredResponse: CompletableDeferred<Boolean>
    )

    private val _incomingTransferRequest = MutableSharedFlow<IncomingRequest>(extraBufferCapacity = 10)
    val incomingTransferRequest: SharedFlow<IncomingRequest> = _incomingTransferRequest

    suspend fun receiveFile(
        session: SecureSession,
        offer: TransferOfferPayload
    ) = withContext(Dispatchers.IO) {
        var progress = TransferProgress(
            transferId = offer.TransferId,
            fileName = offer.FileName,
            totalBytes = offer.TotalBytes,
            bytesTransferred = 0,
            isIncoming = true,
            status = 1 // Receiving
        )
        updateProgress(progress)

        var success = false
        val destFile = File(downloadsDir, offer.FileName)
        try {
            // Wait for user interaction
            val deferred = CompletableDeferred<Boolean>()
            val emitted = _incomingTransferRequest.tryEmit(IncomingRequest(offer, "Incoming Device", session.remoteFingerprint, deferred))
            if (!emitted) {
                // If buffer is full, automatically reject
                deferred.complete(false)
            }
            val isAccepted = kotlinx.coroutines.withTimeoutOrNull(30_000) { deferred.await() } ?: false

            val acceptMsg = ProtocolMessage(
                Type = if (isAccepted) MessageType.TransferAccept else MessageType.TransferReject,
                PayloadJson = com.google.gson.Gson().toJson(
                    TransferAcceptPayload(
                        TransferId = offer.TransferId,
                        Accepted = isAccepted,
                        RequestedOffset = 0
                    ) as Any
                )
            )
            session.writeMessageAsync(acceptMsg, null)

            if (!isAccepted) {
                progress = progress.copy(status = 4) // Cancelled
                updateProgress(progress)
                return@withContext
            }

            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            FileOutputStream(destFile).use { fos ->
                var bytesReceived = 0L
                while (bytesReceived < offer.TotalBytes) {
                    val (chunkMsg, chunkData, _) = session.readMessageAsync()
                    if (chunkMsg.Type == MessageType.TransferCancel) {
                        progress = progress.copy(status = 4)
                        updateProgress(progress)
                        return@withContext
                    }
                    if (chunkMsg.Type == MessageType.FileChunk && chunkData != null) {
                        fos.write(chunkData)
                        bytesReceived += chunkData.size
                        progress = progress.copy(bytesTransferred = bytesReceived)
                        updateProgress(progress)
                    }
                }
            }
            success = true
            progress = progress.copy(bytesTransferred = offer.TotalBytes, status = 3)
            updateProgress(progress)
        } catch (e: Exception) {
            e.printStackTrace()
            progress = progress.copy(status = 5)
            updateProgress(progress)
        } finally {
            if (!success) {
                // Cleanup partial file if failed?
            }
            session.close()

            // Save to Database
            if (progress.status in listOf(3, 4, 5)) {
                transferDao.insertTransfer(
                    com.ioristudios.crossdroid.backend.database.TransferRecordEntity(
                        transferId = progress.transferId,
                        fileName = progress.fileName,
                        deviceId = "Unknown",
                        deviceName = "Unknown Device",
                        isIncoming = true,
                        totalBytes = progress.totalBytes,
                        bytesTransferred = progress.bytesTransferred,
                        status = progress.status,
                        destinationPath = destFile.absolutePath,
                        completedUtc = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun updateProgress(progress: TransferProgress) {
        val currentMap = _activeTransfers.value.toMutableMap()
        currentMap[progress.transferId] = progress
        _activeTransfers.value = currentMap
    }
}
