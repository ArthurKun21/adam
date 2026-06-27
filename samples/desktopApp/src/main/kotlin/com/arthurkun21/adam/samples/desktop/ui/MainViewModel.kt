package com.arthurkun21.adam.samples.desktop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.Const
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.request.SerialTarget
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.framebuffer.BufferedImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.logcat.LogcatBuffer
import com.malinskiy.adam.request.logcat.LogcatReadMode
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.logcat.SyncLogcatRequest
import com.malinskiy.adam.request.misc.ConnectDeviceRequest
import com.malinskiy.adam.request.misc.Device
import com.malinskiy.adam.request.misc.DisconnectDeviceRequest
import com.malinskiy.adam.request.misc.PairDeviceRequest
import com.malinskiy.adam.request.misc.ReconnectRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
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
import java.time.Instant
import java.time.ZoneId
import javax.imageio.ImageIO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.time.Duration as JavaDuration

/**
 * Small state holder for the desktop sample.
 *
 * The methods intentionally mirror Adam request names so readers can jump from the
 * UI to the relevant library API: connect, reconnect, disconnect, pair, shell,
 * logcat, and screen capture.
 */
internal class MainViewModel : ViewModel() {
    private val adbClient = AndroidDebugBridgeClientFactory()
        .apply {
            connectTimeout = JavaDuration.ofMillis(socketConnectTimeout.inWholeMilliseconds)
            idleTimeout = JavaDuration.ofMillis(socketIdleTimeout.inWholeMilliseconds)
        }
        .build()

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

    fun onPairingHostChanged(value: String) {
        _uiState.update { it.copy(pairingHost = value) }
    }

    fun onPairingPortChanged(value: String) {
        if (value.all(Char::isDigit)) {
            _uiState.update { it.copy(pairingPort = value) }
        }
    }

    fun onPairingCodeChanged(value: String) {
        _uiState.update { it.copy(pairingCode = value) }
    }

    fun onCommandChanged(command: String) {
        _uiState.update { it.copy(command = command) }
    }

    fun connect() {
        val host = _uiState.value.host.trim()
        val port = _uiState.value.portNumber ?: run {
            validateDeviceEndpoint(host, null)
            return
        }
        if (!validateDeviceEndpoint(host, port)) return

        viewModelScope.launch {
            log.info { "Connecting to ADB device at $host:$port" }
            _uiState.update {
                it.copy(
                    inProgress = true,
                    connectionStatus = "Connecting to ADB device at $host:$port",
                )
            }

            try {
                ensureAdbServerStarted()
                val newConnection = connectDeviceOverTcp(host, port)
                startDevicePolling()
                log.info { "Connected to ADB device at $host:$port" }
                _uiState.update {
                    it.copy(
                        connectionStatus = connectionStatusFor(newConnection),
                        isConnected = true,
                        inProgress = false,
                        deviceSerial = newConnection,
                        snackbarMessage = "Connected to $host:$port; " +
                            "device ${newConnection ?: "not found"}",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                devicePollingJob?.cancel()
                devicePollingJob = null
                log.error(failure) { "Failed to connect to ADB device at $host:$port" }
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

    fun reconnect() {
        viewModelScope.launch {
            val selectedSerial = _uiState.value.deviceSerial
            log.info { "Reconnecting ${selectedSerial ?: "first available ADB device"}" }
            _uiState.update {
                it.copy(
                    inProgress = true,
                    connectionStatus = "Reconnecting ${selectedSerial ?: "first available ADB device"}",
                )
            }

            try {
                ensureAdbServerStarted()
                val output = reconnectDevice(selectedSerial)
                val deviceSerial = findConnectedDeviceSerial()
                startDevicePolling()
                _uiState.update {
                    it.copy(
                        connectionStatus = connectionStatusFor(deviceSerial),
                        isConnected = true,
                        inProgress = false,
                        deviceSerial = deviceSerial,
                        snackbarMessage = "Reconnect requested: $output",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                log.error(failure) { "ADB reconnect failed" }
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        snackbarMessage = "Reconnect failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun disconnect() {
        val host = _uiState.value.host.trim()
        val port = _uiState.value.portNumber ?: run {
            validateDeviceEndpoint(host, null)
            return
        }
        if (!validateDeviceEndpoint(host, port)) return

        viewModelScope.launch {
            log.info { "Disconnecting ADB device at $host:$port" }
            _uiState.update {
                it.copy(
                    inProgress = true,
                    connectionStatus = "Disconnecting ADB device at $host:$port",
                )
            }

            try {
                ensureAdbServerStarted()
                val output = disconnectDevice(host, port)
                val deviceSerial = findConnectedDeviceSerial()
                _uiState.update {
                    it.copy(
                        connectionStatus = connectionStatusFor(deviceSerial),
                        isConnected = deviceSerial != null,
                        inProgress = false,
                        deviceSerial = deviceSerial,
                        snackbarMessage = "Disconnected $host:$port: $output",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                log.error(failure) { "ADB disconnect failed for $host:$port" }
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        snackbarMessage = "Disconnect failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun pair() {
        val host = _uiState.value.pairingHost.trim()
        val port = _uiState.value.pairingPortNumber
        val pairingCode = _uiState.value.pairingCode.trim()
        if (!validatePairingEndpoint(host, port, pairingCode)) return

        viewModelScope.launch {
            val address = "$host:$port"
            log.info { "Pairing ADB device at $address" }
            _uiState.update {
                it.copy(
                    inProgress = true,
                    connectionStatus = "Pairing ADB device at $address",
                )
            }

            try {
                ensureAdbServerStarted()
                val output = pairDevice(address, pairingCode)
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        snackbarMessage = "Pairing result: $output",
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                log.error(failure) { "ADB pairing failed for $address" }
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        snackbarMessage = "Pairing failed: ${failure.message}",
                    )
                }
            }
        }
    }

    fun takeScreenshot() {
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                log.info { "Capturing screenshot from ${_uiState.value.deviceLabel}" }
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

    fun updateSerial(serial: String) {
        _uiState.update { it.copy(deviceSerial = serial) }
    }

    fun executeCommand() {
        val command = _uiState.value.command
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                log.info { "Executing shell command on ${_uiState.value.deviceLabel}: $command" }
                val output = executeCommandInClient(command)
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
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true) }

            try {
                log.info { "Fetching recent logcat from ${_uiState.value.deviceLabel}" }
                val output = fetchLogcatInClient()
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
        viewModelScope.cancel()
        adbClient.close()
    }

    private suspend fun ensureAdbServerStarted() {
        val started = withAdbTimeout("starting local ADB server on port ${Const.DEFAULT_ADB_PORT}") {
            withContext(Dispatchers.IO) {
                StartAdbInteractor().execute()
            }
        }
        check(started) {
            "ADB server is not running and adb could not be started. Check that adb is on PATH or ANDROID_HOME is set."
        }
    }

    private fun validateDeviceEndpoint(host: String, port: Int?): Boolean {
        if (host.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionStatus = "Device host is required",
                    snackbarMessage = "Enter a device host",
                )
            }
            return false
        }
        if (port == null) {
            _uiState.update {
                it.copy(
                    connectionStatus = "Device port is required",
                    snackbarMessage = "Enter a valid device port",
                )
            }
            return false
        }
        return true
    }

    private fun validatePairingEndpoint(host: String, port: Int?, pairingCode: String): Boolean {
        if (host.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionStatus = "Pairing host is required",
                    snackbarMessage = "Enter a pairing host",
                )
            }
            return false
        }
        if (port == null) {
            _uiState.update {
                it.copy(
                    connectionStatus = "Pairing port is required",
                    snackbarMessage = "Enter the pairing port shown on the device",
                )
            }
            return false
        }
        if (pairingCode.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionStatus = "Pairing code is required",
                    snackbarMessage = "Enter the pairing code shown on the device",
                )
            }
            return false
        }
        return true
    }

    private suspend fun connectDeviceOverTcp(host: String, port: Int): String? {
        val output = withAdbTimeout("connecting ADB device at $host:$port") {
            withContext(Dispatchers.IO) {
                adbClient.execute(
                    ConnectDeviceRequest(host, port),
                )
            }
        }
        log.info { "ADB connect output: $output" }

        check(!output.startsWith("failed", ignoreCase = true)) { output }

        return findConnectedDeviceSerial()
    }

    private suspend fun reconnectDevice(serial: String?): String {
        return withAdbTimeout("reconnecting ADB device") {
            withContext(Dispatchers.IO) {
                if (serial == null) {
                    adbClient.execute(ReconnectRequest())
                } else {
                    adbClient.execute(
                        request = ReconnectRequest(
                            reconnectTarget = Device,
                            target = SerialTarget(serial),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun disconnectDevice(host: String, port: Int): String {
        return withAdbTimeout("disconnecting ADB device") {
            withContext(Dispatchers.IO) {
                adbClient.execute(DisconnectDeviceRequest(host, port))
            }
        }
    }

    private suspend fun pairDevice(address: String, pairingCode: String): String {
        return withAdbTimeout("pairing ADB device") {
            withContext(Dispatchers.IO) {
                adbClient.execute(PairDeviceRequest(address, pairingCode))
            }
        }
    }

    private fun startDevicePolling() {
        devicePollingJob?.cancel()
        devicePollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val deviceSerial = findConnectedDeviceSerial(logDevices = false)
                    _uiState.update {
                        it.copy(
                            deviceSerial = deviceSerial,
                            isConnected = true,
                            connectionStatus = connectionStatusFor(deviceSerial),
                        )
                    }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    log.error(failure) { "ADB device polling failed" }
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

                delay(devicePollInterval)
            }
        }
    }

    private suspend fun findConnectedDeviceSerial(logDevices: Boolean = true): String? {
        val devices = withAdbTimeout("listing connected ADB devices") {
            withContext(Dispatchers.IO) {
                adbClient.execute(ListDevicesRequest())
            }
        }
        val sortedDevices = devices
            .sortedBy { it.serial.contains("emulator", ignoreCase = true) }

        _uiState.update { current ->
            current.copy(deviceSerialList = devices.map { it.serial })
        }

        if (logDevices) {
            val deviceSummary = sortedDevices.joinToString { "${it.serial}:${it.state.name.lowercase()}" }
                .ifBlank { "none" }
            log.info { "ADB devices: $deviceSummary" }
        }

        return sortedDevices.firstOrNull { it.state == DeviceState.DEVICE }?.serial
    }

    private suspend fun takeScreenshotInClient(): ByteArray? {
        val serial = _uiState.value.deviceSerial ?: run {
            log.error {
                "Screenshot capture failed: no connected device"
            }
            return null
        }

        val adapter = BufferedImageScreenCaptureAdapter()

        val image = withAdbTimeout("capturing screenshot", screenshotTimeout) {
            withContext(Dispatchers.IO) {
                adbClient.execute(
                    request = ScreenCaptureRequest(adapter),
                    serial = serial,
                )
            }
        }
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(image, "png", output)) { "No PNG image writer is available" }
            output.toByteArray()
        }
    }

    private suspend fun executeCommandInClient(command: String): String {
        val serial = _uiState.value.deviceSerial ?: return ""

        val result = withAdbTimeout("executing shell command") {
            withContext(Dispatchers.IO) {
                adbClient.execute(
                    ShellCommandRequest(command),
                    serial = serial,
                )
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

    private suspend fun fetchLogcatInClient(): String {
        val serial = _uiState.value.deviceSerial ?: return ""

        val since = LogcatSinceFormat.DateStringYear(
            Instant.now().minusSeconds(LOGCAT_LOOKBACK_SECONDS),
            ZoneId.systemDefault().id,
        )
        val output = withAdbTimeout("fetching recent logcat") {
            withContext(Dispatchers.IO) {
                adbClient.execute(
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
        timeoutMillis: Duration = adbOperationTimeout,
        block: suspend () -> T,
    ): T = try {
        withTimeout(timeoutMillis) {
            block()
        }
    } catch (failure: TimeoutCancellationException) {
        throw IllegalStateException(
            "Timed out while $operation after ${timeoutMillis.inWholeSeconds}s",
            failure,
        )
    }

    private fun connectionStatusFor(deviceSerial: String?): String = buildString {
        append("Connected to ADB server")
        if (deviceSerial == null) {
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

private val devicePollInterval = 5.seconds
private val adbOperationTimeout = 15.seconds
private val screenshotTimeout = 30.seconds
private val socketConnectTimeout = 5.seconds
private val socketIdleTimeout = 15.seconds
private const val LOGCAT_LOOKBACK_SECONDS = 120L
private const val MAX_LOGCAT_OUTPUT_LENGTH = 20_000
