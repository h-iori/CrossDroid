package com.ioristudios.crossdroid.backend.network

import com.google.gson.Gson
import com.ioristudios.crossdroid.backend.security.IdentityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ServerSocket

class ConnectionListener(
    private val identityManager: IdentityManager,
    private val port: Int = 31928
) {
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _incomingOffers = MutableSharedFlow<Pair<SecureSession, TransferOfferPayload>>()
    val incomingOffers: SharedFlow<Pair<SecureSession, TransferOfferPayload>> = _incomingOffers

    private val _incomingPairRequests = MutableSharedFlow<Pair<SecureSession, PairRequestPayload>>()
    val incomingPairRequests: SharedFlow<Pair<SecureSession, PairRequestPayload>> = _incomingPairRequests

    private val gson = Gson()

    fun start() {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isActive) {
                    val socket = serverSocket!!.accept()
                    launch { handleClient(socket) }
                }
            } catch (e: Exception) {
                // Socket closed or port in use
            }
        }
    }

    fun stop() {
        scope.cancel()
        try { serverSocket?.close() } catch (e: Exception) {}
    }

    private suspend fun handleClient(socket: java.net.Socket) {
        val session = SecureSession(socket)
        try {
            session.authenticateAsServerAsync(identityManager)
            val (message, _, _) = session.readMessageAsync()

            when (message.Type) {
                MessageType.TransferOffer -> {
                    val payload = gson.fromJson(message.PayloadJson, TransferOfferPayload::class.java)
                    _incomingOffers.emit(Pair(session, payload))
                    // The backend will take over the session and close it when done
                }
                MessageType.PairRequest -> {
                    val payload = gson.fromJson(message.PayloadJson, PairRequestPayload::class.java)
                    _incomingPairRequests.emit(Pair(session, payload))
                    // The backend will take over the session
                }
                else -> {
                    session.close()
                }
            }
        } catch (e: Exception) {
            session.close()
        }
    }
}
