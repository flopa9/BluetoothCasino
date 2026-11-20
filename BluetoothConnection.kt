
package com.example.bluetoothcasino

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothServerSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class BluetoothConnection {

    companion object {
        private val APP_UUID: UUID =
            UUID.fromString("b7e4a3d2-7c91-4d8e-9f12-6a5b4c3d2e10")

        private const val APP_NAME = "BluetoothCardGame"
    }

    private val adapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var socket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null

    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    private val _status = MutableStateFlow("Не подключено")
    val status: StateFlow<String> = _status

    private val _messages = MutableStateFlow("")
    val messages: StateFlow<String> = _messages

    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            _status.value = "Нет разрешения Bluetooth"
            emptyList()
        }
    }

    fun startServer() {
        scope.launch {
            try {
                _status.value = "Ожидание второго телефона..."

                serverSocket =
                    adapter?.listenUsingRfcommWithServiceRecord(
                        APP_NAME,
                        APP_UUID
                    )

                val connectedSocket = serverSocket?.accept()

                if (connectedSocket != null) {
                    setupConnection(connectedSocket)
                }

                serverSocket?.close()
                serverSocket = null

            } catch (e: Exception) {
                _status.value = "Ошибка комнаты: ${e.message}"
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        scope.launch {
            try {
                _status.value = "Подключение..."

                adapter?.cancelDiscovery()

                val newSocket =
                    device.createRfcommSocketToServiceRecord(APP_UUID)

                newSocket.connect()

                setupConnection(newSocket)

            } catch (e: Exception) {
                _status.value = "Ошибка подключения: ${e.message}"
            }
        }
    }

    private fun setupConnection(newSocket: BluetoothSocket) {
        socket = newSocket

        writer = OutputStreamWriter(
            newSocket.outputStream,
            Charsets.UTF_8
        )

        reader = BufferedReader(
            InputStreamReader(
                newSocket.inputStream,
                Charsets.UTF_8
            )
        )

        _status.value = "Bluetooth подключён!"

        listenForMessages()
    }

    private fun listenForMessages() {
        scope.launch {
            try {
                while (socket?.isConnected == true) {
                    val message = reader?.readLine() ?: break
                    _messages.value = message
                }

                _status.value = "Соединение закрыто"

            } catch (e: Exception) {
                _status.value = "Соединение потеряно"
            }
        }
    }

    fun sendMessage(message: String) {
        scope.launch {
            try {
                writer?.apply {
                    write(message)
                    write("\n")
                    flush()
                }
            } catch (e: Exception) {
                _status.value = "Ошибка отправки"
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                serverSocket?.close()
                socket?.close()
                writer?.close()
                reader?.close()
            } catch (_: Exception) {
            }

            socket = null
            serverSocket = null
            writer = null
            reader = null

            _status.value = "Не подключено"
        }
    }
}
