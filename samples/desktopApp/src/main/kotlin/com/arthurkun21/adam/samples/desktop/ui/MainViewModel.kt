package com.arthurkun21.adam.samples.desktop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.framebuffer.BufferedImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import javax.imageio.ImageIO

internal class MainViewModel : ViewModel() {
    private var connection: AdbConnection? = null
    private var devicePollingJob: Job? = null

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        connect()
    }

    fun onAdbHostChanged(value: String) {
        _uiState.update { it.copy(host = value) }
    }

    fun onAdbPortChanged(value: String) {
        if (value.all(Char::isDigit)) {
            _uiState.update { it.copy(port = value) }
        }
    }

    fun onCommandChanged(command: String) {
        _uiState.update { it.copy(command = command) }
    }

    fun connect() {
        val port = _uiState.value.portNumber
        if (port == null) {
            _uiState.update {
                it.copy(
                    connectionStatus = "ADB server port is required",
                    snackbarMessage = "Enter a valid ADB server port",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                startLocalAdbServerIfNeeded(_uiState.value.host, port)
                val newConnection = createAdbConnection(
                    _uiState.value.host,
                    port,
                )
                connection?.client?.close()
                connection = newConnection
                startDevicePolling(newConnection)
                _uiState.update {
                    it.copy(
                        connectionStatus = "Connected to ADB server at ${newConnection.host}:${newConnection.port}",
                        isConnected = true,
                        inProgress = false,
                        deviceSerial = newConnection.deviceSerial,
                        snackbarMessage = "Connected to ${newConnection.host}:${newConnection.port}; " +
                            newConnection.deviceLabel,
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                devicePollingJob?.cancel()
                devicePollingJob = null
                connection?.client?.close()
                connection = null
                _uiState.update {
                    it.copy(
                        connectionStatus = "Not connected",
                        isConnected = false,
                        inProgress = false,
                        deviceSerial = null,
                        snackbarMessage = "Connection failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun takeScreenshot() {
        val currentConnection = connection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                val screenshot = takeScreenshotInClient(currentConnection)
                _uiState.update {
                    it.copy(
                        screenshot = screenshot,
                        inProgress = false,
                        snackbarMessage = "Screenshot captured",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        snackbarMessage = "Screenshot failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun executeCommand() {
        val currentConnection = connection ?: return
        val command = _uiState.value.command
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                val output = executeCommandInClient(currentConnection, command)
                _uiState.update {
                    it.copy(
                        commandOutput = output,
                        inProgress = false,
                        snackbarMessage = "Command executed",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        commandOutput = "",
                        inProgress = false,
                        snackbarMessage = "Command failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun close() {
        devicePollingJob?.cancel()
        devicePollingJob = null
        connection?.client?.close()
        connection = null
    }

    private suspend fun startLocalAdbServerIfNeeded(
        host: String,
        port: Int,
    ) {
        if (!host.isLocalAdbHost()) return

        val started = withContext(Dispatchers.IO) {
            StartAdbInteractor().execute(serverPort = port)
        }
        check(started) {
            "ADB server is not running and adb could not be started. Check that adb is on PATH or ANDROID_HOME is set."
        }
    }

    private suspend fun createAdbConnection(
        host: String,
        port: Int,
    ): AdbConnection {
        val client = AndroidDebugBridgeClientFactory()
            .apply {
                this.host = InetAddress.getByName(host)
                this.port = port
            }
            .build()

        return try {
            val deviceSerial = findConnectedDeviceSerial(client)

            AdbConnection(
                client = client,
                host = host,
                port = port,
                deviceSerial = deviceSerial,
            )
        } catch (failure: Exception) {
            client.close()
            throw failure
        }
    }

    private fun startDevicePolling(connection: AdbConnection) {
        devicePollingJob?.cancel()
        devicePollingJob = viewModelScope.launch {
            while (isActive && this@MainViewModel.connection?.client === connection.client) {
                try {
                    val deviceSerial = findConnectedDeviceSerial(connection.client)
                    this@MainViewModel.connection = connection.copy(deviceSerial = deviceSerial)
                    _uiState.update {
                        it.copy(
                            deviceSerial = deviceSerial,
                            connectionStatus = "Connected to ADB server at ${connection.host}:${connection.port}",
                        )
                    }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    this@MainViewModel.connection = null
                    _uiState.update {
                        it.copy(
                            connectionStatus = "ADB server disconnected: ${failure.message}",
                            isConnected = false,
                            deviceSerial = null,
                            snackbarMessage = "ADB connection lost: ${failure.message}",
                        )
                    }
                    return@launch
                }

                delay(DEVICE_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun findConnectedDeviceSerial(client: com.malinskiy.adam.AndroidDebugBridgeClient): String? =
        withContext(Dispatchers.IO) {
            client.execute(ListDevicesRequest())
                .firstOrNull { it.state == DeviceState.DEVICE }
                ?.serial
        }

    private suspend fun takeScreenshotInClient(connection: AdbConnection): ByteArray {
        val serial = requireNotNull(connection.deviceSerial) {
            "No connected device was reported by the ADB server"
        }
        val image = withContext(Dispatchers.IO) {
            connection.client.execute(
                request = ScreenCaptureRequest(BufferedImageScreenCaptureAdapter()),
                serial = serial,
            )
        }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    private suspend fun executeCommandInClient(
        connection: AdbConnection,
        command: String,
    ): String {
        val serial = requireNotNull(connection.deviceSerial) {
            "No connected device was reported by the ADB server"
        }
        val result = withContext(Dispatchers.IO) {
            connection.client.execute(ShellCommandRequest(command), serial = serial)
        }
        return buildString {
            append(result.output)
            appendLine()
            append("Exit code: ")
            append(result.exitCode)
        }
    }
}

private fun String.isLocalAdbHost(): Boolean = this == DEFAULT_ADB_HOST || equals("localhost", ignoreCase = true)

private const val DEVICE_POLL_INTERVAL_MS = 1_000L
