package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sanshare.smsgateway.data.local.entity.RequestAuditLogEntity

@Dao
interface RequestAuditLogDao {
    @Insert
    suspend fun insert(entity: RequestAuditLogEntity): Long

    @Query("SELECT * FROM request_audit_logs ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun query(limit: Int, offset: Int): List<RequestAuditLogEntity>

    @Query(
        """
        SELECT * FROM request_audit_logs
        WHERE (:dateFrom IS NULL OR createdAt >= :dateFrom)
          AND (:dateTo IS NULL OR createdAt <= :dateTo)
        ORDER BY
          CASE WHEN :sortDirection = 'ASC' THEN createdAt END ASC,
          CASE WHEN :sortDirection = 'DESC' THEN createdAt END DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun queryFiltered(
        dateFrom: Long?,
        dateTo: Long?,
        limit: Int,
        offset: Int,
        sortDirection: String,
    ): List<RequestAuditLogEntity>

    @Query(
        """
        SELECT COUNT(*) FROM request_audit_logs
        WHERE (:dateFrom IS NULL OR createdAt >= :dateFrom)
          AND (:dateTo IS NULL OR createdAt <= :dateTo)
        """,
    )
    suspend fun countFiltered(dateFrom: Long?, dateTo: Long?): Int

    @Query("DELETE FROM request_audit_logs")
    suspend fun deleteAll()
}
