package com.filesafe.vault

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.filesafe.vault.crypto.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AccessKeyTest {
    @Test
    fun checksumValidation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = CryptoManager(context)
        val wrapKey = ByteArray(32) { it.toByte() }
        val accessKey = crypto.buildAccessKey(wrapKey)
        val parsed = crypto.parseAccessKey(accessKey)
        assertArrayEquals(wrapKey, parsed)
    }
}
