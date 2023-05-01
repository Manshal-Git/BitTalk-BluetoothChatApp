package com.example.bittalk


import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.bittalk.presentation.BTViewModel
import com.example.composeit.Projects.BLE.composables.DeviceScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val enableBTLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
            } else {
                // User declined to enable Bluetooth
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if (bluetoothAdapter?.isEnabled == false && canEnableBluetooth) {
                enableBTLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }

        setContent {
            val viewModel = remember { BTViewModel() }
            viewModel.setUpViewModel(applicationContext)
            val state by viewModel.state!!.collectAsState()

            /* val pairedDevState = state.pairedDevList
             val scannedDevState = state.scannedDevList*/

            // A surface container using the 'background' color from the theme
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                DeviceScreen(
                    state, onStartScan = {
                        if (bluetoothAdapter?.isEnabled == false) {
                            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            if (ActivityCompat.checkSelfPermission(
                                    this, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Manifest.permission.BLUETOOTH_SCAN
                                    } else Manifest.permission.BLUETOOTH
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                println("bluetooth not granted")
                                return@DeviceScreen
                            }
                            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && checkSelfPermission(
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                println("location granted")
                                val locationManager =
                                    getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val isGpsEnabled =
                                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                if (!isGpsEnabled) {
                                    startActivityForResult(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                        Constants.ACCESS_LOCATION
                                    )
                                }
                            } else {
                                ActivityCompat.requestPermissions(
                                    this, arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ), 1
                                )
                            }
                            startActivityForResult(enableIntent, Constants.ENABLE_BLUETOOTH)
                        } else {
                            viewModel.startScan()
                        }
                    }, onStopScan = viewModel::stopScan , onStartServer = viewModel::waitForConnectionRequests,
                    onClickOnDev = viewModel::connectToDevice ,
                    toast = {
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    },
                    exitChat = viewModel::disconnectFromDevice,
                    sendMessage = {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.abc?.bluetoothDataTransferService?.sendMessage(
                                it.toBytes()
                            )
                        }
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ENABLE_BLUETOOTH) {
            Toast.makeText(
                this, if (resultCode == RESULT_OK) "Enabled" else "Declined", Toast.LENGTH_SHORT
            ).show()
        }
    }
}
