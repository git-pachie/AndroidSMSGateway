package com.sanshare.smsgateway.sms

data class IncomingSmsSegment(
    val sender: String?,
    val body: String,
    val timestampMillis: Long,
    val subscriptionId: Int?,
    val simSlot: Int?,
    val sequenceHint: Int,
)

data class ParsedIncomingSms(
    val sender: String,
    val message: String,
    val receivedAt: Long,
    val subscriptionId: Int?,
    val simSlot: Int?,
)
