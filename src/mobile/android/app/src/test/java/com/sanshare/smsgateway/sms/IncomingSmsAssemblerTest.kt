package com.sanshare.smsgateway.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingSmsAssemblerTest {
    private val assembler = IncomingSmsAssembler()

    @Test
    fun reconstructsMultipartBodyInSequenceOrder() {
        val assembled = assembler.assemble(
            listOf(
                IncomingSmsSegment("ACMEBANK", " part-2", 2_000L, 1, 0, 1),
                IncomingSmsSegment("ACMEBANK", "Hello", 1_000L, 1, 0, 0),
            ),
        )

        requireNotNull(assembled)
        assertEquals("ACMEBANK", assembled.sender)
        assertEquals("Hello part-2", assembled.message)
        assertEquals(1_000L, assembled.receivedAt)
    }

    @Test
    fun returnsNullWhenNoUsableSenderOrMessageExists() {
        val assembled = assembler.assemble(
            listOf(
                IncomingSmsSegment(null, "", 1_000L, null, null, 0),
            ),
        )

        assertNull(assembled)
    }
}
