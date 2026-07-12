package com.sanshare.smsgateway.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sanshare.smsgateway.MainActivity
import com.sanshare.smsgateway.R
import com.sanshare.smsgateway.core.constants.AppConstants

class GatewayNotificationFactory(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            AppConstants.NOTIFICATION_CHANNEL_GATEWAY,
            context.getString(R.string.notification_channel_gateway),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_gateway_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun startingNotification(port: Int): Notification {
        return buildNotification(
            title = "SMS Gateway Starting",
            body = "Starting HTTP server on port $port.",
            ongoing = true,
        )
    }

    fun runningNotification(serverAddress: String?, port: Int): Notification {
        val body = serverAddress ?: "Server running on port $port"
        return buildNotification(
            title = "SMS Gateway Running",
            body = "Server: $body",
            ongoing = true,
        )
    }

    fun errorNotification(message: String): Notification {
        return buildNotification(
            title = "SMS Gateway Error",
            body = message,
            ongoing = false,
        )
    }

    private fun buildNotification(
        title: String,
        body: String,
        ongoing: Boolean,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            SmsGatewayService.stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_GATEWAY)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }
}
