package com.sanshare.smsgateway.sms

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsModule {
    @Binds
    @Singleton
    abstract fun bindSmsDispatcher(impl: AndroidSmsDispatcher): SmsDispatcher

    @Binds
    @Singleton
    abstract fun bindOutgoingSmsLifecycle(impl: OutgoingSmsStatusUpdater): OutgoingSmsLifecycle
}
