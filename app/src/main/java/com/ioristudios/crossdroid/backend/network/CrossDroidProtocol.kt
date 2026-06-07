package com.ioristudios.crossdroid.backend.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class MessageType {
    PairRequest,
    PairResponse,
    TransferOffer,
    TransferAccept,
    TransferReject,
    TransferCancel,
    FileChunk
}

data class ProtocolMessage(
    var Type: MessageType = MessageType.PairRequest,
    var PayloadJson: String = ""
)

data class PairRequestPayload(
    var DeviceId: String = "",
    var DisplayName: String = "",
    var PublicKeyBase64: String = "",
    var Pin: String = ""
)

data class PairResponsePayload(
    var Accepted: Boolean = false,
    var PublicKeyBase64: String = ""
)

data class TransferOfferPayload(
    var TransferId: String = "",
    var FileName: String = "",
    var TotalBytes: Long = 0,
    var IsFolder: Boolean = false,
    var ItemCount: Int = 0,
    var Hash: String = "",
    var Offset: Long = 0
)

data class TransferAcceptPayload(
    var TransferId: String = "",
    var Accepted: Boolean = false,
    var RequestedOffset: Long = 0
)

data class FileChunkPayload(
    var TransferId: String = "",
    var RelativePath: String = "",
    var Offset: Long = 0
)

object ProtocolFramer {
    private val gson = Gson()

    suspend fun writeMessageAsync(
        stream: OutputStream,
        message: ProtocolMessage,
        binaryPayload: ByteArray?,
        binLenOverride: Int = -1
    ) = withContext(Dispatchers.IO) {
        val json = gson.toJson(message)
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val binLen = if (binLenOverride >= 0) binLenOverride else (binaryPayload?.size ?: 0)

        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(jsonBytes.size)
        header.putInt(binLen)

        stream.write(header.array())
        stream.write(jsonBytes)
        if (binaryPayload != null && binLen > 0) {
            stream.write(binaryPayload, 0, binLen)
        }
        stream.flush()
    }

    suspend fun readMessageAsync(
        stream: InputStream
    ): Triple<ProtocolMessage, ByteArray?, Int> = withContext(Dispatchers.IO) {
        val header = ByteArray(8)
        if (!tryReadExact(stream, header)) throw EOFException()

        val headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val jsonLen = headerBuffer.int
        val binLen = headerBuffer.int

        if (jsonLen < 0 || binLen < 0 || jsonLen > 1024 * 1024 || binLen > 1024 * 1024 * 5) {
            throw java.io.IOException("Message length invalid")
        }

        val jsonBytes = ByteArray(jsonLen)
        if (!tryReadExact(stream, jsonBytes)) throw EOFException()

        val json = String(jsonBytes, Charsets.UTF_8)
        val message = gson.fromJson(json, ProtocolMessage::class.java)
            ?: throw java.io.IOException("Invalid JSON message")

        var binaryPayload: ByteArray? = null
        if (binLen > 0) {
            binaryPayload = ByteArray(binLen)
            if (!tryReadExact(stream, binaryPayload)) throw EOFException()
        }

        Triple(message, binaryPayload, binLen)
    }

    private fun tryReadExact(stream: InputStream, buffer: ByteArray): Boolean {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = stream.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) return false
            totalRead += read
        }
        return true
    }
}
