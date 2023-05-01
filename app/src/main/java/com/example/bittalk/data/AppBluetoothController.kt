package com.example.bittalk.data
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.example.bittalk.*
import com.example.bittalk.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
class AppBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter = bluetoothManager?.adapter
    var bluetoothServerSocket: BluetoothServerSocket? = null
    var bluetoothClientSocket: BluetoothSocket? = null
    var bluetoothDataTransferService : BTDataTransferService? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> get() = _isConnected.asStateFlow()
    private val _scannedDevices = MutableStateFlow<List<BlueToothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BlueToothDevice>> get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BlueToothDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BlueToothDevice>> get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDevReceiver = BluetoothDeviceReceiver { device ->
        val newDev = device.toBlueToothDevice()
        _scannedDevices.update { devices ->
            if (newDev in devices)
                devices
            else devices + newDev
        }
        Toast.makeText(context, device.toString(), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private val stateReceiver = BluetoothStateReceiver { isConnected, dev ->
        if (!checkPermission(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT
                else Manifest.permission.BLUETOOTH
            )
        ) return@BluetoothStateReceiver

        if (bluetoothAdapter?.bondedDevices?.contains(dev) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.tryEmit("Pair devices before connect")
            }
        }
    }

    init {
        updatePairedDevices()
        context.registerReceiver(
            stateReceiver,
            IntentFilter().apply {
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            }
        )
    }

    override fun startDiscovery() {
        if (!checkPermission(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN
                else Manifest.permission.BLUETOOTH
            )
        ) return

        context.registerReceiver(
            foundDevReceiver,
            IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND)
        )
        updatePairedDevices()
        val isStarted = bluetoothAdapter?.startDiscovery()
        println(isStarted)
    }

    override fun startServer(): Flow<ConnectionResult> {
        return flow {
            if (!checkPermission(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT
                    else Manifest.permission.BLUETOOTH
                )
            ) {
                throw SecurityException("No BT_CONNECT permission")
            }
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(Constants.SERVICE_UUID)
            ).also {
                bluetoothServerSocket = it
            }
            var isServing = true
            while (isServing) {
                bluetoothClientSocket = try {
                    bluetoothServerSocket?.accept()
                } catch (e: IOException) {
                    isServing = false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                bluetoothClientSocket?.let {
                    bluetoothServerSocket?.close()
                    val service = BTDataTransferService(it)
                    bluetoothDataTransferService = service
                    emitAll(
                        service.listenForMessages().map {
                            ConnectionResult.TransferSucceed(it)
                        }
                    )
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectDevice(device: BluetoothDevice): Flow<ConnectionResult> {
        return flow {
            if (!checkPermission(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Manifest.permission.BLUETOOTH_CONNECT
                    else
                        Manifest.permission.BLUETOOTH
                )
            ) {
                throw SecurityException("No BT_CONNECT permission")
            }
            bluetoothClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(Constants.SERVICE_UUID.toUUID())

            stopDiscovery()
            bluetoothClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)
                    val service = BTDataTransferService(socket)
                    bluetoothDataTransferService = service
                    emitAll(
                        service.listenForMessages().map {
                            ConnectionResult.TransferSucceed(it)
                        }
                    )
                } catch (e: IOException) {
                    socket.close()
                    bluetoothClientSocket = null
                    emit(ConnectionResult.Error("Connection interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun closeConnection() {
        bluetoothClientSocket?.close()
        bluetoothServerSocket?.close()
        bluetoothClientSocket = null
        bluetoothServerSocket = null
    }


    override fun stopDiscovery() {
        if (!checkPermission(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN
                else Manifest.permission.BLUETOOTH
            )
        ) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun release() {
        context.unregisterReceiver(foundDevReceiver)
        context.unregisterReceiver(stateReceiver)
        closeConnection()
    }

    private fun updatePairedDevices() {
        if (!checkPermission(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN
                else Manifest.permission.BLUETOOTH
            )
        ) return
        bluetoothAdapter?.bondedDevices?.map {
            it.toBlueToothDevice()
        }?.also { devices ->
            _pairedDevices.update { devices }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

}

private fun String.toUUID(): UUID? {
    return UUID.fromString(this)
}
