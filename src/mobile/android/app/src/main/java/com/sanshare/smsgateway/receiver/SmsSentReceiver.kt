package com.sanshare.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sanshare.smsgateway.sms.OutgoingSmsStatusUpdater
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsSentReceiver : BroadcastReceiver() {
    @Inject lateinit var updater: OutgoingSmsStatusUpdater

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsReceiverContract.ACTION_SMS_SENT) return
        val messageId = intent.getLongExtra(SmsReceiverContract.EXTRA_MESSAGE_ID, -1L)
        val segmentIndex = intent.getIntExtra(SmsReceiverContract.EXTRA_SEGMENT_INDEX, -1)
        val callbackResultCode = resultCode
        if (messageId <= 0L || segmentIndex < 0) return
        val pendingResult = goAsync()
        receiverScope.launch {
            runCatching { updater.handleSentResult(messageId, segmentIndex, callbackResultCode) }
            pendingResult.finish()
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
