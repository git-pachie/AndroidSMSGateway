package com.sanshare.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sanshare.smsgateway.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: AppSettingsEntity): Long

    @Update
    suspend fun update(entity: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = :id")
    suspend fun getById(id: Int = AppSettingsEntity.STABLE_ID): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = :id")
    fun observeById(id: Int = AppSettingsEntity.STABLE_ID): Flow<AppSettingsEntity?>
}
