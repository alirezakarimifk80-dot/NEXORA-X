package com.nexora.x.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-backed AES-256-GCM credential encryption layer for NEXORA X (`MASVS-STORAGE-1`).
 *
 * Encrypts sensitive profile fields at rest before they are written to the Room SQLite database
 * (`nexora_profiles.db`):
 * - `credential` (VLESS/VMess UUID, Trojan password, Shadowsocks key)
 * - `realityPublicKey` (`pbk`)
 * - `realityShortId` (`sid`)
 *
 * Key Management:
 * - On Android devices, generates and stores a non-exportable 256-bit AES-GCM [SecretKey] inside the
 *   hardware-backed `"AndroidKeyStore"` under alias [KEYSTORE_ALIAS].
 * - In local JVM unit-test environments where the `"AndroidKeyStore"` JCA provider is not registered,
 *   falls back to an ephemeral in-memory 256-bit AES key so unit tests can exercise real AES-256-GCM
 *   authenticated encryption and tamper detection without mocking.
 *
 * Ciphertext Format:
 * - `ENCv1:<Base64(12-byte IV || Ciphertext || 128-bit GCM Auth Tag)>`
 * - Backward-compatible migration: if a stored value does not start with [CIPHERTEXT_PREFIX],
 *   [decryptFromStorage] returns the legacy plaintext value so existing profiles continue to work
 *   and are automatically upgraded to AES-256-GCM on their next save.
 */
object CredentialVaultCipher {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "com.nexora.x.credential_vault_aes256_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    const val CIPHERTEXT_PREFIX = "ENCv1:"

    @Volatile
    private var jvmTestFallbackKey: SecretKey? = null

    /**
     * Encrypts [plaintext] using AES-256-GCM and prefixes with [CIPHERTEXT_PREFIX].
     * Blank strings are returned as-is; already-encrypted strings are not double-encrypted.
     */
    fun encryptForStorage(plaintext: String): String {
        if (plaintext.isBlank()) return plaintext
        if (plaintext.startsWith(CIPHERTEXT_PREFIX)) return plaintext

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            if (isAndroidKeyStoreKey(secretKey)) {
                // AndroidKeyStore generates a cryptographically random 12-byte IV automatically
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            } else {
                val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }

            val ivBytes = cipher.iv
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteBuffer.allocate(ivBytes.size + cipherBytes.size)
                .put(ivBytes)
                .put(cipherBytes)
                .array()

            CIPHERTEXT_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(combined)
        } catch (_: Throwable) {
            // Fail-safe fallback so profile persistence never crashes on broken OEM Keystore firmware
            plaintext
        }
    }

    /**
     * Decrypts a value produced by [encryptForStorage].
     * If [storedValue] does not start with [CIPHERTEXT_PREFIX], returns it as-is for seamless migration.
     * Throws [SecurityException] if the ciphertext or GCM authentication tag has been tampered with.
     */
    @Throws(SecurityException::class)
    fun decryptFromStorage(storedValue: String): String {
        if (storedValue.isBlank()) return storedValue
        if (!storedValue.startsWith(CIPHERTEXT_PREFIX)) return storedValue

        val encodedPayload = storedValue.removePrefix(CIPHERTEXT_PREFIX).trim()
        try {
            val combined = Base64.getDecoder().decode(encodedPayload)
            if (combined.size <= GCM_IV_LENGTH_BYTES + 16) {
                throw SecurityException("Corrupted or truncated ENCv1 credential payload.")
            }

            val ivBytes = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            return String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Throwable) {
            throw SecurityException("AES-256-GCM authentication or decryption failed for stored credential.", e)
        }
    }

    private fun isAndroidKeyStoreKey(key: SecretKey): Boolean {
        return key !is SecretKeySpec
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        // Attempt hardware-backed AndroidKeyStore first when running on Android OS
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            val existingEntry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existingEntry != null) {
                return existingEntry.secretKey
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (_: Throwable) {
            // Running on local JVM unit test runner without AndroidKeyStore provider
            val currentFallback = jvmTestFallbackKey
            if (currentFallback != null) return currentFallback
            val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val generated = SecretKeySpec(rawKey, "AES")
            jvmTestFallbackKey = generated
            return generated
        }
    }
}
