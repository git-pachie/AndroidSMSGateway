package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "request_audit_logs",
    indices = [
        Index("createdAt"),
        Index("path"),
        Index("responseCode"),
    ],
)
data class RequestAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val path: String,
    val remoteAddress: String?,
    val responseCode: Int,
    val durationMs: Long,
    val authenticated: Boolean,
    val createdAt: Long,
)
