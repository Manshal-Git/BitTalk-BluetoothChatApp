package com.example.bittalk.domain

import javax.crypto.SecretKey

interface EncryptionProvider {
    fun generateKey(): SecretKey
    fun encryptMessage(message: String, secretKey: SecretKey): ByteArray
    fun decryptMessage(encryptedMessage: ByteArray, secretKey: SecretKey): String
}