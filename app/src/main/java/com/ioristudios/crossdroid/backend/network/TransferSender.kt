package com.ioristudios.crossdroid.backend.network

import android.content.Context
import com.ioristudios.crossdroid.backend.security.IdentityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TransferProgress(
    val transferId: String,
    val fileName: String,
    val totalBytes: Long,
    val bytesTransferred: Long,
    val isIncoming: Boolean,
    val status: Int // 0=Pending, 1=Sending/Receiving, 2=Paused, 3=Completed, 4=Cancelled, 5=Error
)

class TransferSender(
    private val context: Context,
    private val identityManager: IdentityManager,
    private val transferDao: com.ioristudios.crossdroid.backend.database.TransferRecordDao
) {
    private val _activeTransfers = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val activeTransfers: StateFlow<Map<String, TransferProgress>> = _activeTransfers

    suspend fun sendFile(
        deviceIp: InetAddress,
        devicePort: Int,
        deviceFingerprint: String,
        file: File,
        transferId: String,
        pin: String? = null
    ) = withContext(Dispatchers.IO) {
        var progress = TransferProgress(
            transferId = transferId,
            fileName = file.name,
            totalBytes = file.length(),
            bytesTransferred = 0,
            isIncoming = false,
            status = 0
        )
        updateProgress(progress)

        var socket: Socket? = null
        try {
            progress = progress.copy(status = 1)
            updateProgress(progress)

            val plainSocket = Socket()
            plainSocket.connect(InetSocketAddress(deviceIp, devicePort), 5000)
            plainSocket.soTimeout = 30000 // 30 second read timeout
            socket = plainSocket

            val secureSession = SecureSession(socket)
            secureSession.authenticateAsClientAsync(identityManager, deviceFingerprint)
            
            // If PIN is provided, send PairRequest first
            if (!pin.isNullOrEmpty()) {
                val pairReq = ProtocolMessage(
                    Type = MessageType.PairRequest,
                    PayloadJson = com.google.gson.Gson().toJson(PairRequestPayload(
                        DeviceId = identityManager.deviceId,
                        DisplayName = android.os.Build.MODEL,
                        PublicKeyBase64 = identityManager.publicFingerprint,
                        Pin = pin
                    ))
                )
                secureSession.writeMessageAsync(pairReq, null)
                
                val (pairResMsg, _, _) = secureSession.readMessageAsync()
                if (pairResMsg.Type == MessageType.PairResponse) {
                    val pairRes = com.google.gson.Gson().fromJson(pairResMsg.PayloadJson, PairResponsePayload::class.java)
                    if (!pairRes.Accepted) {
                        throw Exception("Pairing rejected by receiver")
                    }
                } else {
                    throw Exception("Unexpected message during pairing")
                }
            }

            val outStream = secureSession.outputStream
            val inStream = secureSession.inputStream

            // Send TransferOffer
            val offer = ProtocolMessage(
                Type = MessageType.TransferOffer,
                PayloadJson = com.google.gson.Gson().toJson(
                    TransferOfferPayload(
                        TransferId = transferId,
                        FileName = file.name,
                        TotalBytes = file.length(),
                        IsFolder = file.isDirectory,
                        ItemCount = 1,
                        Hash = "",
                        Offset = 0
                    ) as Any
                )
            )
            ProtocolFramer.writeMessageAsync(outStream, offer, null)

            // Wait for Accept or Reject
            val (responseMsg, _, _) = ProtocolFramer.readMessageAsync(inStream)
            if (responseMsg.Type == MessageType.TransferReject) {
                progress = progress.copy(status = 4)
                updateProgress(progress)
                return@withContext
            }
            if (responseMsg.Type != MessageType.TransferAccept) {
                progress = progress.copy(status = 5)
                updateProgress(progress)
                return@withContext
            }

            val acceptPayload = com.google.gson.Gson().fromJson(responseMsg.PayloadJson, TransferAcceptPayload::class.java)
            if (!acceptPayload.Accepted) {
                progress = progress.copy(status = 4)
                updateProgress(progress)
                return@withContext
            }

            var offset = acceptPayload.RequestedOffset
            val buffer = ByteArray(1024 * 64)
            FileInputStream(file).use { fis ->
                fis.skip(offset)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val chunkMsg = ProtocolMessage(
                        Type = MessageType.FileChunk,
                        PayloadJson = com.google.gson.Gson().toJson(
                            FileChunkPayload(
                                TransferId = transferId,
                                RelativePath = file.name,
                                Offset = offset
                            ) as Any
                        )
                    )
                    
                    val chunkData = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                    ProtocolFramer.writeMessageAsync(outStream, chunkMsg, chunkData, bytesRead)
                    
                    offset += bytesRead
                    progress = progress.copy(bytesTransferred = offset)
                    updateProgress(progress)
                }
            }

            progress = progress.copy(bytesTransferred = file.length(), status = 3)
            updateProgress(progress)

        } catch (e: Exception) {
            e.printStackTrace()
            progress = progress.copy(status = 5)
            updateProgress(progress)
        } finally {
            socket?.close()
            
            // Save to Database
            if (progress.status in listOf(3, 4, 5)) {
                transferDao.insertTransfer(
                    com.ioristudios.crossdroid.backend.database.TransferRecordEntity(
                        transferId = progress.transferId,
                        fileName = progress.fileName,
                        deviceId = deviceFingerprint,
                        deviceName = "Unknown Device",
                        isIncoming = false,
                        totalBytes = progress.totalBytes,
                        bytesTransferred = progress.bytesTransferred,
                        status = progress.status,
                        destinationPath = file.absolutePath,
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
