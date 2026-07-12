package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivedSmsDao {
    @Insert
    suspend fun insert(entity: ReceivedSmsEntity): Long

    @Update
    suspend fun update(entity: ReceivedSmsEntity)

    @Query("SELECT * FROM received_sms WHERE id = :id")
    suspend fun getById(id: Long): ReceivedSmsEntity?

    @Query("SELECT * FROM received_sms WHERE id = :id")
    fun observeById(id: Long): Flow<ReceivedSmsEntity?>

    @Query("SELECT COUNT(*) FROM received_sms WHERE forwardStatus = :status")
    fun countByForwardStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM received_sms WHERE receivedAt >= :fromInclusive AND receivedAt < :toExclusive")
    suspend fun countReceivedBetween(fromInclusive: Long, toExclusive: Long): Int

    @Query(
        """
        SELECT * FROM received_sms
        WHERE messageFingerprint = :fingerprint
          AND receivedAt >= :windowStart
        ORDER BY receivedAt DESC
        LIMIT 1
        """,
    )
    suspend fun findRecentByFingerprint(
        fingerprint: String,
        windowStart: Long,
    ): ReceivedSmsEntity?

    @Query(
        """
        SELECT * FROM received_sms
        WHERE (:from IS NULL OR fromNumber LIKE '%' || :from || '%')
          AND (:forwardStatus IS NULL OR forwardStatus = :forwardStatus)
          AND (:dateFrom IS NULL OR receivedAt >= :dateFrom)
          AND (:dateTo IS NULL OR receivedAt <= :dateTo)
        ORDER BY
          CASE WHEN :sortDirection = 'ASC' THEN receivedAt END ASC,
          CASE WHEN :sortDirection = 'DESC' THEN receivedAt END DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun query(
        from: String?,
        forwardStatus: String?,
        dateFrom: Long?,
        dateTo: Long?,
        limit: Int,
        offset: Int,
        sortDirection: String,
    ): List<ReceivedSmsEntity>

    @Query(
        """
        SELECT * FROM received_sms
        WHERE (:from IS NULL OR fromNumber LIKE '%' || :from || '%')
          AND (:forwardStatus IS NULL OR forwardStatus = :forwardStatus)
          AND (:dateFrom IS NULL OR receivedAt >= :dateFrom)
          AND (:dateTo IS NULL OR receivedAt <= :dateTo)
        ORDER BY
          CASE WHEN :sortDirection = 'ASC' THEN receivedAt END ASC,
          CASE WHEN :sortDirection = 'DESC' THEN receivedAt END DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun observeQuery(
        from: String?,
        forwardStatus: String?,
        dateFrom: Long?,
        dateTo: Long?,
        limit: Int,
        offset: Int,
        sortDirection: String,
    ): Flow<List<ReceivedSmsEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM received_sms
        WHERE (:from IS NULL OR fromNumber LIKE '%' || :from || '%')
          AND (:forwardStatus IS NULL OR forwardStatus = :forwardStatus)
          AND (:dateFrom IS NULL OR receivedAt >= :dateFrom)
          AND (:dateTo IS NULL OR receivedAt <= :dateTo)
        """,
    )
    suspend fun countFiltered(
        from: String?,
        forwardStatus: String?,
        dateFrom: Long?,
        dateTo: Long?,
    ): Int
}
