package com.example.bittalk.data

import com.example.bittalk.domain.EncryptionProvider
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AES : EncryptionProvider{
    override fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    override fun encryptMessage(message: String, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))
    }

    override fun decryptMessage(encryptedMessage: ByteArray, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(encryptedMessage)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }
}