package com.sanshare.smsgateway.http

import java.security.SecureRandom
import java.util.Base64

object RequestIds {
    private val secureRandom = SecureRandom()
    private val allowed = Regex("^[A-Za-z0-9._:-]{1,80}$")

    fun sanitizeOrGenerate(value: String?): String {
        val candidate = value?.trim()
        if (!candidate.isNullOrBlank() && allowed.matches(candidate)) return candidate
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
