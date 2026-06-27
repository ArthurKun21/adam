package com.arthurkun21.adam.samples.desktop.ui

/**
 * Defaults used by Android emulators and devices listening for ADB over TCP.
 *
 * This is intentionally not the ADB server port. Adam talks to the local ADB
 * server on its own default port, and the server connects to this device endpoint.
 */
internal const val DEFAULT_ADB_HOST = "127.0.0.1"
internal const val DEFAULT_ADB_PORT = 5555

/**
 * Immutable UI model for the desktop sample.
 *
 * Keeping validation and derived labels here keeps composables simple: the UI only
 * asks questions like "can the user connect?" instead of repeating business rules.
 */
internal data class MainScreenState(
    val host: String = DEFAULT_ADB_HOST,
    val port: String = DEFAULT_ADB_PORT.toString(),
    val pairingHost: String = DEFAULT_ADB_HOST,
    val pairingPort: String = "",
    val pairingCode: String = "",
    val command: String = "getprop ro.product.model",
    val commandOutput: String = "",
    val logcatOutput: String = "",
    val screenshot: ByteArray? = null,
    val connectionStatus: String = "Not connected",
    val isConnected: Boolean = false,
    val inProgress: Boolean = false,
    val deviceSerial: String? = null,
    val deviceSerialList: List<String> = emptyList(),
    val snackbarMessage: String? = null,
) {
    val portNumber: Int? = port.toIntOrNull()
    val pairingPortNumber: Int? = pairingPort.toIntOrNull()
    val deviceAddress: String = "$host:$port"
    val pairingAddress: String = "$pairingHost:$pairingPort"
    val deviceLabel: String = deviceSerial?.let { "device $it" } ?: "no connected device"
    val canConnect: Boolean = !inProgress && host.isNotBlank() && portNumber != null
    val canDisconnect: Boolean = canConnect
    val canReconnect: Boolean = !inProgress && isConnected
    val canPair: Boolean = !inProgress &&
        pairingHost.isNotBlank() &&
        pairingPortNumber != null &&
        pairingCode.isNotBlank()
    val hasSelectedDevice: Boolean = deviceSerial != null
    val canRunDeviceAction: Boolean = hasSelectedDevice && !inProgress
    val canExecuteCommand: Boolean = canRunDeviceAction && command.isNotBlank()
    val deviceToolsSubtitle: String = if (isConnected) deviceLabel else "Connect to a device first"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MainScreenState) return false

        return host == other.host &&
            port == other.port &&
            pairingHost == other.pairingHost &&
            pairingPort == other.pairingPort &&
            pairingCode == other.pairingCode &&
            command == other.command &&
            commandOutput == other.commandOutput &&
            logcatOutput == other.logcatOutput &&
            screenshot.contentEquals(other.screenshot) &&
            connectionStatus == other.connectionStatus &&
            isConnected == other.isConnected &&
            inProgress == other.inProgress &&
            deviceSerial == other.deviceSerial &&
            deviceSerialList == other.deviceSerialList &&
            snackbarMessage == other.snackbarMessage
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port.hashCode()
        result = 31 * result + pairingHost.hashCode()
        result = 31 * result + pairingPort.hashCode()
        result = 31 * result + pairingCode.hashCode()
        result = 31 * result + command.hashCode()
        result = 31 * result + commandOutput.hashCode()
        result = 31 * result + logcatOutput.hashCode()
        result = 31 * result + (screenshot?.contentHashCode() ?: 0)
        result = 31 * result + connectionStatus.hashCode()
        result = 31 * result + isConnected.hashCode()
        result = 31 * result + inProgress.hashCode()
        result = 31 * result + (deviceSerial?.hashCode() ?: 0)
        result = 31 * result + deviceSerialList.hashCode()
        result = 31 * result + (snackbarMessage?.hashCode() ?: 0)
        return result
    }
}
