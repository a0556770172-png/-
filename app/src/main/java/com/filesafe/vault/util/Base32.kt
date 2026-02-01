package com.filesafe.vault.util

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val LOOKUP = ALPHABET.withIndex().associate { it.value to it.index }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val output = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                output.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            output.append(ALPHABET[index])
        }
        return output.toString()
    }

    fun decode(text: String): ByteArray {
        val cleaned = text.uppercase().replace("=", "")
        if (cleaned.isEmpty()) return byteArrayOf()
        var buffer = 0
        var bitsLeft = 0
        val output = ArrayList<Byte>()
        for (c in cleaned) {
            val value = LOOKUP[c] ?: throw IllegalArgumentException("Invalid Base32 char: $c")
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                val byte = (buffer shr (bitsLeft - 8)) and 0xFF
                output.add(byte.toByte())
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }
}
