package com.sanshare.smsgateway.core.logging

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface AppLogger {
    fun debug(category: String, message: String)
    fun info(category: String, message: String)
    fun warning(category: String, message: String)
    fun error(category: String, message: String, throwable: Throwable? = null)
}

@Singleton
class LogcatAppLogger @Inject constructor() : AppLogger {
    override fun debug(category: String, message: String) {
        Log.d(tag(category), sanitize(message))
    }

    override fun info(category: String, message: String) {
        Log.i(tag(category), sanitize(message))
    }

    override fun warning(category: String, message: String) {
        Log.w(tag(category), sanitize(message))
    }

    override fun error(category: String, message: String, throwable: Throwable?) {
        Log.e(tag(category), sanitize(message), throwable)
    }

    private fun tag(category: String): String = "SmsGateway:${category.take(16)}"

    private fun sanitize(value: String): String {
        return value
            .replace(Regex("(?i)bearer\\s+[a-z0-9._~+/=-]+"), "Bearer [REDACTED]")
            .replace(Regex("(?i)(api[_ -]?key|webhook[_ -]?secret)=\\S+"), "$1=[REDACTED]")
            .take(1_000)
    }
}
