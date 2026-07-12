package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentSmsDao {
    @Insert
    suspend fun insert(entity: SentSmsEntity): Long

    @Update
    suspend fun update(entity: SentSmsEntity)

    @Query("SELECT * FROM sent_sms WHERE id = :id")
    suspend fun getById(id: Long): SentSmsEntity?

    @Query("SELECT COUNT(*) FROM sent_sms WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sent_sms WHERE createdAt >= :fromInclusive AND createdAt < :toExclusive")
    suspend fun countCreatedBetween(fromInclusive: Long, toExclusive: Long): Int

    @Query(
        """
        SELECT * FROM sent_sms
        WHERE (:status IS NULL OR status = :status)
          AND (:to IS NULL OR toNumber LIKE '%' || :to || '%')
          AND (:clientReference IS NULL OR clientReference = :clientReference)
          AND (:dateFrom IS NULL OR createdAt >= :dateFrom)
          AND (:dateTo IS NULL OR createdAt <= :dateTo)
        ORDER BY
          CASE WHEN :sortDirection = 'ASC' THEN createdAt END ASC,
          CASE WHEN :sortDirection = 'DESC' THEN createdAt END DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun query(
        status: String?,
        to: String?,
        clientReference: String?,
        dateFrom: Long?,
        dateTo: Long?,
        limit: Int,
        offset: Int,
        sortDirection: String,
    ): List<SentSmsEntity>
}
