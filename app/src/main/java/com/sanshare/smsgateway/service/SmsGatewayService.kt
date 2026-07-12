package com.sanshare.smsgateway.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsGatewayService : Service() {
    @Inject lateinit var gatewayStateRepository: GatewayStateRepository
    @Inject lateinit var logger: AppLogger

    private val notificationFactory by lazy { GatewayNotificationFactory(this) }

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
        val current = gatewayStateRepository.state.value
        if (current == GatewayServiceState.RUNNING || current == GatewayServiceState.STARTING) return
        gatewayStateRepository.setState(GatewayServiceState.STARTING)
        startForeground(AppConstants.GATEWAY_NOTIFICATION_ID, notificationFactory.runningNotification())
        gatewayStateRepository.setState(GatewayServiceState.RUNNING)
        logger.info("Service", "Gateway foreground service shell started")
    }

    private fun stopGatewayShell() {
        gatewayStateRepository.setState(GatewayServiceState.STOPPING)
        logger.info("Service", "Gateway foreground service shell stopped")
        gatewayStateRepository.setState(GatewayServiceState.STOPPED)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
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
