package com.sanshare.smsgateway.sms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingSmsAssembler @Inject constructor() {
    fun assemble(segments: List<IncomingSmsSegment>): ParsedIncomingSms? {
        if (segments.isEmpty()) return null
        val ordered = segments.sortedBy { it.sequenceHint }
        val sender = ordered.firstNotNullOfOrNull { it.sender?.trim()?.takeIf(String::isNotBlank) } ?: return null
        val message = ordered.joinToString(separator = "") { it.body }
        if (message.isBlank()) return null
        val receivedAt = ordered.minOf { it.timestampMillis }
        val subscriptionId = ordered.firstNotNullOfOrNull { it.subscriptionId }
        val simSlot = ordered.firstNotNullOfOrNull { it.simSlot }
        return ParsedIncomingSms(
            sender = sender,
            message = message,
            receivedAt = receivedAt,
            subscriptionId = subscriptionId,
            simSlot = simSlot,
        )
    }
}
