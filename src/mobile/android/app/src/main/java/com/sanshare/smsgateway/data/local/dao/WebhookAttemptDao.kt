package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity

@Dao
interface WebhookAttemptDao {
    @Insert
    suspend fun insert(entity: WebhookAttemptEntity): Long

    @Query("SELECT * FROM webhook_attempts WHERE id = :id")
    suspend fun getById(id: Long): WebhookAttemptEntity?

    @Query(
        """
        SELECT * FROM webhook_attempts
        WHERE (:smsId IS NULL OR receivedSmsId = :smsId)
          AND (:success IS NULL OR success = :success)
          AND (:responseCode IS NULL OR responseCode = :responseCode)
          AND (:dateFrom IS NULL OR attemptedAt >= :dateFrom)
          AND (:dateTo IS NULL OR attemptedAt <= :dateTo)
        ORDER BY
          CASE WHEN :sortDirection = 'ASC' THEN attemptedAt END ASC,
          CASE WHEN :sortDirection = 'DESC' THEN attemptedAt END DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun query(
        smsId: Long?,
        success: Boolean?,
        responseCode: Int?,
        dateFrom: Long?,
        dateTo: Long?,
        limit: Int,
        offset: Int,
        sortDirection: String,
    ): List<WebhookAttemptEntity>

    @Query(
        """
        SELECT COUNT(*) FROM webhook_attempts
        WHERE (:smsId IS NULL OR receivedSmsId = :smsId)
          AND (:success IS NULL OR success = :success)
          AND (:responseCode IS NULL OR responseCode = :responseCode)
          AND (:dateFrom IS NULL OR attemptedAt >= :dateFrom)
          AND (:dateTo IS NULL OR attemptedAt <= :dateTo)
        """,
    )
    suspend fun countFiltered(
        smsId: Long?,
        success: Boolean?,
        responseCode: Int?,
        dateFrom: Long?,
        dateTo: Long?,
    ): Int

    @Query(
        """
        SELECT * FROM webhook_attempts
        WHERE receivedSmsId = :smsId
        ORDER BY attemptedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun listBySmsId(
        smsId: Long,
        limit: Int,
    ): List<WebhookAttemptEntity>

    @Query(
        """
        SELECT attemptedAt FROM webhook_attempts
        WHERE success = :success
        ORDER BY attemptedAt DESC
        LIMIT 1
        """,
    )
    fun observeLatestAttemptedAt(success: Boolean): kotlinx.coroutines.flow.Flow<Long?>
}
