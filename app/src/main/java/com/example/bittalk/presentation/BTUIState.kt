package com.example.bittalk.presentation

import com.example.bittalk.domain.BTMessage
import com.example.bittalk.domain.BlueToothDevice

data class BTUIState (
    val scannedDevList : List<BlueToothDevice> = emptyList(),
    val pairedDevList : List<BlueToothDevice> = emptyList(),
    val isConnected : Boolean = false,
    val isConnecting : Boolean = false,
    val error : String? = null,
    val messages : List<BTMessage> = emptyList()
)