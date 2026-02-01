package com.filesafe.vault.util

import java.io.InputStream
import java.security.MessageDigest

object FileUtils {
    fun sha256AndSize(input: InputStream): Pair<String, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        var read = input.read(buffer)
        while (read >= 0) {
            if (read > 0) {
                digest.update(buffer, 0, read)
                total += read
            }
            read = input.read(buffer)
        }
        return HashUtils.toHex(digest.digest()) to total
    }
}
