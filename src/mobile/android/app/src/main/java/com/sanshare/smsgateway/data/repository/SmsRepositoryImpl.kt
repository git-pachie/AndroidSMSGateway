package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.data.local.dao.ReceivedSmsDao
import com.sanshare.smsgateway.data.local.dao.SentSmsDao
import com.sanshare.smsgateway.data.local.dao.SmsSegmentDao
import com.sanshare.smsgateway.data.local.entity.SmsSegmentEntity
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.ReceivedSmsPage
import com.sanshare.smsgateway.domain.repository.ReceivedSmsQuery
import com.sanshare.smsgateway.domain.repository.SentSmsPage
import com.sanshare.smsgateway.domain.repository.SentSmsQuery
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentSmsRepositoryImpl @Inject constructor(
    private val sentSmsDao: SentSmsDao,
    private val smsSegmentDao: SmsSegmentDao,
) : SentSmsRepository {
    override suspend fun insert(entity: SentSmsEntity): Long = sentSmsDao.insert(entity)

    override suspend fun update(entity: SentSmsEntity) = sentSmsDao.update(entity)

    override suspend fun getById(id: Long): SentSmsEntity? = sentSmsDao.getById(id)

    override fun observeById(id: Long): Flow<SentSmsEntity?> = sentSmsDao.observeById(id)

    override suspend fun getLatestByClientReference(clientReference: String): SentSmsEntity? {
        return sentSmsDao.getLatestByClientReference(clientReference)
    }

    override suspend fun saveSegments(messageId: Long, segments: List<SmsSegmentEntity>) {
        smsSegmentDao.insertAll(segments.map { it.copy(sentSmsId = messageId) })
    }

    override suspend fun getSegments(messageId: Long): List<SmsSegmentEntity> = smsSegmentDao.getBySentSmsId(messageId)

    override suspend fun query(query: SentSmsQuery): SentSmsPage {
        val items = sentSmsDao.query(
            status = query.status,
            to = query.to,
            clientReference = query.clientReference,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
            limit = query.limit,
            offset = query.offset,
            sortDirection = query.sortDirection,
        )
        val total = sentSmsDao.countFiltered(
            status = query.status,
            to = query.to,
            clientReference = query.clientReference,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
        )
        return SentSmsPage(items = items, total = total)
    }

    override suspend fun countAcceptedTodayUtc(nowMillis: Long): Int {
        val (start, end) = utcDayBounds(nowMillis)
        return sentSmsDao.countCreatedBetween(start, end)
    }

    override fun countByStatus(status: String): Flow<Int> = sentSmsDao.countByStatus(status)
}

@Singleton
class ReceivedSmsRepositoryImpl @Inject constructor(
    private val receivedSmsDao: ReceivedSmsDao,
) : ReceivedSmsRepository {
    override suspend fun insert(entity: ReceivedSmsEntity): Long = receivedSmsDao.insert(entity)

    override suspend fun update(entity: ReceivedSmsEntity) = receivedSmsDao.update(entity)

    override suspend fun getById(id: Long): ReceivedSmsEntity? = receivedSmsDao.getById(id)

    override fun observeById(id: Long): Flow<ReceivedSmsEntity?> = receivedSmsDao.observeById(id)

    override suspend fun findRecentByFingerprint(fingerprint: String, windowStart: Long): ReceivedSmsEntity? {
        return receivedSmsDao.findRecentByFingerprint(fingerprint, windowStart)
    }

    override suspend fun query(query: ReceivedSmsQuery): ReceivedSmsPage {
        val items = receivedSmsDao.query(
            from = query.from,
            forwardStatus = query.forwardStatus,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
            limit = query.limit,
            offset = query.offset,
            sortDirection = query.sortDirection,
        )
        val total = receivedSmsDao.countFiltered(
            from = query.from,
            forwardStatus = query.forwardStatus,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
        )
        return ReceivedSmsPage(items = items, total = total)
    }

    override fun observeQuery(query: ReceivedSmsQuery): Flow<List<ReceivedSmsEntity>> {
        return receivedSmsDao.observeQuery(
            from = query.from,
            forwardStatus = query.forwardStatus,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
            limit = query.limit,
            offset = query.offset,
            sortDirection = query.sortDirection,
        )
    }

    override fun countByForwardStatus(status: String): Flow<Int> = receivedSmsDao.countByForwardStatus(status)

    override suspend fun countReceivedTodayUtc(nowMillis: Long): Int {
        val (start, end) = utcDayBounds(nowMillis)
        return receivedSmsDao.countReceivedBetween(start, end)
    }
}

private fun utcDayBounds(nowMillis: Long): Pair<Long, Long> {
    val date = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val start = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    return start to end
}
