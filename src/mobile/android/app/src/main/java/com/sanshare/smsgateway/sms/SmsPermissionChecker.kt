package com.sanshare.smsgateway.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface SmsPermissionChecker {
    fun canSendSms(): Boolean
}

@Singleton
class AndroidSmsPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsPermissionChecker {
    override fun canSendSms(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsPermissionModule {
    @Binds
    @Singleton
    abstract fun bindSmsPermissionChecker(impl: AndroidSmsPermissionChecker): SmsPermissionChecker
}
