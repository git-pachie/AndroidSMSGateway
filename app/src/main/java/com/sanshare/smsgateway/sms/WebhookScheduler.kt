package com.sanshare.smsgateway.sms

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WebhookScheduler {
    suspend fun scheduleReceivedSms(receivedSmsId: Long, replaceExisting: Boolean = false): Boolean
    fun observeScheduled(receivedSmsId: Long): Flow<Boolean>
}

@Singleton
class WorkManagerWebhookScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : WebhookScheduler {
    private val workManager by lazy { WorkManager.getInstance(context) }

    override suspend fun scheduleReceivedSms(receivedSmsId: Long, replaceExisting: Boolean): Boolean {
        val workName = uniqueWorkName(receivedSmsId)
        val existing = workManager.getWorkInfosForUniqueWork(workName).get()
        val hasActive = existing.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        if (hasActive && !replaceExisting) return false

        val request = OneTimeWorkRequestBuilder<WebhookRetryWorker>()
            .setInputData(workDataOf(WebhookRetryWorker.KEY_SMS_ID to receivedSmsId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WebhookRetryPolicy.minBackoffSeconds().toLong(),
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            workName,
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }

    override fun observeScheduled(receivedSmsId: Long): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(receivedSmsId))
            .map { infos ->
                infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
            }
    }

    companion object {
        fun uniqueWorkName(receivedSmsId: Long): String = "webhook-retry-$receivedSmsId"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WebhookSchedulerModule {
    @Binds
    @Singleton
    abstract fun bindWebhookScheduler(
        impl: WorkManagerWebhookScheduler,
    ): WebhookScheduler
}
