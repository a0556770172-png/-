package com.filesafe.vault

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.filesafe.vault.crypto.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException

class FsfRoundTripTest {
    @Test
    fun encryptDecryptRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = CryptoManager(context)
        val adminCode = "1234"
        crypto.setupAdmin(adminCode.toCharArray())
        val master = crypto.deriveMaster(adminCode.toCharArray())
        val plain = "Top secret content".toByteArray()

        val output = ByteArrayOutputStream()
        val metadata = crypto.encryptToFsf(
            input = ByteArrayInputStream(plain),
            output = output,
            displayName = "secret.txt",
            mimeType = "text/plain",
            createdAt = 1L,
            originalSize = plain.size.toLong(),
            sha256 = "deadbeef",
            master = master
        )

        val decrypted = ByteArrayOutputStream()
        crypto.decryptFsf(ByteArrayInputStream(output.toByteArray()), decrypted, metadata.wrapKey)
        assertArrayEquals(plain, decrypted.toByteArray())
    }

    @Test
    fun wrongKeyFails() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = CryptoManager(context)
        val adminCode = "1234"
        crypto.setupAdmin(adminCode.toCharArray())
        val master = crypto.deriveMaster(adminCode.toCharArray())
        val plain = "Top secret content".toByteArray()

        val output = ByteArrayOutputStream()
        val metadata = crypto.encryptToFsf(
            input = ByteArrayInputStream(plain),
            output = output,
            displayName = "secret.txt",
            mimeType = "text/plain",
            createdAt = 1L,
            originalSize = plain.size.toLong(),
            sha256 = "deadbeef",
            master = master
        )

        val wrongKey = ByteArray(32) { 0x01 }
        assertThrows(AEADBadTagException::class.java) {
            val decrypted = ByteArrayOutputStream()
            crypto.decryptFsf(ByteArrayInputStream(output.toByteArray()), decrypted, wrongKey)
        }
    }
}
