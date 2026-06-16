package com.arthurkun21.adam.samples.desktop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.framebuffer.BufferedImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.logcat.LogcatBuffer
import com.malinskiy.adam.request.logcat.LogcatReadMode
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.logcat.SyncLogcatRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.imageio.ImageIO

internal class MainViewModel : ViewModel() {
    private var connection: AdbConnection? = null
    private var devicePollingJob: Job? = null
    private val log = AdamLogging.logger {}

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        log.info { "Starting Adam desktop sample" }
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
        val host = _uiState.value.host.trim()
        val port = _uiState.value.portNumber
        if (host.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionStatus = "ADB server host is required",
                    snackbarMessage = "Enter an ADB server host",
                )
            }
            return
        }
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
            log.info { "Connecting to ADB server at $host:$port" }
            _uiState.update {
                it.copy(
                    inProgress = true,
                    connectionStatus = "Connecting to ADB server at $host:$port",
                )
            }

            try {
                startLocalAdbServerIfNeeded(host, port)
                val newConnection = createAdbConnection(
                    host,
                    port,
                )
                connection?.client?.close()
                connection = newConnection
                startDevicePolling(newConnection)
                log.info { "Connected to ADB server at ${newConnection.host}:${newConnection.port}" }
                _uiState.update {
                    it.copy(
                        connectionStatus = connectionStatusFor(newConnection),
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
                log.error(failure) { "Failed to connect to ADB server at $host:$port" }
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
                log.info { "Capturing screenshot from ${currentConnection.deviceLabel}" }
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
                log.error(failure) { "Screenshot capture failed" }
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
                log.info { "Executing shell command on ${currentConnection.deviceLabel}: $command" }
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
                log.error(failure) { "Shell command failed: $command" }
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

    fun fetchLogcat() {
        val currentConnection = connection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                log.info { "Fetching recent logcat from ${currentConnection.deviceLabel}" }
                val output = fetchLogcatInClient(currentConnection)
                _uiState.update {
                    it.copy(
                        logcatOutput = output,
                        inProgress = false,
                        snackbarMessage = "Logcat fetched",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                log.error(failure) { "Logcat fetch failed" }
                _uiState.update {
                    it.copy(
                        logcatOutput = "",
                        inProgress = false,
                        snackbarMessage = "Logcat failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun close() {
        log.info { "Closing Adam desktop sample ADB connection" }
        devicePollingJob?.cancel()
        devicePollingJob = null
        connection?.client?.close()
        connection = null
    }

    private suspend fun startLocalAdbServerIfNeeded(
        host: String,
        port: Int,
    ) {
        if (!host.isLocalAdbHost()) {
            log.info { "Skipping local adb start for remote ADB server $host:$port" }
            return
        }

        val started = withAdbTimeout("starting local ADB server on port $port") {
            withContext(Dispatchers.IO) {
                StartAdbInteractor().execute(serverPort = port)
            }
        }
        check(started) {
            "ADB server is not running and adb could not be started. Check that adb is on PATH or ANDROID_HOME is set."
        }
    }

    private suspend fun createAdbConnection(
        host: String,
        port: Int,
    ): AdbConnection {
        val adbHost = withAdbTimeout("resolving ADB host $host") {
            withContext(Dispatchers.IO) {
                InetAddress.getByName(host)
            }
        }
        val client = AndroidDebugBridgeClientFactory()
            .apply {
                this.host = adbHost
                this.port = port
                connectTimeout = Duration.ofMillis(SOCKET_CONNECT_TIMEOUT_MS)
                idleTimeout = Duration.ofMillis(SOCKET_IDLE_TIMEOUT_MS)
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
                    val deviceSerial = findConnectedDeviceSerial(connection.client, logDevices = false)
                    val updatedConnection = connection.copy(deviceSerial = deviceSerial)
                    this@MainViewModel.connection = updatedConnection
                    _uiState.update {
                        it.copy(
                            deviceSerial = deviceSerial,
                            isConnected = true,
                            connectionStatus = connectionStatusFor(updatedConnection),
                        )
                    }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    log.error(failure) { "ADB device polling failed" }
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

    private suspend fun findConnectedDeviceSerial(
        client: AndroidDebugBridgeClient,
        logDevices: Boolean = true,
    ): String? {
        val devices = withAdbTimeout("listing connected ADB devices") {
            withContext(Dispatchers.IO) {
                client.execute(ListDevicesRequest())
            }
        }
        if (logDevices) {
            val deviceSummary = devices.joinToString { "${it.serial}:${it.state.name.lowercase()}" }
                .ifBlank { "none" }
            log.info { "ADB devices: $deviceSummary" }
        }
        return devices.firstOrNull { it.state == DeviceState.DEVICE }?.serial
    }

    private suspend fun takeScreenshotInClient(connection: AdbConnection): ByteArray {
        val serial = requireNotNull(connection.deviceSerial) {
            "No connected device was reported by the ADB server"
        }
        val image = withAdbTimeout("capturing screenshot", SCREENSHOT_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                connection.client.execute(
                    request = ScreenCaptureRequest(BufferedImageScreenCaptureAdapter()),
                    serial = serial,
                )
            }
        }
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(image, "png", output)) { "No PNG image writer is available" }
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
        val result = withAdbTimeout("executing shell command") {
            withContext(Dispatchers.IO) {
                connection.client.execute(ShellCommandRequest(command), serial = serial)
            }
        }
        if (result.exitCode != 0) {
            log.warn { "Shell command exited with code ${result.exitCode}: $command" }
        }
        return buildString {
            append(result.output)
            appendLine()
            append("Exit code: ")
            append(result.exitCode)
        }
    }

    private suspend fun fetchLogcatInClient(connection: AdbConnection): String {
        val serial = requireNotNull(connection.deviceSerial) {
            "No connected device was reported by the ADB server"
        }
        val since = LogcatSinceFormat.DateStringYear(
            Instant.now().minusSeconds(LOGCAT_LOOKBACK_SECONDS),
            ZoneId.systemDefault().id,
        )
        val output = withAdbTimeout("fetching recent logcat") {
            withContext(Dispatchers.IO) {
                connection.client.execute(
                    request = SyncLogcatRequest(
                        since = since,
                        modes = listOf(LogcatReadMode.time),
                        buffers = listOf(LogcatBuffer.default),
                    ),
                    serial = serial,
                )
            }
        }
        return output.trimForLogcat()
    }

    private suspend fun <T> withAdbTimeout(
        operation: String,
        timeoutMillis: Long = ADB_OPERATION_TIMEOUT_MS,
        block: suspend () -> T,
    ): T = try {
        withTimeout(timeoutMillis) {
            block()
        }
    } catch (failure: TimeoutCancellationException) {
        throw IllegalStateException(
            "Timed out while $operation after ${timeoutMillis / 1_000}s",
            failure,
        )
    }

    private fun connectionStatusFor(connection: AdbConnection): String = buildString {
        append("Connected to ADB server at ${connection.host}:${connection.port}")
        if (connection.deviceSerial == null) {
            append("; waiting for a device")
        }
    }

    private fun String.trimForLogcat(): String {
        val trimmed = trim()
        if (trimmed.isBlank()) {
            return "No logcat entries found in the last ${LOGCAT_LOOKBACK_SECONDS}s."
        }
        if (trimmed.length <= MAX_LOGCAT_OUTPUT_LENGTH) return trimmed

        return buildString {
            append("Showing the last $MAX_LOGCAT_OUTPUT_LENGTH characters of recent logcat output:")
            appendLine()
            append(trimmed.takeLast(MAX_LOGCAT_OUTPUT_LENGTH))
        }
    }
}

private fun String.isLocalAdbHost(): Boolean = this == DEFAULT_ADB_HOST || equals("localhost", ignoreCase = true)

private const val DEVICE_POLL_INTERVAL_MS = 1_000L
private const val ADB_OPERATION_TIMEOUT_MS = 15_000L
private const val SCREENSHOT_TIMEOUT_MS = 30_000L
private const val SOCKET_CONNECT_TIMEOUT_MS = 5_000L
private const val SOCKET_IDLE_TIMEOUT_MS = 15_000L
private const val LOGCAT_LOOKBACK_SECONDS = 120L
private const val MAX_LOGCAT_OUTPUT_LENGTH = 20_000
