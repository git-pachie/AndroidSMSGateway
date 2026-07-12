package com.sanshare.smsgateway.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.network.NetworkAddressProvider
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.http.GatewayHttpServer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@AndroidEntryPoint
class SmsGatewayService : Service() {
    @Inject lateinit var gatewayStateRepository: GatewayStateRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var gatewayHttpServer: GatewayHttpServer
    @Inject lateinit var networkAddressProvider: NetworkAddressProvider
    @Inject lateinit var logger: AppLogger

    private val notificationFactory by lazy { GatewayNotificationFactory(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        notificationFactory.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> startGatewayShell()
            ACTION_STOP -> stopGatewayShell()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startGatewayShell() {
        serviceScope.launch {
            stateMutex.withLock {
                val current = gatewayStateRepository.state.value
                if (current == GatewayServiceState.RUNNING || current == GatewayServiceState.STARTING) return@withLock

                val settings = settingsRepository.getSettings()
                gatewayStateRepository.setState(GatewayServiceState.STARTING)
                startForeground(
                    AppConstants.GATEWAY_NOTIFICATION_ID,
                    notificationFactory.startingNotification(settings.serverPort),
                )

                try {
                    gatewayHttpServer.start(settings.serverPort)
                    settingsRepository.setServerEnabled(true)
                    gatewayStateRepository.setState(GatewayServiceState.RUNNING)
                    val serverAddress = networkAddressProvider.baseUrl(settings.serverPort)
                    updateNotification(notificationFactory.runningNotification(serverAddress, settings.serverPort))
                    logger.info("Service", "Gateway server started on ${serverAddress ?: "port ${settings.serverPort}"}")
                } catch (ex: Exception) {
                    settingsRepository.setServerEnabled(false)
                    gatewayStateRepository.setState(GatewayServiceState.ERROR)
                    val safeMessage = ex.message?.takeIf { it.isNotBlank() } ?: "Server startup failed"
                    updateNotification(notificationFactory.errorNotification(safeMessage))
                    logger.error("Service", "Gateway server failed to start", ex)
                }
            }
        }
    }

    private fun stopGatewayShell() {
        serviceScope.launch {
            stateMutex.withLock {
                val current = gatewayStateRepository.state.value
                if (current == GatewayServiceState.STOPPED || current == GatewayServiceState.STOPPING) {
                    ServiceCompat.stopForeground(this@SmsGatewayService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@withLock
                }

                gatewayStateRepository.setState(GatewayServiceState.STOPPING)
                runCatching { gatewayHttpServer.stop() }
                    .onFailure { logger.error("Service", "Gateway server stop encountered an error", it) }
                settingsRepository.setServerEnabled(false)
                gatewayStateRepository.setState(GatewayServiceState.STOPPED)
                logger.info("Service", "Gateway server stopped")
                ServiceCompat.stopForeground(this@SmsGatewayService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateNotification(notification: android.app.Notification) {
        NotificationManagerCompat.from(this).notify(AppConstants.GATEWAY_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ACTION_START = "com.sanshare.smsgateway.action.START"
        private const val ACTION_STOP = "com.sanshare.smsgateway.action.STOP"

        fun startIntent(context: Context): Intent {
            return Intent(context, SmsGatewayService::class.java).setAction(ACTION_START)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, SmsGatewayService::class.java).setAction(ACTION_STOP)
        }
    }
}
