package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.data.local.dao.WebhookAttemptDao
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity
import com.sanshare.smsgateway.domain.repository.RequestAuditLogRepository
import com.sanshare.smsgateway.domain.repository.SystemLogRepository
import com.sanshare.smsgateway.domain.repository.WebhookAttemptPage
import com.sanshare.smsgateway.domain.repository.WebhookAttemptQuery
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookAttemptRepositoryImpl @Inject constructor(
    private val webhookAttemptDao: WebhookAttemptDao,
) : WebhookAttemptRepository {
    override suspend fun insert(entity: WebhookAttemptEntity): Long = webhookAttemptDao.insert(entity)

    override suspend fun query(query: WebhookAttemptQuery): WebhookAttemptPage {
        val items = webhookAttemptDao.query(
            smsId = query.smsId,
            success = query.success,
            responseCode = query.responseCode,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
            limit = query.limit,
            offset = query.offset,
            sortDirection = query.sortDirection,
        )
        val total = webhookAttemptDao.countFiltered(
            smsId = query.smsId,
            success = query.success,
            responseCode = query.responseCode,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
        )
        return WebhookAttemptPage(items = items, total = total)
    }

    override suspend fun listBySmsId(smsId: Long, limit: Int): List<WebhookAttemptEntity> {
        return webhookAttemptDao.listBySmsId(smsId, limit)
    }

    override fun observeLatestAttemptedAt(success: Boolean): Flow<Long?> {
        return webhookAttemptDao.observeLatestAttemptedAt(success)
    }
}

@Singleton
class SystemLogRepositoryImpl @Inject constructor() : SystemLogRepository

@Singleton
class RequestAuditLogRepositoryImpl @Inject constructor() : RequestAuditLogRepository
