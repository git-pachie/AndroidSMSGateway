package com.sanshare.smsgateway.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.sanshare.smsgateway.core.error.AppError
import com.sanshare.smsgateway.core.error.ErrorCode
import com.sanshare.smsgateway.core.result.AppResult
import com.sanshare.smsgateway.receiver.SmsDeliveredReceiver
import com.sanshare.smsgateway.receiver.SmsReceiverContract
import com.sanshare.smsgateway.receiver.SmsSentReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PreparedSmsDispatch {
    val subscriptionId: Int?
    val simSlot: Int?
    val segments: List<String>
}

interface SmsDispatcher {
    fun prepare(message: String, requestedSubscriptionId: Int?): AppResult<PreparedSmsDispatch>
    fun dispatch(messageId: Long, destination: String, prepared: PreparedSmsDispatch)
}

@Singleton
class AndroidSmsDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsDispatcher {
    override fun prepare(message: String, requestedSubscriptionId: Int?): AppResult<PreparedSmsDispatch> {
        val resolved = resolveManager(requestedSubscriptionId) ?: return AppResult.Failure(
            AppError(ErrorCode.NO_SIM_AVAILABLE, "No SMS-capable SIM is available"),
        )
        return try {
            val parts = resolved.manager.divideMessage(message).ifEmpty { listOf(message) }
            AppResult.Success(
                AndroidPreparedSmsDispatch(
                    manager = resolved.manager,
                    subscriptionId = resolved.subscriptionId,
                    simSlot = resolved.simSlot,
                    segments = parts,
                ),
            )
        } catch (ex: IllegalArgumentException) {
            AppResult.Failure(AppError(ErrorCode.INVALID_REQUEST, "Unable to prepare SMS message"))
        } catch (ex: SecurityException) {
            AppResult.Failure(AppError(ErrorCode.SMS_PERMISSION_DENIED, "SMS permission is required"))
        }
    }

    override fun dispatch(messageId: Long, destination: String, prepared: PreparedSmsDispatch) {
        val androidPrepared = prepared as? AndroidPreparedSmsDispatch
            ?: error("PreparedSmsDispatch was not created by AndroidSmsDispatcher")
        val sentIntents = androidPrepared.segments.mapIndexed { index, _ ->
            PendingIntent.getBroadcast(
                context,
                SmsPendingIntentRequestCodes.sent(messageId, index),
                Intent(context, SmsSentReceiver::class.java)
                    .setAction(SmsReceiverContract.ACTION_SMS_SENT)
                    .putExtra(SmsReceiverContract.EXTRA_MESSAGE_ID, messageId)
                    .putExtra(SmsReceiverContract.EXTRA_SEGMENT_INDEX, index)
                    .putExtra(SmsReceiverContract.EXTRA_TOTAL_SEGMENTS, androidPrepared.segments.size),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val deliveredIntents = androidPrepared.segments.mapIndexed { index, _ ->
            PendingIntent.getBroadcast(
                context,
                SmsPendingIntentRequestCodes.delivered(messageId, index),
                Intent(context, SmsDeliveredReceiver::class.java)
                    .setAction(SmsReceiverContract.ACTION_SMS_DELIVERED)
                    .putExtra(SmsReceiverContract.EXTRA_MESSAGE_ID, messageId)
                    .putExtra(SmsReceiverContract.EXTRA_SEGMENT_INDEX, index)
                    .putExtra(SmsReceiverContract.EXTRA_TOTAL_SEGMENTS, androidPrepared.segments.size),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        if (androidPrepared.segments.size == 1) {
            androidPrepared.manager.sendTextMessage(
                destination,
                null,
                androidPrepared.segments.first(),
                sentIntents.first(),
                deliveredIntents.first(),
            )
        } else {
            androidPrepared.manager.sendMultipartTextMessage(
                destination,
                null,
                ArrayList(androidPrepared.segments),
                ArrayList(sentIntents),
                ArrayList(deliveredIntents),
            )
        }
    }

    private fun resolveManager(requestedSubscriptionId: Int?): ResolvedSmsManager? {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        val activeSubscriptions = runCatching { subscriptionManager?.activeSubscriptionInfoList.orEmpty() }.getOrElse { emptyList() }

        if (requestedSubscriptionId != null) {
            val active = activeSubscriptions.firstOrNull { it.subscriptionId == requestedSubscriptionId }
            if (activeSubscriptions.isNotEmpty() && active == null) return null
            return runCatching {
                ResolvedSmsManager(
                    manager = SmsManager.getSmsManagerForSubscriptionId(requestedSubscriptionId),
                    subscriptionId = requestedSubscriptionId,
                    simSlot = active?.simSlotIndex,
                )
            }.getOrNull()
        }

        val firstActive = activeSubscriptions.firstOrNull()
        if (firstActive != null) {
            return runCatching {
                ResolvedSmsManager(
                    manager = SmsManager.getSmsManagerForSubscriptionId(firstActive.subscriptionId),
                    subscriptionId = firstActive.subscriptionId,
                    simSlot = firstActive.simSlotIndex,
                )
            }.getOrNull()
        }

        val defaultSubscriptionId = SmsManager.getDefaultSmsSubscriptionId()
        if (defaultSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && defaultSubscriptionId >= 0) {
            return runCatching {
                ResolvedSmsManager(
                    manager = SmsManager.getSmsManagerForSubscriptionId(defaultSubscriptionId),
                    subscriptionId = defaultSubscriptionId,
                    simSlot = null,
                )
            }.getOrNull()
        }
        return null
    }
}

private data class ResolvedSmsManager(
    val manager: SmsManager,
    val subscriptionId: Int?,
    val simSlot: Int?,
)

data class AndroidPreparedSmsDispatch(
    val manager: SmsManager,
    override val subscriptionId: Int?,
    override val simSlot: Int?,
    override val segments: List<String>,
) : PreparedSmsDispatch

object SmsPendingIntentRequestCodes {
    fun sent(messageId: Long, segmentIndex: Int): Int = code(messageId, segmentIndex, 1)
    fun delivered(messageId: Long, segmentIndex: Int): Int = code(messageId, segmentIndex, 2)

    private fun code(messageId: Long, segmentIndex: Int, kind: Int): Int {
        val raw = (messageId * 97L) + (segmentIndex.toLong() * 7L) + kind
        return (raw and Int.MAX_VALUE.toLong()).toInt()
    }
}
