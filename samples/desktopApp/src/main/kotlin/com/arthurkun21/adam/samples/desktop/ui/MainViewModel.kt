package com.arthurkun21.adam.samples.desktop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.framebuffer.BufferedImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import javax.imageio.ImageIO

internal class MainViewModel : ViewModel() {
    private val client = AndroidDebugBridgeClientFactory().build()

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    fun onAdbHostChanged(value: String) {
        _uiState.update { it.copy(host = value) }
    }

    fun onAdbPortChanged(value: String) {
        val newValue = value.toIntOrNull() ?: DEFAULT_ADB_PORT
        _uiState.update { it.copy(port = newValue) }
    }

    fun onCommandChanged(command: String) {
        _uiState.update { it.copy(command = command) }
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                client.close()
            } catch (ex: Exception) {
                logcat(LogPriority.ERROR, TAG) {
                    "Failed to close the client: ${ex.asLog()}"
                }
            }

            try {
                val newConnection = createAdbConnection(
                    _uiState.value.host,
                    _uiState.value.port,
                )
                connection?.client?.close()
                connection = newConnection
                _uiState.update {
                    it.copy(
                        connectionStatus = "Connected device: ${newConnection.deviceLabel}",
                        isConnected = true,
                        inProgress = false,
                        snackbarMessage = "Connected to ${newConnection.host}:${newConnection.port}; " +
                            newConnection.deviceLabel,
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                connection?.client?.close()
                connection = null
                _uiState.update {
                    it.copy(
                        connectionStatus = "Not connected",
                        isConnected = false,
                        inProgress = false,
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
                val screenshot = takeScreenshotInClient()
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
                val output = executeCommandInClient()
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
        connection?.client?.close()
        connection = null
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
            val deviceSerial = client.execute(ListDevicesRequest())
                .firstOrNull { it.state == DeviceState.DEVICE }
                ?.serial

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

    suspend fun takeScreenshotInClient(): ByteArray {
        val serial = requireNotNull(_uiState.value.deviceSerial) {
            "No connected device was reported by the ADB server"
        }
        val image = client.execute(
            request = ScreenCaptureRequest(BufferedImageScreenCaptureAdapter()),
            serial = serial,
        )
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    suspend fun executeCommandInClient(command: String): String {
        val serial = requireNotNull(_uiState.value.deviceSerial) {
            "No connected device was reported by the ADB server"
        }
        val result = client.execute(ShellCommandRequest(command), serial = serial)
        return buildString {
            append(result.output)
            appendLine()
            append("Exit code: ")
            append(result.exitCode)
        }
    }
}

private const val TAG = "Adb-ui"
