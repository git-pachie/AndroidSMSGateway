package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sanshare.smsgateway.data.local.entity.SmsSegmentEntity

@Dao
interface SmsSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SmsSegmentEntity>)

    @Update
    suspend fun update(entity: SmsSegmentEntity)

    @Query("SELECT * FROM sms_segments WHERE sentSmsId = :sentSmsId ORDER BY segmentIndex ASC")
    suspend fun getBySentSmsId(sentSmsId: Long): List<SmsSegmentEntity>

    @Query("SELECT * FROM sms_segments WHERE sentSmsId = :sentSmsId AND segmentIndex = :segmentIndex LIMIT 1")
    suspend fun getBySentSmsIdAndIndex(sentSmsId: Long, segmentIndex: Int): SmsSegmentEntity?
}
