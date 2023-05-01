package com.example.bittalk.data

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BluetoothDeviceReceiver(private val onDeviceFound : (BluetoothDevice) -> Unit) : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
        if (intent != null) {
            when(intent.action){
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    device?.let { onDeviceFound(it) }
                }
            }
        }
    }
}