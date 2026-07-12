package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "webhook_attempts",
    foreignKeys = [
        ForeignKey(
            entity = ReceivedSmsEntity::class,
            parentColumns = ["id"],
            childColumns = ["receivedSmsId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("receivedSmsId"),
        Index("attemptedAt"),
    ],
)
data class WebhookAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receivedSmsId: Long,
    val attemptNumber: Int,
    val requestUrlSummary: String,
    val responseCode: Int?,
    val responseBodySummary: String?,
    val durationMs: Long?,
    val success: Boolean,
    val errorCode: String?,
    val errorMessage: String?,
    val attemptedAt: Long,
)
