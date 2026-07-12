package com.sanshare.smsgateway.domain.model

object OutgoingSmsStatus {
    const val PENDING = "PENDING"
    const val SENDING = "SENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val FAILED = "FAILED"
    const val UNKNOWN = "UNKNOWN"
}

object OutgoingSmsSegmentStatus {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val FAILED = "FAILED"
    const val DELIVERED = "DELIVERED"
    const val UNAVAILABLE = "UNAVAILABLE"
}
