package com.example.bittalk.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected : StateFlow<Boolean>
    val scannedDevices : StateFlow<List<BlueToothDevice>>
    val pairedDevices : StateFlow<List<BlueToothDevice>>
    val errors : SharedFlow<String>

    fun startDiscovery()
    fun startServer() : Flow<ConnectionResult>
    fun connectDevice(device: BluetoothDevice) : Flow<ConnectionResult>
    fun closeConnection()
    fun stopDiscovery()
    fun release()
}