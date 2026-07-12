package com.sanshare.smsgateway.sms

import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface IncomingSmsIntentParser {
    fun parse(intent: Intent): List<IncomingSmsSegment>
}

@Singleton
class AndroidIncomingSmsIntentParser @Inject constructor() : IncomingSmsIntentParser {
    override fun parse(intent: Intent): List<IncomingSmsSegment> {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty()
        val subscriptionId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            .takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        val slotFromExtras = intent.extras?.let { bundle ->
            when {
                bundle.containsKey("slot") -> bundle.getInt("slot")
                bundle.containsKey("simSlot") -> bundle.getInt("simSlot")
                bundle.containsKey("phone") -> bundle.getInt("phone")
                else -> null
            }
        }
        return messages.mapIndexed { index, sms ->
            IncomingSmsSegment(
                sender = sms.displayOriginatingAddress ?: sms.originatingAddress,
                body = sms.displayMessageBody ?: sms.messageBody.orEmpty(),
                timestampMillis = sms.timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
                subscriptionId = subscriptionId,
                simSlot = slotFromExtras,
                sequenceHint = index,
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class IncomingSmsParserModule {
    @Binds
    @Singleton
    abstract fun bindIncomingSmsIntentParser(
        impl: AndroidIncomingSmsIntentParser,
    ): IncomingSmsIntentParser
}
