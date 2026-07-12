package com.sanshare.smsgateway.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyHasherTest {
    @Test
    fun generatedKeyVerifiesAgainstStoredHash() {
        val hasher = ApiKeyHasher()
        val rawKey = hasher.generateRawKey()
        val hash = hasher.hash(rawKey)

        assertTrue(rawKey.length >= 32)
        assertTrue(hasher.verify(rawKey, hash.encoded))
        assertFalse(hasher.verify("wrong-$rawKey", hash.encoded))
    }

    @Test
    fun regeneratedHashInvalidatesOldRawKey() {
        val hasher = ApiKeyHasher()
        val oldKey = hasher.generateRawKey()
        val newKey = hasher.generateRawKey()
        val newHash = hasher.hash(newKey)

        assertNotEquals(oldKey, newKey)
        assertFalse(hasher.verify(oldKey, newHash.encoded))
        assertTrue(hasher.verify(newKey, newHash.encoded))
    }
}
