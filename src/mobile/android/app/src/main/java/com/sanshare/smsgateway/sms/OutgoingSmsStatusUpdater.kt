package com.sanshare.smsgateway.sms

import android.app.Activity
import android.telephony.SmsManager
import androidx.room.withTransaction
import com.sanshare.smsgateway.core.error.ErrorCode
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.time.TimeProvider
import com.sanshare.smsgateway.data.local.AppDatabase
import com.sanshare.smsgateway.data.local.dao.SentSmsDao
import com.sanshare.smsgateway.data.local.dao.SmsSegmentDao
import com.sanshare.smsgateway.domain.model.OutgoingSmsSegmentStatus
import com.sanshare.smsgateway.domain.model.OutgoingSmsStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutgoingSmsStatusUpdater @Inject constructor(
    private val database: AppDatabase,
    private val sentSmsDao: SentSmsDao,
    private val smsSegmentDao: SmsSegmentDao,
    private val timeProvider: TimeProvider,
    private val outgoingSmsWebhookNotifier: OutgoingSmsWebhookNotifier,
    private val logger: AppLogger,
) : OutgoingSmsLifecycle {
    override suspend fun markSending(messageId: Long) {
        database.withTransaction {
            val current = sentSmsDao.getById(messageId) ?: return@withTransaction
            sentSmsDao.update(
                current.copy(
                    status = OutgoingSmsStatus.SENDING,
                    sendingAt = current.sendingAt ?: timeProvider.nowMillis(),
                    errorCode = null,
                    errorMessage = null,
                ),
            )
        }
    }

    override suspend fun markDispatchFailure(messageId: Long, code: ErrorCode, message: String) {
        var updatedMessage: com.sanshare.smsgateway.data.local.entity.SentSmsEntity? = null
        database.withTransaction {
            val current = sentSmsDao.getById(messageId) ?: return@withTransaction
            updatedMessage = current.copy(
                status = OutgoingSmsStatus.FAILED,
                failedAt = timeProvider.nowMillis(),
                errorCode = code.name,
                errorMessage = message,
            )
            sentSmsDao.update(updatedMessage!!)
        }
        updatedMessage?.let { outgoingSmsWebhookNotifier.notifyStatus(it) }
    }

    suspend fun handleSentResult(messageId: Long, segmentIndex: Int, resultCode: Int) {
        var terminalStatusMessage: com.sanshare.smsgateway.data.local.entity.SentSmsEntity? = null
        database.withTransaction {
            val message = sentSmsDao.getById(messageId) ?: return@withTransaction
            val segment = smsSegmentDao.getBySentSmsIdAndIndex(messageId, segmentIndex) ?: return@withTransaction
            val now = timeProvider.nowMillis()
            if (resultCode == Activity.RESULT_OK) {
                smsSegmentDao.update(
                    segment.copy(
                        sentStatus = OutgoingSmsSegmentStatus.SENT,
                        sentResultCode = resultCode,
                        updatedAt = now,
                    ),
                )
                val segments = smsSegmentDao.getBySentSmsId(messageId)
                if (segments.all { it.sentStatus == OutgoingSmsSegmentStatus.SENT }) {
                    terminalStatusMessage = message.copy(
                        status = OutgoingSmsStatus.SENT,
                        sentAt = now,
                        errorCode = null,
                        errorMessage = null,
                    )
                    sentSmsDao.update(terminalStatusMessage!!)
                    logger.info("SMS", "Outgoing SMS sent: id=$messageId segments=${segments.size}")
                }
            } else {
                val mapped = mapSentResultCode(resultCode)
                smsSegmentDao.update(
                    segment.copy(
                        sentStatus = OutgoingSmsSegmentStatus.FAILED,
                        sentResultCode = resultCode,
                        updatedAt = now,
                    ),
                )
                terminalStatusMessage = message.copy(
                    status = OutgoingSmsStatus.FAILED,
                    failedAt = now,
                    errorCode = mapped.first.name,
                    errorMessage = mapped.second,
                )
                sentSmsDao.update(terminalStatusMessage!!)
                logger.warning("SMS", "Outgoing SMS failed: id=$messageId code=${mapped.first.name}")
            }
        }
        terminalStatusMessage?.let { outgoingSmsWebhookNotifier.notifyStatus(it) }
    }

    suspend fun handleDeliveryResult(messageId: Long, segmentIndex: Int, resultCode: Int) {
        var deliveredMessage: com.sanshare.smsgateway.data.local.entity.SentSmsEntity? = null
        database.withTransaction {
            val message = sentSmsDao.getById(messageId) ?: return@withTransaction
            val segment = smsSegmentDao.getBySentSmsIdAndIndex(messageId, segmentIndex) ?: return@withTransaction
            val now = timeProvider.nowMillis()
            val status = if (resultCode == Activity.RESULT_OK) {
                OutgoingSmsSegmentStatus.DELIVERED
            } else {
                OutgoingSmsSegmentStatus.UNAVAILABLE
            }
            smsSegmentDao.update(
                segment.copy(
                    deliveryStatus = status,
                    deliveryResultCode = resultCode,
                    updatedAt = now,
                ),
            )
            val segments = smsSegmentDao.getBySentSmsId(messageId)
            if (segments.all { it.deliveryStatus == OutgoingSmsSegmentStatus.DELIVERED }) {
                deliveredMessage = message.copy(
                    status = OutgoingSmsStatus.DELIVERED,
                    deliveredAt = now,
                )
                sentSmsDao.update(deliveredMessage!!)
                logger.info("SMS", "Outgoing SMS delivered: id=$messageId")
            }
        }
        deliveredMessage?.let { outgoingSmsWebhookNotifier.notifyStatus(it) }
    }

    fun mapSentResultCode(resultCode: Int): Pair<ErrorCode, String> {
        return when (resultCode) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> ErrorCode.SMS_GENERIC_FAILURE to "Generic SMS failure"
            SmsManager.RESULT_ERROR_RADIO_OFF -> ErrorCode.SMS_RADIO_OFF to "Radio is off"
            SmsManager.RESULT_ERROR_NULL_PDU -> ErrorCode.SMS_NULL_PDU to "Null PDU"
            SmsManager.RESULT_ERROR_NO_SERVICE -> ErrorCode.SMS_NO_SERVICE to "No SMS service"
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> ErrorCode.SMS_LIMIT_EXCEEDED to "SMS limit exceeded"
            SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> ErrorCode.SMS_FDN_CHECK_FAILURE to "FDN check failure"
            SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> ErrorCode.SMS_SHORT_CODE_NOT_ALLOWED to "Short code not allowed"
            SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> ErrorCode.SMS_SHORT_CODE_NEVER_ALLOWED to "Short code never allowed"
            else -> ErrorCode.UNKNOWN_SMS_ERROR to "Unknown SMS error"
        }
    }
}
