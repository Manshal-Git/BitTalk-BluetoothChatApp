package com.example.bittalk.domain

typealias BlueToothDevice = BluetoothDevice

data class BluetoothDevice (
    val name : String,
    val address : String
)