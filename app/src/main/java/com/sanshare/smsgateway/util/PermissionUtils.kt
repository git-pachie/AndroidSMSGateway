package com.sanshare.smsgateway.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sanshare.smsgateway.domain.model.GatewayPermissionState

object PermissionUtils {
    fun gatewayPermissionState(context: Context): GatewayPermissionState {
        return GatewayPermissionState(
            canSendSms = hasPermission(context, Manifest.permission.SEND_SMS),
            canReceiveSms = hasPermission(context, Manifest.permission.RECEIVE_SMS),
            canReadSms = hasPermission(context, Manifest.permission.READ_SMS),
            canPostNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(context, Manifest.permission.POST_NOTIFICATIONS),
            batteryOptimizationIgnored = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context),
        )
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
