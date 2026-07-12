package com.sanshare.smsgateway.sms

import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.validation.GatewayValidators
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.net.ConnectException
import java.net.URI
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class WebhookForwarder @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val logger: AppLogger,
) {
    private val json = Json {
        explicitNulls = false
        encodeDefaults = true
    }

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = READ_TIMEOUT_MILLIS
        }
    }

    suspend fun forward(request: WebhookDispatchRequest): WebhookDispatchResult {
        val settings = settingsRepository.getSettings()
        if (!settings.webhookEnabled) {
            return WebhookDispatchResult(
                success = false,
                responseCode = null,
                responseBodySummary = null,
                durationMs = 0,
                errorCode = "WEBHOOK_DISABLED",
                errorMessage = "Webhook forwarding is disabled",
                retryable = false,
                requestUrlSummary = null,
            )
        }

        val url = settings.webhookUrl?.trim().orEmpty()
        if (url.isBlank()) {
            return WebhookDispatchResult(
                success = false,
                responseCode = null,
                responseBodySummary = null,
                durationMs = 0,
                errorCode = "WEBHOOK_NOT_CONFIGURED",
                errorMessage = "Webhook URL is not configured",
                retryable = false,
                requestUrlSummary = null,
            )
        }

        val validation = GatewayValidators.webhookUrl(url, enabled = true, requireHttps = settings.requireHttpsWebhook)
        if (!validation.valid) {
            return WebhookDispatchResult(
                success = false,
                responseCode = null,
                responseBodySummary = null,
                durationMs = 0,
                errorCode = "INVALID_WEBHOOK_URL",
                errorMessage = validation.error ?: "Webhook URL is invalid",
                retryable = false,
                requestUrlSummary = safeUrlSummary(url),
            )
        }

        val requestBody = json.encodeToString(request.payload)
        val requestUrl = validation.normalized ?: url
        val webhookSecret = settingsRepository.getWebhookSecret()?.takeIf { it.isNotBlank() }
        val startedAt = System.currentTimeMillis()
        return try {
            val response = httpClient.post(requestUrl) {
                contentType(ContentType.Application.Json)
                header("X-SMS-Gateway-Device", settings.deviceId)
                header("X-SMS-Gateway-Event", request.payload.eventType)
                header("X-Request-ID", request.requestId.ifBlank { UUID.randomUUID().toString() })
                if (webhookSecret != null) {
                    header(HttpHeaders.Authorization, "Bearer $webhookSecret")
                }
                setBody(requestBody)
            }
            val durationMs = System.currentTimeMillis() - startedAt
            val responseBody = response.bodyAsText()
            val success = response.status.value in 200..299
            WebhookDispatchResult(
                success = success,
                responseCode = response.status.value,
                responseBodySummary = summarize(responseBody),
                durationMs = durationMs,
                errorCode = if (success) null else "WEBHOOK_HTTP_ERROR",
                errorMessage = if (success) null else "Webhook returned HTTP ${response.status.value}",
                retryable = !success && response.status.value.isRetryableHttpCode(),
                requestUrlSummary = safeUrlSummary(requestUrl),
            )
        } catch (ex: HttpRequestTimeoutException) {
            failure("WEBHOOK_TIMEOUT", "Webhook request timed out", true, requestUrl, startedAt)
        } catch (ex: java.net.SocketTimeoutException) {
            failure("WEBHOOK_TIMEOUT", "Webhook request timed out", true, requestUrl, startedAt)
        } catch (ex: UnknownHostException) {
            failure("WEBHOOK_UNREACHABLE", "Webhook host could not be resolved", true, requestUrl, startedAt)
        } catch (ex: ConnectException) {
            failure("WEBHOOK_UNREACHABLE", "Webhook connection failed", true, requestUrl, startedAt)
        } catch (ex: SSLException) {
            failure("WEBHOOK_TLS_ERROR", "Webhook TLS validation failed", false, requestUrl, startedAt)
        } catch (ex: Exception) {
            logger.warning("WEBHOOK", "Webhook delivery failed: ${ex::class.simpleName}")
            failure("WEBHOOK_UNREACHABLE", "Webhook request failed", true, requestUrl, startedAt)
        }
    }

    private fun failure(
        errorCode: String,
        errorMessage: String,
        retryable: Boolean,
        requestUrl: String,
        startedAt: Long,
    ): WebhookDispatchResult {
        return WebhookDispatchResult(
            success = false,
            responseCode = null,
            responseBodySummary = null,
            durationMs = System.currentTimeMillis() - startedAt,
            errorCode = errorCode,
            errorMessage = errorMessage,
            retryable = retryable,
            requestUrlSummary = safeUrlSummary(requestUrl),
        )
    }

    private fun safeUrlSummary(raw: String): String {
        val uri = runCatching { URI(raw) }.getOrNull()
        val host = uri?.host ?: return raw.take(120)
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        return "${uri.scheme}://$host$path".take(160)
    }

    private fun summarize(body: String?): String? = body?.replace(Regex("\\s+"), " ")?.trim()?.take(500)?.ifBlank { null }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 10_000L
        private const val READ_TIMEOUT_MILLIS = 15_000L
        private const val REQUEST_TIMEOUT_MILLIS = 20_000L
    }
}

internal fun Int.isRetryableHttpCode(): Boolean {
    return this == 408 || this == 429 || this in 500..599
}
