package com.sanshare.smsgateway.domain.repository

import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import kotlinx.coroutines.flow.Flow

interface SentSmsRepository {
    suspend fun insert(entity: SentSmsEntity): Long
    suspend fun getById(id: Long): SentSmsEntity?
    suspend fun countAcceptedTodayUtc(nowMillis: Long = System.currentTimeMillis()): Int
    fun countByStatus(status: String): Flow<Int>
}

interface ReceivedSmsRepository {
    suspend fun insert(entity: ReceivedSmsEntity): Long
    suspend fun getById(id: Long): ReceivedSmsEntity?
    fun countByForwardStatus(status: String): Flow<Int>
    suspend fun countReceivedTodayUtc(nowMillis: Long = System.currentTimeMillis()): Int
}

interface WebhookAttemptRepository
interface SystemLogRepository
interface RequestAuditLogRepository
