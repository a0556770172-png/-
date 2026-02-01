package com.filesafe.vault.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.filesafe.vault.util.Base32
import com.filesafe.vault.util.HashUtils
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(context: Context) {
    private val prefs: SharedPreferences
    private val secureRandom = SecureRandom()

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "vault_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasAdmin(): Boolean = prefs.contains(KEY_GLOBAL_SALT) && prefs.contains(KEY_ADMIN_VERIFIER)

    fun setupAdmin(adminCode: CharArray) {
        val globalSalt = randomBytes(16)
        val master = deriveMaster(adminCode, globalSalt)
        val verifier = HashUtils.sha256(master + VERIFY_SUFFIX)
        prefs.edit()
            .putString(KEY_GLOBAL_SALT, Base32.encode(globalSalt))
            .putString(KEY_ADMIN_VERIFIER, Base32.encode(verifier))
            .apply()
        adminCode.fill('\u0000')
    }

    fun verifyAdmin(adminCode: CharArray): Boolean {
        val saltText = prefs.getString(KEY_GLOBAL_SALT, null) ?: return false
        val verifierText = prefs.getString(KEY_ADMIN_VERIFIER, null) ?: return false
        val globalSalt = Base32.decode(saltText)
        val expected = Base32.decode(verifierText)
        val master = deriveMaster(adminCode, globalSalt)
        val actual = HashUtils.sha256(master + VERIFY_SUFFIX)
        adminCode.fill('\u0000')
        return HashUtils.constantTimeEquals(expected, actual)
    }

    fun deriveMaster(adminCode: CharArray): ByteArray {
        val saltText = prefs.getString(KEY_GLOBAL_SALT, null)
            ?: throw IllegalStateException("Admin not initialized")
        return deriveMaster(adminCode, Base32.decode(saltText))
    }

    fun storeWrapKey(id: String, wrapKey: ByteArray) {
        prefs.edit().putString(KEY_WRAP_PREFIX + id, Base32.encode(wrapKey)).apply()
    }

    fun loadWrapKey(id: String): ByteArray? {
        val data = prefs.getString(KEY_WRAP_PREFIX + id, null) ?: return null
        return Base32.decode(data)
    }

    fun forgetWrapKey(id: String) {
        prefs.edit().remove(KEY_WRAP_PREFIX + id).apply()
    }

    fun buildAccessKey(wrapKey: ByteArray): String {
        val body = Base32.encode(wrapKey)
        val checksum = Base32.encode(HashUtils.sha256(wrapKey)).take(6)
        return "$body-$checksum"
    }

    fun parseAccessKey(text: String): ByteArray {
        val parts = text.trim().uppercase().split("-")
        require(parts.size == 2) { "Invalid access key" }
        val wrapKey = Base32.decode(parts[0])
        val checksum = Base32.encode(HashUtils.sha256(wrapKey)).take(6)
        require(parts[1] == checksum) { "Checksum mismatch" }
        return wrapKey
    }

    fun encryptToFsf(
        input: InputStream,
        output: OutputStream,
        displayName: String,
        mimeType: String,
        createdAt: Long,
        originalSize: Long,
        sha256: String,
        master: ByteArray
    ): FsfMetadata {
        val contentKey = randomBytes(32)
        val fileSalt = randomBytes(16)
        val wrapKey = deriveWrapKey(master, fileSalt)
        val wrappedKeyBlob = wrapKeyBlob(wrapKey, contentKey)

        val header = JSONObject().apply {
            put("originalName", displayName)
            put("mimeType", mimeType)
            put("createdAt", createdAt)
            put("originalSize", originalSize)
            put("sha256", sha256)
            put("fileSaltB64", Base32.encode(fileSalt))
            put("kdf", JSONObject().apply {
                put("iterations", WRAP_ITERATIONS)
                put("algo", "pbkdf2-sha256")
            })
        }
        val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
        val payloadIv = randomBytes(12)

        val headerLen = headerBytes.size
        val wrappedLen = wrappedKeyBlob.size

        output.write(MAGIC)
        output.write(byteArrayOf(VERSION))
        output.write(intToBytes(headerLen))
        output.write(headerBytes)
        output.write(shortToBytes(wrappedLen))
        output.write(wrappedKeyBlob)
        output.write(byteArrayOf(payloadIv.size.toByte()))
        output.write(payloadIv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, payloadIv))
        cipherInputToOutput(cipher, input, output)

        return FsfMetadata(
            wrapKey = wrapKey,
            fileSalt = fileSalt,
            headerJson = header.toString(),
            payloadIv = payloadIv
        )
    }

    fun decryptFsf(
        input: InputStream,
        output: OutputStream,
        wrapKey: ByteArray
    ): FsfHeader {
        val header = parseHeader(input)
        val wrappedBlob = readBytes(input, header.wrappedKeyLen)
        val payloadIvLen = input.read()
        require(payloadIvLen > 0) { "Invalid payload IV length" }
        val payloadIv = readBytes(input, payloadIvLen)

        val contentKey = unwrapKey(wrapKey, wrappedBlob)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, payloadIv))
        cipherInputToOutput(cipher, input, output)
        return header
    }

    fun parseHeader(stream: InputStream): FsfHeader {
        val magic = readBytes(stream, MAGIC.size)
        require(magic.contentEquals(MAGIC)) { "Invalid magic" }
        val version = stream.read().toByte()
        require(version == VERSION) { "Unsupported version" }
        val headerLen = bytesToInt(readBytes(stream, 4))
        val headerJson = String(readBytes(stream, headerLen), Charsets.UTF_8)
        val wrappedLen = bytesToShort(readBytes(stream, 2)).toInt()
        return FsfHeader(headerJson = headerJson, wrappedKeyLen = wrappedLen)
    }

    fun parseHeaderMetadata(headerJson: String): FsfMetadataHeader {
        val obj = JSONObject(headerJson)
        return FsfMetadataHeader(
            originalName = obj.getString("originalName"),
            mimeType = obj.getString("mimeType"),
            createdAt = obj.getLong("createdAt"),
            originalSize = obj.getLong("originalSize"),
            sha256 = obj.getString("sha256"),
            fileSalt = Base32.decode(obj.getString("fileSaltB64"))
        )
    }

    fun buildWrapKeyFromAccess(accessKey: String): ByteArray = parseAccessKey(accessKey)

    fun deriveWrapKey(master: ByteArray, fileSalt: ByteArray): ByteArray {
        val spec = PBEKeySpec(Base32.encode(master).toCharArray(), fileSalt, WRAP_ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun deriveMaster(adminCode: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(adminCode, salt, MASTER_ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun wrapKeyBlob(wrapKey: ByteArray, contentKey: ByteArray): ByteArray {
        val wrapIv = randomBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(wrapKey, "AES"), GCMParameterSpec(128, wrapIv))
        val wrapped = cipher.doFinal(contentKey)
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(wrapIv.size.toByte()))
        output.write(wrapIv)
        output.write(wrapped)
        return output.toByteArray()
    }

    private fun unwrapKey(wrapKey: ByteArray, blob: ByteArray): ByteArray {
        val input = ByteArrayInputStream(blob)
        val ivLen = input.read()
        require(ivLen > 0) { "Invalid wrap IV len" }
        val iv = readBytes(input, ivLen)
        val wrapped = input.readBytes()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(wrapKey, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(wrapped)
    }

    private fun cipherInputToOutput(cipher: Cipher, input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read = input.read(buffer)
        while (read >= 0) {
            if (read > 0) {
                val chunk = cipher.update(buffer, 0, read)
                if (chunk != null) {
                    output.write(chunk)
                }
            }
            read = input.read(buffer)
        }
        val final = cipher.doFinal()
        output.write(final)
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { secureRandom.nextBytes(it) }

    private fun intToBytes(value: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array()

    private fun shortToBytes(value: Int): ByteArray = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value.toShort()).array()

    private fun bytesToInt(bytes: ByteArray): Int = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int

    private fun bytesToShort(bytes: ByteArray): Short = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).short

    private fun readBytes(input: InputStream, count: Int): ByteArray {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            require(read > 0) { "Unexpected EOF" }
            offset += read
        }
        return buffer
    }

    data class FsfHeader(val headerJson: String, val wrappedKeyLen: Int)
    data class FsfMetadata(
        val wrapKey: ByteArray,
        val fileSalt: ByteArray,
        val headerJson: String,
        val payloadIv: ByteArray
    )

    data class FsfMetadataHeader(
        val originalName: String,
        val mimeType: String,
        val createdAt: Long,
        val originalSize: Long,
        val sha256: String,
        val fileSalt: ByteArray
    )

    companion object {
        private const val KEY_GLOBAL_SALT = "global_salt"
        private const val KEY_ADMIN_VERIFIER = "admin_verifier"
        private const val KEY_WRAP_PREFIX = "wrap_"
        private val MAGIC = byteArrayOf('F'.code.toByte(), 'S'.code.toByte(), 'F'.code.toByte(), '1'.code.toByte())
        private const val VERSION: Byte = 1
        private const val MASTER_ITERATIONS = 200_000
        private const val WRAP_ITERATIONS = 120_000
        private val VERIFY_SUFFIX = "verify".toByteArray(Charsets.UTF_8)
    }
}
