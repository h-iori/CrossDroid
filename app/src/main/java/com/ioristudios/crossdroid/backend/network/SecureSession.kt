package com.ioristudios.crossdroid.backend.network

import com.ioristudios.crossdroid.backend.security.IdentityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SecureSession(private val socket: Socket) : AutoCloseable {

    var sslSocket: SSLSocket? = null
        private set

    var remoteFingerprint: String = ""
        private set

    val inputStream: InputStream
        get() = sslSocket?.inputStream ?: throw IllegalStateException("Session not established")
        
    val outputStream: OutputStream
        get() = sslSocket?.outputStream ?: throw IllegalStateException("Session not established")

    suspend fun authenticateAsServerAsync(identityManager: IdentityManager) = withContext(Dispatchers.IO) {
        val sslContext = createSslContext(identityManager, null)
        val factory = sslContext.socketFactory
        sslSocket = factory.createSocket(socket, socket.inetAddress.hostAddress, socket.port, true) as SSLSocket
        sslSocket!!.useClientMode = false
        sslSocket!!.needClientAuth = true
        sslSocket!!.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
        sslSocket!!.startHandshake()

        val certs = sslSocket!!.session.peerCertificates
        if (certs.isNotEmpty()) {
            remoteFingerprint = calculateFingerprint(certs[0] as X509Certificate)
        }
    }

    suspend fun authenticateAsClientAsync(identityManager: IdentityManager, expectedFingerprint: String) = withContext(Dispatchers.IO) {
        val sslContext = createSslContext(identityManager, expectedFingerprint)
        val factory = sslContext.socketFactory
        sslSocket = factory.createSocket(socket, socket.inetAddress.hostAddress, socket.port, true) as SSLSocket
        sslSocket!!.useClientMode = true
        sslSocket!!.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
        sslSocket!!.startHandshake()

        val certs = sslSocket!!.session.peerCertificates
        if (certs.isNotEmpty()) {
            remoteFingerprint = calculateFingerprint(certs[0] as X509Certificate)
        }
    }

    private fun createSslContext(identityManager: IdentityManager, expectedFingerprint: String?): SSLContext {
        // Create KeyManager from our IdentityManager's keyPair and cert
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setKeyEntry("CrossDroid", identityManager.keyPair.private, null, arrayOf(identityManager.certificate))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, null)

        // Custom TrustManager that accepts any self-signed cert but validates fingerprint if provided
        val trustManagers = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                validateChain(chain, expectedFingerprint)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                validateChain(chain, expectedFingerprint)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.keyManagers, trustManagers, SecureRandom())
        return sslContext
    }

    private fun validateChain(chain: Array<out X509Certificate>?, expectedFingerprint: String?) {
        if (chain.isNullOrEmpty()) throw java.security.cert.CertificateException("No certificate provided")
        val fingerprint = calculateFingerprint(chain[0])
        if (expectedFingerprint != null && !fingerprint.equals(expectedFingerprint, ignoreCase = true)) {
            throw java.security.cert.CertificateException("Fingerprint mismatch")
        }
    }

    private fun calculateFingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(cert.encoded)
        return hashBytes.joinToString("") { "%02X".format(it) }
    }

    suspend fun writeMessageAsync(message: ProtocolMessage, binaryPayload: ByteArray?, binLenOverride: Int = -1) {
        ProtocolFramer.writeMessageAsync(outputStream, message, binaryPayload, binLenOverride)
    }

    suspend fun readMessageAsync(): Triple<ProtocolMessage, ByteArray?, Int> {
        return ProtocolFramer.readMessageAsync(inputStream)
    }

    override fun close() {
        try { sslSocket?.close() } catch (e: Exception) {}
        try { socket.close() } catch (e: Exception) {}
    }
}
