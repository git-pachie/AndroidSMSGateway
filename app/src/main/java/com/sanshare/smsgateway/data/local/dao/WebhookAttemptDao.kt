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
}
