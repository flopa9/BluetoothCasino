
package com.example.bluetoothcasino

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

private val APP_UUID: UUID =
    UUID.fromString("b7e4a3d2-7c91-4d8e-9f12-6a5b4c3d2e10")

class MainActivity : ComponentActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        setContent {
            MaterialTheme {
                BluetoothScreen(
                    activity = this,
                    adapter = bluetoothAdapter
                )
            }
        }
    }
}

@Composable
private fun BluetoothScreen(
    activity: Activity,
    adapter: BluetoothAdapter?
) {
    val context = activity
    val scope = rememberCoroutineScope()

    var status by remember {
        mutableStateOf("Bluetooth готов к настройке")
    }

    var devices by remember {
        mutableStateOf<List<BluetoothDevice>>(emptyList())
    }

    var connectedName by remember {
        mutableStateOf("")
    }

    var socket by remember {
        mutableStateOf<BluetoothSocket?>(null)
    }

    var serverSocket by remember {
        mutableStateOf<BluetoothServerSocket?>(null)
    }

    var discovering by remember {
        mutableStateOf(false)
    }

    var permissionGranted by remember {
        mutableStateOf(false)
    }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            permissionGranted =
                requiredPermissions.all {
                    result[it] == true ||
                        ContextCompat.checkSelfPermission(
                            context,
                            it
                        ) == PackageManager.PERMISSION_GRANTED
                }

            status = if (permissionGranted) {
                "Разрешения получены"
            } else {
                "Нужны разрешения Bluetooth"
            }
        }

    fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        if (checkPermissions()) {
            permissionGranted = true
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    val receiver = remember {
        object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            if (Build.VERSION.SDK_INT >= 33) {
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE
                                )
                            }

                        if (device != null) {
                            devices = (devices + device)
                                .distinctBy { it.address }
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        discovering = false
                        status = "Поиск завершён"
                    }

                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        status = "Статус сопряжения изменён"
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }

            try {
                adapter?.cancelDiscovery()
                socket?.close()
                serverSocket?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun enableBluetooth() {
        if (adapter == null) {
            status = "На устройстве нет Bluetooth"
            return
        }

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!adapter.isEnabled) {
            val intent = Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE
            )
            activity.startActivity(intent)
        } else {
            status = "Bluetooth включён"
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (adapter == null) {
            status = "Bluetooth недоступен"
            return
        }

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!adapter.isEnabled) {
            status = "Сначала включи Bluetooth"
            return
        }

        devices = emptyList()
        discovering = true
        status = "Ищем телефоны..."

        try {
            adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: Exception) {
            discovering = false
            status = "Ошибка поиска: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun showPairedDevices() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        try {
            devices = adapter?.bondedDevices?.toList() ?: emptyList()
            status = "Сопряжённые устройства"
        } catch (e: Exception) {
            status = "Ошибка: ${e.message}"
        }
    }

    fun createRoom() {
        if (adapter == null) {
            status = "Bluetooth недоступен"
            return
        }

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!adapter.isEnabled) {
            status = "Сначала включи Bluetooth"
            return
        }

        val discoverableIntent = Intent(
            BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE
        ).apply {
            putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                300
            )
        }

        activity.startActivity(discoverableIntent)

        scope.launch {
            try {
                status = "Создаём комнату..."

                val server = withContext(Dispatchers.IO) {
                    adapter.listenUsingRfcommWithServiceRecord(
                        "BluetoothMultiplayer",
                        APP_UUID
                    )
                }

                serverSocket = server
                status = "Комната создана. Ждём игрока..."

                val accepted = withContext(Dispatchers.IO) {
                    server.accept()
                }

                socket = accepted
                serverSocket = null

                connectedName = try {
                    accepted.remoteDevice.name ?: "Игрок"
                } catch (_: Exception) {
                    "Игрок"
                }

                status = "Подключён: $connectedName"

            } catch (e: Exception) {
                status = "Ошибка комнаты: ${e.message}"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (adapter == null) return

        scope.launch {
            try {
                status = "Подключаемся..."

                adapter.cancelDiscovery()

                val newSocket = withContext(Dispatchers.IO) {
                    device.createRfcommSocketToServiceRecord(APP_UUID)
                        .also { it.connect() }
                }

                socket = newSocket
                connectedName = device.name ?: "Игрок"
                status = "Подключён: $connectedName"

            } catch (e: Exception) {
                status = "Не удалось подключиться: ${e.message}"
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
            serverSocket?.close()
        } catch (_: IOException) {
        }

        socket = null
        serverSocket = null
        connectedName = ""
        status = "Отключено"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10151D))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "BLUETOOTH",
            color = Color.White,
            fontSize = 30.sp
        )

        Text(
            text = "Мультиплеер",
            color = Color(0xFF54D6A0),
            fontSize = 18.sp
        )

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF202B38)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "Состояние",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = status,
                    color = Color.White,
                    fontSize = 17.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        GameButton(
            text = "Включить Bluetooth",
            onClick = { enableBluetooth() }
        )

        GameButton(
            text = "Создать комнату",
            onClick = { createRoom() }
        )

        GameButton(
            text = if (discovering) {
                "Идёт поиск..."
            } else {
                "Найти телефоны"
            },
            onClick = { startDiscovery() }
        )

        GameButton(
            text = "Мои устройства",
            onClick = { showPairedDevices() }
        )

        GameButton(
            text = "Отключиться",
            onClick = { disconnect() }
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "НАЙДЕННЫЕ УСТРОЙСТВА",
            color = Color(0xFF54D6A0),
            fontSize = 15.sp
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = devices,
                key = { it.address }
            ) { device ->

                val name = try {
                    device.name ?: "Неизвестное устройство"
                } catch (_: SecurityException) {
                    "Устройство"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable {
                            connectToDevice(device)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF263747)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = name,
                            color = Color.White
                        )

                        Text(
                            text = device.address,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Нажми для подключения",
                            color = Color(0xFF54D6A0),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF176B50)
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp
        )
    }
}
