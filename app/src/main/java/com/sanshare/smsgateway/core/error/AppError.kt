package com.sanshare.smsgateway.core.error

enum class ErrorCode {
    SMS_PERMISSION_DENIED,
    RECEIVE_SMS_PERMISSION_DENIED,
    NOTIFICATION_PERMISSION_DENIED,
    PORT_IN_USE,
    SERVER_START_FAILED,
    DATABASE_ERROR,
    INTERNAL_ERROR,
    INVALID_REQUEST,
    MISSING_API_KEY,
    INVALID_API_KEY,
    RECORD_NOT_FOUND,
    INVALID_WEBHOOK_URL,
    WEBHOOK_NOT_CONFIGURED,
    WEBHOOK_DISABLED,
    FEATURE_NOT_READY,
}

data class AppError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null,
)
