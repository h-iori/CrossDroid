package com.ioristudios.crossdroid.backend.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID

class IdentityManager(private val context: Context) {
    private val keyStoreAlias = "CrossDroidIdentity"
    private val prefs: SharedPreferences = context.getSharedPreferences("CrossDroidSecurity", Context.MODE_PRIVATE)

    var deviceId: String
        private set
    var publicFingerprint: String = ""
        private set

    lateinit var certificate: X509Certificate
    lateinit var keyPair: KeyPair

    init {
        deviceId = prefs.getString("DeviceId", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("DeviceId", it).apply()
        }
        initializeIdentity()
    }

    private fun initializeIdentity() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(keyStoreAlias)) {
            generateIdentity()
        }

        val privateKey = keyStore.getKey(keyStoreAlias, null) as java.security.PrivateKey
        val cert = keyStore.getCertificate(keyStoreAlias) as X509Certificate
        val publicKey = cert.publicKey

        keyPair = KeyPair(publicKey, privateKey)
        certificate = cert
        publicFingerprint = calculateFingerprint(cert)
    }

    private fun generateIdentity() {
        // Generate RSA KeyPair in AndroidKeyStore
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
        ).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
         .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
         .setKeySize(2048)
         .build()

        kpg.initialize(parameterSpec)
        val kp = kpg.generateKeyPair()

        // Generate Self-Signed Certificate using BouncyCastle
        val issuer = X500Name("CN=CrossDroid")
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30) // 1 month ago
        val notAfter = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10) // 10 years

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            issuer,
            kp.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(kp.private)
        val holder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(holder)

        // Store Certificate back in KeyStore
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.setKeyEntry(keyStoreAlias, kp.private, null, arrayOf(cert))
    }

    private fun calculateFingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(cert.encoded)
        return hashBytes.joinToString("") { "%02X".format(it) }
    }
}
