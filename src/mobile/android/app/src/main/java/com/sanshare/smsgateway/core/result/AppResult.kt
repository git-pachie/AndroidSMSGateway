package com.sanshare.smsgateway.core.result

import com.sanshare.smsgateway.core.error.AppError

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
