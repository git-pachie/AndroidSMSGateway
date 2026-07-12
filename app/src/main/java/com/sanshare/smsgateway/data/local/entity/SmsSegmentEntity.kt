package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_segments",
    foreignKeys = [
        ForeignKey(
            entity = SentSmsEntity::class,
            parentColumns = ["id"],
            childColumns = ["sentSmsId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sentSmsId"),
        Index(value = ["sentSmsId", "segmentIndex"], unique = true),
        Index("updatedAt"),
    ],
)
data class SmsSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sentSmsId: Long,
    val segmentIndex: Int,
    val totalSegments: Int,
    val sentStatus: String,
    val deliveryStatus: String,
    val sentResultCode: Int?,
    val deliveryResultCode: Int?,
    val updatedAt: Long,
)
