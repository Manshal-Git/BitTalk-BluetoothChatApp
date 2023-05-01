package com.example.composeit.Projects.BLE.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bittalk.domain.BTMessage
import com.example.bittalk.presentation.BTUIState
import com.example.bittalk.domain.BlueToothDevice


val paddingLow = 8.dp
val paddingMedium = 16.dp

@Composable
fun DeviceScreen(
    devsState: BTUIState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClickOnDev: (BlueToothDevice) -> Unit,
    onStartServer: () -> Unit,
    toast: (String) -> Unit,
    exitChat: () -> Unit,
    sendMessage: (BTMessage) -> Unit
) {
    LaunchedEffect(key1 = devsState.isConnected, block = {
        if (devsState.isConnected) toast("Connected")
        else toast("Ready for new connections")
    })

    if (devsState.isConnected) {
        ChatScreen(devsState = devsState, send = sendMessage, close = exitChat)
    } else {
        if (devsState.isConnecting) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {

                Text(text = "Connecting")
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        } else {
            Box {
                Column(
                    modifier = Modifier.padding(
                        0.dp,
                        0.dp,
                        0.dp,
                        56.dp,
                    )
                ) {
                    BluetoothDeviceList(
                        sectionName = "Paired Devices",
                        deviceList = devsState.pairedDevList,
                        onClick = onClickOnDev
                    )
                    BluetoothDeviceList(
                        sectionName = "Found Devices",
                        deviceList = devsState.scannedDevList,
                        onClick = onClickOnDev
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = onStartScan) {
                        Text(text = "Start scan")
                    }
                    Button(onClick = onStopScan) {
                        Text(text = "Stop scan")
                    }
                    Button(onClick = onStartServer) {
                        Text(text = "start server")
                    }
                }
            }
        }
    }

}

@Composable
fun BluetoothDeviceList(
    sectionName: String,
    deviceList: List<BlueToothDevice>,
    onClick: (BlueToothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = sectionName,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(paddingMedium)
            )
        }
        items(deviceList) {
            Text(
                text = it.name,
                modifier = Modifier
                    .clickable {
                        onClick(it)
                    }
                    .padding(paddingLow)
            )
        }
    }
}

@Composable
fun ChatScreen(
    devsState: BTUIState,
    send: (BTMessage) -> Unit,
    close: () -> Unit
) {

    Column {
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceAround) {
            Text(text = "Messages")
            Button(onClick = close) {
                Text(text = "Exit chat")
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(devsState.messages) { btMessage ->
                Column {
                    btMessage.message?.let {
                        val rounded = 16.dp
                        Text(
                            text = it,
                            modifier = Modifier
                                .align(
                                    if (btMessage.isMe == true) Alignment.End
                                    else Alignment.Start
                                )
                                .background(
                                    if (btMessage.isMe==true) Color(0, 100, 200)
                                    else Color(0, 50, 200),
                                    RoundedCornerShape(
                                        topEnd = rounded,
                                        topStart = if (btMessage.isMe == true) rounded else 0.dp,
                                        bottomEnd = if (btMessage.isMe == true) 0.dp else rounded,
                                        bottomStart = rounded
                                    )
                                )
                                .padding(8.dp, 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            var message by remember {
                mutableStateOf("")
            }
            TextField(value = message, onValueChange = {
                message = it
            })
            Button(onClick = {
                send(
                    BTMessage(
                        message
                    )
                )
            }) {
                Text(text = "Send")
            }
        }
    }
}