package com.sanshare.smsgateway.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.network.NetworkAddressProvider
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.usecase.RunLowBatteryWebhookUseCase
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
    @Inject lateinit var runLowBatteryWebhookUseCase: RunLowBatteryWebhookUseCase
    @Inject lateinit var logger: AppLogger

    private val notificationFactory by lazy { GatewayNotificationFactory(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateMutex = Mutex()
    private var batteryReceiverRegistered = false
    private var lowBatteryEventSent = false

    override fun onCreate() {
        super.onCreate()
        notificationFactory.ensureChannel()
        registerBatteryReceiver()
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
        unregisterBatteryReceiver()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiverRegistered) return
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryReceiverRegistered = true
    }

    private fun unregisterBatteryReceiver() {
        if (!batteryReceiverRegistered) return
        runCatching { unregisterReceiver(batteryReceiver) }
        batteryReceiverRegistered = false
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return

            val percentage = ((level * 100f) / scale).toInt()
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val isLowBattery = percentage <= LOW_BATTERY_THRESHOLD_PERCENT

            if (!isLowBattery) {
                lowBatteryEventSent = false
                return
            }
            if (lowBatteryEventSent) return

            lowBatteryEventSent = true
            serviceScope.launch {
                val result = runCatching {
                    runLowBatteryWebhookUseCase(
                        batteryPercentage = percentage,
                        thresholdPercentage = LOW_BATTERY_THRESHOLD_PERCENT,
                        isCharging = isCharging,
                    )
                }.onFailure {
                    lowBatteryEventSent = false
                    logger.error("Battery", "Low-battery webhook dispatch failed", it)
                }.getOrNull() ?: return@launch

                if (result.success) {
                    logger.info("Battery", "Low-battery webhook sent at ${percentage}%")
                } else {
                    lowBatteryEventSent = false
                    logger.warning(
                        "Battery",
                        "Low-battery webhook failed at ${percentage}% with ${result.errorCode ?: "unknown_error"}",
                    )
                }
            }
        }
    }

    private fun updateNotification(notification: android.app.Notification) {
        NotificationManagerCompat.from(this).notify(AppConstants.GATEWAY_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ACTION_START = "com.sanshare.smsgateway.action.START"
        private const val ACTION_STOP = "com.sanshare.smsgateway.action.STOP"
        private const val LOW_BATTERY_THRESHOLD_PERCENT = 20

        fun startIntent(context: Context): Intent {
            return Intent(context, SmsGatewayService::class.java).setAction(ACTION_START)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, SmsGatewayService::class.java).setAction(ACTION_STOP)
        }
    }
}
