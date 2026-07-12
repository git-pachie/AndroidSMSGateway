package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.domain.repository.RequestAuditLogRepository
import com.sanshare.smsgateway.domain.repository.SystemLogRepository
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookAttemptRepositoryImpl @Inject constructor() : WebhookAttemptRepository

@Singleton
class SystemLogRepositoryImpl @Inject constructor() : SystemLogRepository

@Singleton
class RequestAuditLogRepositoryImpl @Inject constructor() : RequestAuditLogRepository
