package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.RequestAuditLogRepository
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.SystemLogRepository
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGatewayStateRepository(
        impl: InMemoryGatewayStateRepository,
    ): GatewayStateRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSentSmsRepository(impl: SentSmsRepositoryImpl): SentSmsRepository

    @Binds
    @Singleton
    abstract fun bindReceivedSmsRepository(impl: ReceivedSmsRepositoryImpl): ReceivedSmsRepository

    @Binds
    @Singleton
    abstract fun bindWebhookAttemptRepository(impl: WebhookAttemptRepositoryImpl): WebhookAttemptRepository

    @Binds
    @Singleton
    abstract fun bindSystemLogRepository(impl: SystemLogRepositoryImpl): SystemLogRepository

    @Binds
    @Singleton
    abstract fun bindRequestAuditLogRepository(impl: RequestAuditLogRepositoryImpl): RequestAuditLogRepository
}
