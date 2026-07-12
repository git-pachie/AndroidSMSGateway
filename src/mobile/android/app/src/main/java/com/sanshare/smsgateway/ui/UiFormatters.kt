package com.sanshare.smsgateway.ui

import android.text.format.DateUtils
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object UiFormatters {
    private val localDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())

    fun localTimestamp(epochMillis: Long?): String {
        if (epochMillis == null) return "n/a"
        return localDateTimeFormatter.format(Instant.ofEpochMilli(epochMillis))
    }

    fun relativeTime(epochMillis: Long?): String {
        if (epochMillis == null) return "n/a"
        return DateUtils.getRelativeTimeSpanString(
            epochMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }

    fun phonePreview(value: String, reveal: Int = 3): String {
        val trimmed = value.trim()
        if (trimmed.length <= reveal * 2) return trimmed
        return "${trimmed.take(reveal)}...${trimmed.takeLast(reveal)}"
    }

    fun messagePreview(value: String, max: Int = 72): String {
        val normalized = value.replace(Regex("\\s+"), " ").trim()
        return if (normalized.length <= max) normalized else normalized.take(max - 1) + "…"
    }

    fun urlPreview(raw: String?): String {
        if (raw.isNullOrBlank()) return "Not configured"
        val uri = runCatching { URI(raw) }.getOrNull() ?: return raw.take(48)
        val path = uri.rawPath?.takeIf { it.isNotBlank() && it != "/" }.orEmpty()
        return "${uri.scheme}://${uri.host}$path".take(60)
    }

    fun duration(value: Long?): String {
        if (value == null) return "n/a"
        if (value < 1_000L) return "${value}ms"
        val duration = Duration.ofMillis(value)
        val seconds = duration.seconds
        val minutes = seconds / 60
        return if (minutes > 0) "${minutes}m ${seconds % 60}s" else "${seconds}s"
    }
}
