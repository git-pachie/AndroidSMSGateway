package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sent_sms",
    indices = [
        Index("createdAt"),
        Index("status"),
        Index("clientReference"),
        Index("toNumber"),
    ],
)
data class SentSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toNumber: String,
    val message: String,
    val clientReference: String?,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val sendingAt: Long?,
    val sentAt: Long?,
    val deliveredAt: Long?,
    val failedAt: Long?,
    val retryCount: Int,
    val segmentCount: Int,
    val subscriptionId: Int?,
    val simSlot: Int?,
)
