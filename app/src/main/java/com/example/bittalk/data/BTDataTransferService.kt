package com.example.bittalk.data

import android.bluetooth.BluetoothSocket
import com.example.bittalk.domain.BTMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BTDataTransferService(private val socket: BluetoothSocket) {
    suspend fun sendMessage(bytes : ByteArray) : Boolean{
        return withContext(Dispatchers.IO){
            try {
                socket.outputStream.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false
            }
            true
        }
    }

    fun listenForMessages(): Flow<BTMessage> {
        return flow<BTMessage> {
            if (!socket.isConnected) return@flow
            val buffer = ByteArray(1024)
            while (true) {
                val readCount = socket.inputStream.read(buffer)
                emit(
                    BTMessage().fromString(buffer.decodeToString(endIndex = readCount),false)
                )
            }
        }.flowOn(Dispatchers.IO)
    }
}