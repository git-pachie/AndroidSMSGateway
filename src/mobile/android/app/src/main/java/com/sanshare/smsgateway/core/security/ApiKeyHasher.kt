package com.sanshare.smsgateway.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class ApiKeyHash(
    val encoded: String,
    val identifier: String,
)

class ApiKeyHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generateRawKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(rawKey: String): ApiKeyHash {
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        val digest = hmacSha256(salt, rawKey)
        val encodedSalt = Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
        val encodedDigest = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        val identifier = rawKey.takeLast(6)
        return ApiKeyHash("v1:$encodedSalt:$encodedDigest", identifier)
    }

    fun verify(rawKey: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 3 || parts[0] != "v1") return false
        return try {
            val salt = Base64.getUrlDecoder().decode(parts[1])
            val expected = Base64.getUrlDecoder().decode(parts[2])
            val actual = hmacSha256(salt, rawKey)
            MessageDigest.isEqual(expected, actual)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun hmacSha256(salt: ByteArray, rawKey: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(rawKey.toByteArray(Charsets.UTF_8))
    }
}
