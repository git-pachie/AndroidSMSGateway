package com.sanshare.smsgateway.sms

import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmsPendingIntentRequestCodesTest {
    @Test
    fun generatesDistinctCodesAcrossKindsAndSegments() {
        val sentFirst = SmsPendingIntentRequestCodes.sent(42L, 0)
        val sentSecond = SmsPendingIntentRequestCodes.sent(42L, 1)
        val deliveredFirst = SmsPendingIntentRequestCodes.delivered(42L, 0)
        val otherMessage = SmsPendingIntentRequestCodes.sent(43L, 0)

        assertNotEquals(sentFirst, sentSecond)
        assertNotEquals(sentFirst, deliveredFirst)
        assertNotEquals(sentFirst, otherMessage)
        assertNotEquals(sentSecond, deliveredFirst)
    }
}
