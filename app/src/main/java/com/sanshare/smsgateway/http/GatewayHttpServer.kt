package com.sanshare.smsgateway.http

import android.content.Context
import com.sanshare.smsgateway.BuildConfig
import com.sanshare.smsgateway.core.error.ErrorCode
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.validation.GatewayValidators
import com.sanshare.smsgateway.core.validation.PagingRequest
import com.sanshare.smsgateway.data.local.dao.RequestAuditLogDao
import com.sanshare.smsgateway.data.local.dao.SystemLogDao
import com.sanshare.smsgateway.data.local.entity.RequestAuditLogEntity
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.SettingsUpdate
import com.sanshare.smsgateway.util.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.BindException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GatewayHttpServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val receivedSmsRepository: ReceivedSmsRepository,
    private val systemLogDao: SystemLogDao,
    private val auditLogDao: RequestAuditLogDao,
    private val logger: AppLogger,
) {
    private val mutex = Mutex()
    private var engine: ApplicationEngine? = null
    private var boundPort: Int? = null

    suspend fun start(port: Int) = mutex.withLock {
        if (engine != null) return
        try {
            val newEngine = embeddedServer(Netty, host = "0.0.0.0", port = port) {
                configure()
            }
            withContext(Dispatchers.IO) {
                newEngine.start(wait = false)
            }
            engine = newEngine
            boundPort = port
        } catch (ex: BindException) {
            throw GatewayServerException(ErrorCode.PORT_IN_USE, "Port $port is already in use")
        } catch (ex: Exception) {
            throw GatewayServerException(ErrorCode.SERVER_START_FAILED, "Server failed to start")
        }
    }

    suspend fun stop() = mutex.withLock {
        val current = engine ?: return
        withContext(Dispatchers.IO) {
            current.stop(gracePeriodMillis = 500, timeoutMillis = 2_000)
        }
        engine = null
        boundPort = null
    }

    fun isRunning(): Boolean = engine != null
    fun port(): Int? = boundPort

    private fun Application.configure() {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; explicitNulls = false })
        }
        routing {
            get("/api/health") {
                handle(call, authRequired = false) {
                    val settings = settingsRepository.getSettings()
                    val permissions = PermissionUtils.gatewayPermissionState(context)
                    val degraded = !permissions.canSendSms || !permissions.canReceiveSms || !permissions.canPostNotifications
                    val pending = receivedSmsRepository.countByForwardStatus("PENDING")
                    call.respondSuccess(
                        HealthResponse(
                            status = if (!isRunning()) "stopped" else if (degraded) "degraded" else "running",
                            deviceId = settings.deviceId,
                            serverTime = Instant.now().toString(),
                            version = BuildConfig.VERSION_NAME,
                            smsPermission = permissions.canSendSms,
                            receiveSmsPermission = permissions.canReceiveSms,
                            notificationPermission = permissions.canPostNotifications,
                            batteryOptimizationDisabled = permissions.batteryOptimizationIgnored,
                            simAvailable = null,
                            webhookEnabled = settings.webhookEnabled,
                            pendingWebhookCount = kotlinx.coroutines.flow.first(pending),
                        ),
                    )
                }
            }
            get("/api/settings") {
                handle(call) {
                    call.respondSuccess(settingsRepository.getSettings().toSafeResponse())
                }
            }
            put("/api/settings") {
                handle(call) {
                    val request = call.receive<SettingsUpdateRequest>()
                    validateSettings(request)
                    val updated = settingsRepository.updateSettings(
                        SettingsUpdate(
                            deviceId = request.deviceId,
                            webhookUrl = request.webhookUrl,
                            webhookEnabled = request.webhookEnabled,
                            allowedPrefixes = request.allowedPrefixes,
                            rateLimitPerMinute = request.rateLimitPerMinute,
                            dailySmsLimitEnabled = request.dailySmsLimitEnabled,
                            dailySmsLimit = request.dailySmsLimit,
                            maxRetryCount = request.maxRetryCount,
                            retryBaseDelaySeconds = request.retryBaseDelaySeconds,
                            autoStartEnabled = request.autoStartEnabled,
                            requireHttpsWebhook = request.requireHttpsWebhook,
                        ),
                    )
                    call.respondSuccess(updated.toSafeResponse())
                }
            }
            post("/api/settings/webhook") {
                handle(call) {
                    val request = call.receive<WebhookSettingsRequest>()
                    val requireHttps = settingsRepository.getSettings().requireHttpsWebhook
                    val urlCheck = GatewayValidators.webhookUrl(request.webhookUrl, request.enabled == true, requireHttps)
                    if (!urlCheck.valid) throw GatewayServerException(ErrorCode.INVALID_WEBHOOK_URL, urlCheck.error ?: "Invalid webhook URL")
                    val updated = settingsRepository.updateSettings(
                        SettingsUpdate(
                            webhookUrl = request.webhookUrl,
                            webhookEnabled = request.enabled,
                            maxRetryCount = request.maxRetryCount,
                        ),
                    )
                    call.respondSuccess(updated.toSafeResponse())
                }
            }
            put("/api/settings/server") {
                handle(call) {
                    val request = call.receive<ServerSettingsRequest>()
                    val portCheck = GatewayValidators.serverPort(request.serverPort)
                    if (!portCheck.valid) throw GatewayServerException(ErrorCode.INVALID_REQUEST, portCheck.error ?: "Invalid port")
                    val updated = settingsRepository.updateServerPort(request.serverPort)
                    call.respondSuccess(RestartRequiredResponse(updated.toSafeResponse(), restartRequired = true))
                }
            }
            get("/api/logs/system") {
                handle(call) {
                    val paging = parsePaging(call)
                    val level = call.request.queryParameters["level"]
                    val category = call.request.queryParameters["category"]
                    val sort = sortDirection(call)
                    val items = systemLogDao.queryFiltered(level, category, null, null, paging.limit, paging.offset, sort)
                    val total = systemLogDao.countFiltered(level, category, null, null)
                    call.respondSuccess(PagedResponse(items.map { it.toDto() }, paging.limit, paging.offset, items.size, total))
                }
            }
            get("/api/logs/audit") {
                handle(call) {
                    val paging = parsePaging(call)
                    val sort = sortDirection(call)
                    val items = auditLogDao.queryFiltered(null, null, paging.limit, paging.offset, sort)
                    val total = auditLogDao.countFiltered(null, null)
                    call.respondSuccess(PagedResponse(items.map { it.toDto() }, paging.limit, paging.offset, items.size, total))
                }
            }
            post("/api/sms/send") { notReady(call, "send_sms") }
            get("/api/sms/status/{messageId}") { notReady(call, "sms_status") }
            get("/api/sms/sent") { notReady(call, "sent_sms") }
            get("/api/sms/inbox") { notReady(call, "sms_inbox") }
        }
    }

    private suspend fun notReady(call: ApplicationCall, feature: String) {
        handle(call) {
            call.respond(HttpStatusCode.NotImplemented, ApiSuccess(data = FeatureNotReadyResponse(feature)))
        }
    }

    private suspend fun handle(
        call: ApplicationCall,
        authRequired: Boolean = true,
        block: suspend () -> Unit,
    ) {
        val startedAt = System.currentTimeMillis()
        val requestId = RequestIds.sanitizeOrGenerate(call.request.headers["X-Request-ID"])
        var authenticated = false
        var responseCode = 200
        call.response.header("X-Request-ID", requestId)
        try {
            if (authRequired) {
                authenticated = authenticate(call)
            }
            block()
            responseCode = call.response.status()?.value ?: responseCode
        } catch (ex: GatewayServerException) {
            responseCode = ex.status.value
            call.respondError(ex.status, ex.code.name, ex.safeMessage, requestId)
        } catch (ex: Exception) {
            responseCode = HttpStatusCode.InternalServerError.value
            logger.error("HTTP", "Unhandled API error", ex)
            call.respondError(HttpStatusCode.InternalServerError, ErrorCode.INTERNAL_ERROR.name, "Internal server error", requestId)
        } finally {
            val duration = System.currentTimeMillis() - startedAt
            runCatching {
                auditLogDao.insert(
                    RequestAuditLogEntity(
                        method = call.request.httpMethod.value,
                        path = call.request.path(),
                        remoteAddress = call.request.local.remoteHost,
                        responseCode = responseCode,
                        durationMs = duration,
                        authenticated = authenticated,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    private suspend fun authenticate(call: ApplicationCall): Boolean {
        val header = call.request.headers["Authorization"]
            ?: throw GatewayServerException(ErrorCode.MISSING_API_KEY, "Missing API key", HttpStatusCode.Unauthorized)
        if (!header.startsWith("Bearer ")) {
            throw GatewayServerException(ErrorCode.INVALID_API_KEY, "Invalid API key", HttpStatusCode.Unauthorized)
        }
        val token = header.removePrefix("Bearer ").trim()
        if (token.isBlank() || !settingsRepository.verifyApiKey(token)) {
            throw GatewayServerException(ErrorCode.INVALID_API_KEY, "Invalid API key", HttpStatusCode.Unauthorized)
        }
        return true
    }

    private fun validateSettings(request: SettingsUpdateRequest) {
        request.webhookUrl?.let {
            val result = GatewayValidators.webhookUrl(it, request.webhookEnabled == true, request.requireHttpsWebhook ?: true)
            if (!result.valid) throw GatewayServerException(ErrorCode.INVALID_WEBHOOK_URL, result.error ?: "Invalid webhook URL")
        }
        request.rateLimitPerMinute?.let {
            if (it <= 0 || it > 10_000) throw GatewayServerException(ErrorCode.INVALID_REQUEST, "Invalid rate limit")
        }
        request.dailySmsLimit?.let {
            if (it <= 0) throw GatewayServerException(ErrorCode.INVALID_REQUEST, "Invalid daily limit")
        }
    }

    private fun parsePaging(call: ApplicationCall): PagingRequest {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()
        val offsetRaw = call.request.queryParameters["offset"]?.toIntOrNull()
        if (offsetRaw != null && offsetRaw < 0) {
            throw GatewayServerException(ErrorCode.INVALID_REQUEST, "Offset must be non-negative")
        }
        return PagingRequest.normalize(limit, offsetRaw)
    }

    private fun sortDirection(call: ApplicationCall): String {
        return if (call.request.queryParameters["sortDirection"]?.uppercase() == "ASC") "ASC" else "DESC"
    }

    private suspend fun <T> ApplicationCall.respondSuccess(data: T) {
        respond(ApiSuccess(data = data))
    }

    private suspend fun ApplicationCall.respondError(
        status: HttpStatusCode,
        code: String,
        message: String,
        requestId: String,
    ) {
        respond(
            status,
            ApiErrorEnvelope(
                error = ApiErrorBody(code = code, message = message),
                requestId = requestId,
                timestamp = Instant.now().toString(),
            ),
        )
    }
}

class GatewayServerException(
    val code: ErrorCode,
    val safeMessage: String,
    val status: HttpStatusCode = statusFor(code),
) : Exception(safeMessage)

private fun statusFor(code: ErrorCode): HttpStatusCode {
    return when (code) {
        ErrorCode.MISSING_API_KEY,
        ErrorCode.INVALID_API_KEY,
        -> HttpStatusCode.Unauthorized
        ErrorCode.RECORD_NOT_FOUND -> HttpStatusCode.NotFound
        ErrorCode.PORT_IN_USE -> HttpStatusCode.Conflict
        ErrorCode.SERVER_START_FAILED -> HttpStatusCode.ServiceUnavailable
        ErrorCode.FEATURE_NOT_READY -> HttpStatusCode.NotImplemented
        else -> HttpStatusCode.BadRequest
    }
}
