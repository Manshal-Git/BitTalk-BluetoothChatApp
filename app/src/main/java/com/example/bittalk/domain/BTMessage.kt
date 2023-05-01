package com.example.bittalk.domain

data class BTMessage(
    val message : String? =null,
    val sender : String? =null,
    val isMe : Boolean? =null,
){
    fun fromString(s:String,isMe: Boolean?): BTMessage {
        return BTMessage(
            s.substringAfter("#"),
            s.substringBefore("#"),
            isMe = isMe
        )
    }

    fun toBytes(): ByteArray {
        return "$sender#$message".encodeToByteArray()
    }
}
