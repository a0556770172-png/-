package com.filesafe.vault

import com.filesafe.vault.util.Base32
import org.junit.Assert.assertEquals
import org.junit.Test

class Base32Test {
    @Test
    fun roundTrip() {
        val data = "hello world".toByteArray()
        val encoded = Base32.encode(data)
        val decoded = Base32.decode(encoded)
        assertEquals(String(data), String(decoded))
    }
}
