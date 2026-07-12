package com.sanshare.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.domain.usecase.StoreIncomingSmsUseCase
import com.sanshare.smsgateway.sms.IncomingSmsAssembler
import com.sanshare.smsgateway.sms.IncomingSmsIntentParser
import com.sanshare.smsgateway.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var parser: IncomingSmsIntentParser
    @Inject lateinit var assembler: IncomingSmsAssembler
    @Inject lateinit var storeIncomingSmsUseCase: StoreIncomingSmsUseCase
    @Inject lateinit var logger: AppLogger

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val permissions = PermissionUtils.gatewayPermissionState(context)
                if (!permissions.canReceiveSms) {
                    logger.warning("SMS", "Incoming SMS ignored because RECEIVE_SMS permission is unavailable")
                    return@launch
                }
                val parsedSegments = parser.parse(intent)
                val assembled = assembler.assemble(parsedSegments)
                if (assembled == null) {
                    logger.warning("SMS", "Incoming SMS parse failed or produced no logical message")
                    return@launch
                }
                storeIncomingSmsUseCase(assembled)
            } catch (ex: Exception) {
                logger.error("SMS", "Incoming SMS receiver failed", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
