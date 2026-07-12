package com.sanshare.smsgateway.core.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayValidatorsTest {
    @Test
    fun validatesServerPortRange() {
        assertFalse(GatewayValidators.serverPort(80).valid)
        assertTrue(GatewayValidators.serverPort(8080).valid)
        assertFalse(GatewayValidators.serverPort(70_000).valid)
    }

    @Test
    fun validatesDestinationNumber() {
        assertTrue(GatewayValidators.destinationNumber("+639171234567").valid)
        assertTrue(GatewayValidators.destinationNumber("0917 123 4567").valid)
        assertFalse(GatewayValidators.destinationNumber("abc://bad").valid)
    }

    @Test
    fun validatesSmsMessageLength() {
        assertFalse(GatewayValidators.smsMessage("").valid)
        assertTrue(GatewayValidators.smsMessage("hello").valid)
        assertFalse(GatewayValidators.smsMessage("x".repeat(5_001)).valid)
    }

    @Test
    fun validatesWebhookUrl() {
        assertTrue(GatewayValidators.webhookUrl("https://example.com/sms", enabled = true, requireHttps = true).valid)
        assertFalse(GatewayValidators.webhookUrl("http://example.com/sms", enabled = true, requireHttps = true).valid)
        assertFalse(GatewayValidators.webhookUrl("file:///tmp/a", enabled = true, requireHttps = false).valid)
        assertFalse(GatewayValidators.webhookUrl("", enabled = true, requireHttps = false).valid)
    }

    @Test
    fun parsesAllowedPrefixes() {
        assertEquals(listOf("+63", "09"), GatewayValidators.allowedPrefixes(" +63,09,+63,bad-value "))
        assertEquals(emptyList<String>(), GatewayValidators.allowedPrefixes(""))
    }

    @Test
    fun normalizesPaging() {
        assertEquals(PagingRequest(100, 0), PagingRequest.normalize(500, -3))
        assertEquals(PagingRequest(50, 20), PagingRequest.normalize(null, 20))
    }
}
