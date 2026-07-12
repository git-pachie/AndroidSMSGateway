package com.sanshare.smsgateway.service

import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

interface GatewayServiceStarter {
    fun start()
}

@Singleton
class AndroidGatewayServiceStarter @Inject constructor(
    @ApplicationContext private val context: Context,
) : GatewayServiceStarter {
    override fun start() {
        ContextCompat.startForegroundService(context, SmsGatewayService.startIntent(context))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GatewayServiceStarterModule {
    @Binds
    @Singleton
    abstract fun bindGatewayServiceStarter(
        impl: AndroidGatewayServiceStarter,
    ): GatewayServiceStarter
}
