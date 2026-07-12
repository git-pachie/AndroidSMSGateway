package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sanshare.smsgateway.data.local.entity.SystemLogEntity

@Dao
interface SystemLogDao {
    @Insert
    suspend fun insert(entity: SystemLogEntity): Long

    @Query("SELECT * FROM system_logs ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun query(limit: Int, offset: Int): List<SystemLogEntity>

    @Query(
        """
        SELECT * FROM system_logs
        WHERE (:level IS NULL OR level = :level)
          AND (:category IS NULL OR category = :category)
          AND (:dateFrom IS NULL OR createdAt >= :dateFrom)
          AND (:dateTo IS NULL OR createdAt <= :dateTo)
        ORDER BY
          CASE WHEN :sortDirection = 'ASC' THEN createdAt END ASC,
          CASE WHEN :sortDirection = 'DESC' THEN createdAt END DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun queryFiltered(
        level: String?,
        category: String?,
        dateFrom: Long?,
        dateTo: Long?,
        limit: Int,
        offset: Int,
        sortDirection: String,
    ): List<SystemLogEntity>

    @Query(
        """
        SELECT COUNT(*) FROM system_logs
        WHERE (:level IS NULL OR level = :level)
          AND (:category IS NULL OR category = :category)
          AND (:dateFrom IS NULL OR createdAt >= :dateFrom)
          AND (:dateTo IS NULL OR createdAt <= :dateTo)
        """,
    )
    suspend fun countFiltered(level: String?, category: String?, dateFrom: Long?, dateTo: Long?): Int

    @Query("DELETE FROM system_logs")
    suspend fun deleteAll()
}
