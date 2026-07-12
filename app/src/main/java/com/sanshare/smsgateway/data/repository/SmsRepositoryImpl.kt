package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.data.local.dao.ReceivedSmsDao
import com.sanshare.smsgateway.data.local.dao.SentSmsDao
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentSmsRepositoryImpl @Inject constructor(
    private val sentSmsDao: SentSmsDao,
) : SentSmsRepository {
    override suspend fun insert(entity: SentSmsEntity): Long = sentSmsDao.insert(entity)

    override suspend fun getById(id: Long): SentSmsEntity? = sentSmsDao.getById(id)

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

    override suspend fun getById(id: Long): ReceivedSmsEntity? = receivedSmsDao.getById(id)

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
