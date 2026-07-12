package com.sanshare.smsgateway.core.logging

import android.util.Log
import com.sanshare.smsgateway.data.local.dao.SystemLogDao
import com.sanshare.smsgateway.data.local.entity.SystemLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbBackedAppLogger @Inject constructor(
    private val systemLogDao: SystemLogDao,
) : AppLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun debug(category: String, message: String) = write("DEBUG", category, message, null)
    override fun info(category: String, message: String) = write("INFO", category, message, null)
    override fun warning(category: String, message: String) = write("WARNING", category, message, null)
    override fun error(category: String, message: String, throwable: Throwable?) = write("ERROR", category, message, throwable)

    private fun write(level: String, category: String, message: String, throwable: Throwable?) {
        val sanitized = sanitize(message)
        val tag = "SmsGateway:${category.take(16)}"
        when (level) {
            "DEBUG" -> Log.d(tag, sanitized)
            "INFO" -> Log.i(tag, sanitized)
            "WARNING" -> Log.w(tag, sanitized)
            else -> Log.e(tag, sanitized, throwable)
        }
        scope.launch {
            runCatching {
                systemLogDao.insert(
                    SystemLogEntity(
                        level = level,
                        category = category.take(64),
                        eventCode = null,
                        message = sanitized.take(500),
                        details = throwable?.javaClass?.simpleName?.take(200),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }.onFailure {
                Log.w("SmsGateway:Logger", "Database log write failed")
            }
        }
    }

    private fun sanitize(value: String): String {
        return value
            .replace(Regex("(?i)bearer\\s+[a-z0-9._~+/=-]+"), "Bearer [REDACTED]")
            .replace(Regex("(?i)(api[_ -]?key|webhook[_ -]?secret)=\\S+"), "$1=[REDACTED]")
            .take(1_000)
    }
}
