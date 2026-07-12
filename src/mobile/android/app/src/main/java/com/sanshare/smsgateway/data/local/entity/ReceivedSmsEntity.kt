package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "received_sms",
    indices = [
        Index("receivedAt"),
        Index("forwardStatus"),
        Index("fromNumber"),
        Index("messageFingerprint"),
    ],
)
data class ReceivedSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromNumber: String,
    val message: String,
    val receivedAt: Long,
    val subscriptionId: Int?,
    val simSlot: Int?,
    val forwardStatus: String,
    val webhookResponseCode: Int?,
    val webhookResponseBody: String?,
    val lastForwardAttemptAt: Long?,
    val nextRetryAt: Long?,
    val retryCount: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val messageFingerprint: String?,
)
