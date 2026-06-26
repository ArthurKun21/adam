package com.arthurkun21.adam.samples.desktop.ui

internal const val DEFAULT_ADB_HOST = "127.0.0.1"
internal const val DEFAULT_ADB_PORT = 5555

internal data class MainScreenState(
    val host: String = DEFAULT_ADB_HOST,
    val port: String = DEFAULT_ADB_PORT.toString(),
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
    val deviceLabel: String = deviceSerial?.let { "device $it" } ?: "no connected device"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MainScreenState) return false

        return host == other.host &&
            port == other.port &&
            command == other.command &&
            commandOutput == other.commandOutput &&
            logcatOutput == other.logcatOutput &&
            screenshot.contentEquals(other.screenshot) &&
            connectionStatus == other.connectionStatus &&
            isConnected == other.isConnected &&
            inProgress == other.inProgress &&
            deviceSerial == other.deviceSerial &&
            snackbarMessage == other.snackbarMessage
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port.hashCode()
        result = 31 * result + command.hashCode()
        result = 31 * result + commandOutput.hashCode()
        result = 31 * result + logcatOutput.hashCode()
        result = 31 * result + (screenshot?.contentHashCode() ?: 0)
        result = 31 * result + connectionStatus.hashCode()
        result = 31 * result + isConnected.hashCode()
        result = 31 * result + inProgress.hashCode()
        result = 31 * result + (deviceSerial?.hashCode() ?: 0)
        result = 31 * result + (snackbarMessage?.hashCode() ?: 0)
        return result
    }
}
