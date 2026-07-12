package com.sanshare.smsgateway.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sanshare.smsgateway.domain.usecase.ProcessWebhookDeliveryUseCase
import com.sanshare.smsgateway.domain.usecase.WebhookExecutionState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class WebhookRetryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, WebhookRetryWorkerEntryPoint::class.java)
    }

    override suspend fun doWork(): Result {
        val smsId = inputData.getLong(KEY_SMS_ID, -1L)
        if (smsId <= 0L) return Result.failure()
        return when (entryPoint.processWebhookDeliveryUseCase().invoke(smsId).state) {
            WebhookExecutionState.SUCCESS,
            WebhookExecutionState.FAILED,
            WebhookExecutionState.MISSING_SMS,
            -> Result.success()
            WebhookExecutionState.RETRY -> Result.retry()
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WebhookRetryWorkerEntryPoint {
        fun processWebhookDeliveryUseCase(): ProcessWebhookDeliveryUseCase
    }

    companion object {
        const val KEY_SMS_ID = "sms_id"
    }
}
