package com.sanshare.smsgateway.sms

interface OutgoingSmsLifecycle {
    suspend fun markSending(messageId: Long)
    suspend fun markDispatchFailure(messageId: Long, code: com.sanshare.smsgateway.core.error.ErrorCode, message: String)
}
