package com.ioristudios.crossdroid.backend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ioristudios.crossdroid.backend.database.AppDatabase
import com.ioristudios.crossdroid.backend.database.SettingsRepository
import com.ioristudios.crossdroid.backend.network.ConnectionListener
import com.ioristudios.crossdroid.backend.network.DiscoveryService
import com.ioristudios.crossdroid.backend.network.TransferReceiver
import com.ioristudios.crossdroid.backend.network.TransferSender
import com.ioristudios.crossdroid.backend.network.TransferOfferPayload
import com.ioristudios.crossdroid.backend.security.IdentityManager
import java.io.File
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CrossDroidBackendService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    lateinit var identityManager: IdentityManager
    lateinit var database: AppDatabase
    lateinit var settingsRepository: SettingsRepository
    lateinit var discoveryService: DiscoveryService
    lateinit var connectionListener: ConnectionListener
    lateinit var transferSender: TransferSender
    lateinit var transferReceiver: TransferReceiver

    private val _activeTransfers = MutableStateFlow<Map<String, com.ioristudios.crossdroid.backend.network.TransferProgress>>(emptyMap())
    val activeTransfers: StateFlow<Map<String, com.ioristudios.crossdroid.backend.network.TransferProgress>> = _activeTransfers

    val transferHistory: kotlinx.coroutines.flow.Flow<List<com.ioristudios.crossdroid.backend.database.TransferRecordEntity>>
        get() = database.transferDao().getAllTransfers()

    val incomingTransferRequest: kotlinx.coroutines.flow.SharedFlow<TransferReceiver.IncomingRequest>
        get() = transferReceiver.incomingTransferRequest

    inner class LocalBinder : Binder() {
        fun getService(): CrossDroidBackendService = this@CrossDroidBackendService
    }


    lateinit var pairingManager: com.ioristudios.crossdroid.backend.security.PairingManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        identityManager = IdentityManager(this)
        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        pairingManager = com.ioristudios.crossdroid.backend.security.PairingManager()
        
        discoveryService = DiscoveryService(
            context = this,
            deviceId = identityManager.deviceId,
            deviceName = android.os.Build.MODEL,
            publicFingerprint = identityManager.publicFingerprint,
            serverPort = 31928,
            pairingManager = pairingManager
        )
        
        connectionListener = ConnectionListener(identityManager, 31928)
        transferSender = TransferSender(this, identityManager, database.transferDao())
        
        val downloadsFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CrossDroid")
        transferReceiver = TransferReceiver(this, downloadsFolder, database.transferDao())

        scope.launch {
            kotlinx.coroutines.flow.combine(
                transferSender.activeTransfers,
                transferReceiver.activeTransfers
            ) { sends, receives ->
                sends + receives
            }.collect { combined ->
                _activeTransfers.value = combined
            }
        }

        startForegroundService()

        discoveryService.start()
        connectionListener.start()

        observeIncomingConnections()
    }

    private fun startForegroundService() {
        val channelId = "crossdroid_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CrossDroid Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CrossDroid")
            .setContentText("Listening for incoming transfers on the local network...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun observeIncomingConnections() {
        scope.launch {
            connectionListener.incomingOffers.collect { (session, offer) ->
                scope.launch {
                    transferReceiver.receiveFile(session, offer)
                }
            }
        }
        scope.launch {
            connectionListener.incomingPairRequests.collect { (session, payload) ->
                var accepted = false
                if (payload.Pin.isNotEmpty() && pairingManager.isPinValid) {
                    if (payload.Pin == pairingManager.currentPin.value) {
                        accepted = true
                    } else {
                        pairingManager.handleFailedAttempt()
                    }
                }
                
                val response = com.ioristudios.crossdroid.backend.network.ProtocolMessage(
                    Type = com.ioristudios.crossdroid.backend.network.MessageType.PairResponse,
                    PayloadJson = com.google.gson.Gson().toJson(
                        com.ioristudios.crossdroid.backend.network.PairResponsePayload(
                            Accepted = accepted,
                            PublicKeyBase64 = identityManager.publicFingerprint
                        )
                    )
                )
                session.writeMessageAsync(response, null)
                
                if (!accepted) {
                    session.close()
                } else {
                    // Start listening for TransferOffer on this same authenticated session
                    val (message, _, _) = session.readMessageAsync()
                    if (message.Type == com.ioristudios.crossdroid.backend.network.MessageType.TransferOffer) {
                        val offer = com.google.gson.Gson().fromJson(message.PayloadJson, com.ioristudios.crossdroid.backend.network.TransferOfferPayload::class.java)
                        scope.launch {
                            transferReceiver.receiveFile(session, offer)
                        }
                    } else {
                        session.close()
                    }
                }
            }
        }
    }

    fun sendFiles(deviceIp: java.net.InetAddress, devicePort: Int, deviceFingerprint: String, files: List<File>, pin: String? = null) {
        scope.launch {
            for (file in files) {
                transferSender.sendFile(deviceIp, devicePort, deviceFingerprint, file, java.util.UUID.randomUUID().toString(), pin)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryService.stop()
        connectionListener.stop()
        scope.cancel()
    }
}
