package com.example.bittalk.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bittalk.data.AppBluetoothController
import com.example.bittalk.domain.BluetoothDevice
import com.example.bittalk.domain.ConnectionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*

class BTViewModel : ViewModel() {

    var abc : AppBluetoothController? = null
    private var connectionJob: Job? = null

    private val _state = MutableStateFlow(BTUIState())
    var state = abc?.let {
        combine(
        it.scannedDevices,
        abc!!.pairedDevices,
        _state
    ){ sd , pd, state ->
        state.copy(
            scannedDevList = sd,
            pairedDevList = pd
        )
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _state.value
    )
    }

    fun setUpViewModel(context : Context){
        abc = AppBluetoothController(context)
        state = combine(
            abc!!.scannedDevices,
            abc!!.pairedDevices,
            _state
        ){ sd , pd, state ->
            state.copy(
                scannedDevList = sd,
                pairedDevList = pd
            )
        }.stateIn(viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _state.value
        )
        abc!!.isConnected.onEach { isConnected ->
            _state.update { it.copy( isConnected =  isConnected ) }
        }.launchIn(viewModelScope)

        abc!!.errors.onEach {  }
    }

    fun Flow<ConnectionResult>.listen() : Job {
        return onEach { result ->
            when(result){
                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            error = result.message
                        )
                    }
                }
                ConnectionResult.ConnectionEstablished ->{
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            error = null
                        )
                    }
                }
                is ConnectionResult.TransferSucceed -> {
                    _state.update {
                        it.copy(
                            messages = it.messages + result.message
                        )
                    }
                }
            }
        }.catch {t->
            abc?.closeConnection()
            _state.update {
                it.copy(
                    isConnecting = false,
                    isConnected = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(dev : BluetoothDevice){
        _state.update { it.copy(isConnecting = true) }
        connectionJob = abc?.connectDevice(dev)?.listen()
    }

    fun disconnectFromDevice(){
        connectionJob?.cancel()
        abc?.closeConnection()
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false
            )
        }
    }

    fun waitForConnectionRequests(){
        _state.update {
            it.copy(
                isConnecting = true
            )
        }
        connectionJob = abc?.startServer()?.listen()
    }
    fun startScan(){
        abc?.startDiscovery()
    }
    fun stopScan(){
        abc?.stopDiscovery()
    }
}