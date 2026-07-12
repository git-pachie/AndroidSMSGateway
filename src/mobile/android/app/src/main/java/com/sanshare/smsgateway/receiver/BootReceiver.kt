package com.sanshare.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.domain.usecase.HandleBootCompletedUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var handleBootCompletedUseCase: HandleBootCompletedUseCase
    @Inject lateinit var logger: AppLogger

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                handleBootCompletedUseCase()
            } catch (ex: Exception) {
                logger.error("Boot", "Boot receiver failed to start gateway", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
