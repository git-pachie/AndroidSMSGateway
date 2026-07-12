package com.sanshare.smsgateway.domain.repository

import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.data.local.entity.SmsSegmentEntity
import kotlinx.coroutines.flow.Flow

data class SentSmsQuery(
    val status: String? = null,
    val to: String? = null,
    val clientReference: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortDirection: String = "DESC",
)

data class SentSmsPage(
    val items: List<SentSmsEntity>,
    val total: Int,
)

data class ReceivedSmsQuery(
    val from: String? = null,
    val forwardStatus: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortDirection: String = "DESC",
)

data class ReceivedSmsPage(
    val items: List<ReceivedSmsEntity>,
    val total: Int,
)

data class WebhookAttemptQuery(
    val smsId: Long? = null,
    val success: Boolean? = null,
    val responseCode: Int? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortDirection: String = "DESC",
)

data class WebhookAttemptPage(
    val items: List<com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity>,
    val total: Int,
)

interface SentSmsRepository {
    suspend fun insert(entity: SentSmsEntity): Long
    suspend fun update(entity: SentSmsEntity)
    suspend fun getById(id: Long): SentSmsEntity?
    fun observeById(id: Long): Flow<SentSmsEntity?>
    suspend fun getLatestByClientReference(clientReference: String): SentSmsEntity?
    suspend fun saveSegments(messageId: Long, segments: List<SmsSegmentEntity>)
    suspend fun getSegments(messageId: Long): List<SmsSegmentEntity>
    suspend fun query(query: SentSmsQuery): SentSmsPage
    suspend fun countAcceptedTodayUtc(nowMillis: Long = System.currentTimeMillis()): Int
    fun countByStatus(status: String): Flow<Int>
}

interface ReceivedSmsRepository {
    suspend fun insert(entity: ReceivedSmsEntity): Long
    suspend fun update(entity: ReceivedSmsEntity)
    suspend fun getById(id: Long): ReceivedSmsEntity?
    fun observeById(id: Long): Flow<ReceivedSmsEntity?>
    suspend fun findRecentByFingerprint(fingerprint: String, windowStart: Long): ReceivedSmsEntity?
    suspend fun query(query: ReceivedSmsQuery): ReceivedSmsPage
    fun observeQuery(query: ReceivedSmsQuery): Flow<List<ReceivedSmsEntity>>
    fun countByForwardStatus(status: String): Flow<Int>
    suspend fun countReceivedTodayUtc(nowMillis: Long = System.currentTimeMillis()): Int
}

interface WebhookAttemptRepository {
    suspend fun insert(entity: com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity): Long
    suspend fun query(query: WebhookAttemptQuery): WebhookAttemptPage
    suspend fun listBySmsId(smsId: Long, limit: Int = 20): List<com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity>
    fun observeLatestAttemptedAt(success: Boolean): Flow<Long?>
}
interface SystemLogRepository
interface RequestAuditLogRepository
