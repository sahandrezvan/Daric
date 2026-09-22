package com.example.core.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores PINs as a versioned salted PBKDF2 hash instead of plaintext.
 * Legacy plaintext values remain verifiable so existing users are not locked out;
 * the ViewModel upgrades them automatically after loading settings.
 */
object PinSecurity {
    private const val PREFIX = "v1"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun isEncoded(value: String): Boolean = value.startsWith("$PREFIX:")

    fun hash(pin: String): String {
        require(pin.length >= 4) { "PIN is too short" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = derive(pin, salt)
        return listOf(
            PREFIX,
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(derived, Base64.NO_WRAP)
        ).joinToString(":")
    }

    fun verify(pin: String, stored: String): Boolean {
        if (!isEncoded(stored)) {
            return MessageDigest.isEqual(
                pin.toByteArray(Charsets.UTF_8),
                stored.toByteArray(Charsets.UTF_8)
            )
        }

        val parts = stored.split(":")
        if (parts.size != 3 || parts[0] != PREFIX) return false

        return runCatching {
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val expected = Base64.decode(parts[2], Base64.NO_WRAP)
            MessageDigest.isEqual(expected, derive(pin, salt))
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
