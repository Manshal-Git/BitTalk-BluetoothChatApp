package com.example.bittalk.data

import android.annotation.SuppressLint
import com.example.bittalk.domain.BlueToothDevice


@SuppressLint("MissingPermission")
fun android.bluetooth.BluetoothDevice.toBlueToothDevice() = BlueToothDevice(
    name = this.name,
    address = this.address
)