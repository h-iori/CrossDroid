package com.ioristudios.crossdroid.backend.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class PairingManager {
    private val secureRandom = SecureRandom()

    private val _currentPin = MutableStateFlow("")
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _pinExpiresUtc = MutableStateFlow(0L)
    val pinExpiresUtc: StateFlow<Long> = _pinExpiresUtc.asStateFlow()

    private var failedAttempts = 0

    val isPinValid: Boolean
        get() = _currentPin.value.isNotEmpty() && System.currentTimeMillis() < _pinExpiresUtc.value

    val pinRemainingTimeMillis: Long
        get() = if (isPinValid) _pinExpiresUtc.value - System.currentTimeMillis() else 0L

    fun getPairingUri(deviceId: String): String {
        var uri = "crossdroid://pair?id=$deviceId"
        if (isPinValid) {
            uri += "&pin=${_currentPin.value}"
        }
        return uri
    }

    fun generateTemporaryPin(): String {
        // Cryptographically secure 6-digit PIN
        val pin = (secureRandom.nextInt(900000) + 100000).toString() // Generates 100000 to 999999
        _currentPin.value = pin
        _pinExpiresUtc.value = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)
        failedAttempts = 0
        return pin
    }

    fun invalidatePin() {
        _currentPin.value = ""
        _pinExpiresUtc.value = 0L
        failedAttempts = 0
    }

    fun handleFailedAttempt() {
        failedAttempts++
        if (failedAttempts >= 5) {
            invalidatePin()
        }
    }

    fun checkAndAutoRegeneratePin() {
        if (!isPinValid) {
            generateTemporaryPin()
        }
    }
}
