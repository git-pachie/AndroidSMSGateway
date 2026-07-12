package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "system_logs",
    indices = [
        Index("createdAt"),
        Index("level"),
        Index("category"),
    ],
)
data class SystemLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: String,
    val category: String,
    val eventCode: String?,
    val message: String,
    val details: String?,
    val createdAt: Long,
)
