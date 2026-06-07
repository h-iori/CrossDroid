package com.ioristudios.crossdroid.backend.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.ioristudios.crossdroid.backend.security.PairingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiscoveredDevice(
    val deviceId: String,
    val name: String,
    val endpoint: String,
    val fingerprint: String,
    val trustState: Int = 0
)

class DiscoveryService(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: String,
    private val publicFingerprint: String,
    private val serverPort: Int,
    private val pairingManager: PairingManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private val serviceType = "_crossdroid._tcp."
    private val serviceName = "CrossDroid-$deviceId"

    fun start() {
        startAdvertising()
        startDiscovery()
    }

    fun stop() {
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
        } catch (e: Exception) {}
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {}
    }

    private fun startAdvertising(pinOverride: String? = null) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@DiscoveryService.serviceName
            serviceType = this@DiscoveryService.serviceType
            port = serverPort
            setAttribute("id", deviceId)
            setAttribute("name", deviceName)
            setAttribute("type", "android")
            setAttribute("fp", publicFingerprint)
            if (pinOverride != null) {
                setAttribute("Pin", pinOverride)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == serviceType && service.serviceName != serviceName) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val id = serviceInfo.attributes["id"]?.let { String(it) } ?: return
                            val name = serviceInfo.attributes["name"]?.let { String(it) } ?: "Unknown"
                            val fp = serviceInfo.attributes["fp"]?.let { String(it) } ?: return
                            val incomingPin = serviceInfo.attributes["Pin"]?.let { String(it) }
                            
                            val ip = serviceInfo.host?.hostAddress ?: return
                            val endpoint = "$ip:${serviceInfo.port}"

                            // PIN Search Validation
                            if (!incomingPin.isNullOrEmpty() && pairingManager != null) {
                                if (pairingManager.isPinValid) {
                                    if (incomingPin == pairingManager.currentPin.value) {
                                        // TODO: We could temporarily become visible here if we were hidden
                                    } else {
                                        pairingManager.handleFailedAttempt()
                                        Log.d("CrossDroid", "Invalid incoming PIN discovery attempt")
                                    }
                                }
                            }

                            val device = DiscoveredDevice(id, name, endpoint, fp)
                            val current = _discoveredDevices.value.toMutableList()
                            current.removeAll { it.deviceId == id }
                            current.add(device)
                            _discoveredDevices.value = current
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                val current = _discoveredDevices.value.toMutableList()
                current.removeAll { it.name == service.serviceName }
                _discoveredDevices.value = current
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun broadcastPinSearch(pin: String) {
        scope.launch {
            try {
                registrationListener?.let { nsdManager.unregisterService(it) }
            } catch (e: Exception) {}
            
            delay(500)
            startAdvertising(pin)
            
            delay(5000)
            
            try {
                registrationListener?.let { nsdManager.unregisterService(it) }
            } catch (e: Exception) {}
            
            delay(500)
            startAdvertising()
        }
    }
}
