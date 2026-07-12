package com.sanshare.smsgateway.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IncomingSmsFingerprintTest {
    private val fingerprint = IncomingSmsFingerprint()

    @Test
    fun preservesAlphanumericSenderSemantics() {
        assertEquals("ACMEBANK", fingerprint.normalizeSender("  AcmeBank "))
        assertEquals("+639171234567", fingerprint.normalizeSender("+63 917-123-4567"))
    }

    @Test
    fun groupsDuplicateFingerprintsWithinSameWindow() {
        val first = fingerprint.create("ACMEBANK", "OTP 123456", 600_000L, 1)
        val second = fingerprint.create("ACMEBANK", "OTP 123456", 600_100L, 1)
        val later = fingerprint.create("ACMEBANK", "OTP 123456", 1_200_100L, 1)

        assertEquals(first, second)
        assertNotEquals(first, later)
    }
}
