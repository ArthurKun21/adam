package com.arthurkun21.adam.samples.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arthurkun21.adam.samples.desktop.ui.MainScreen
import logcat.LogcatLogger
import logcat.PrintLogger

fun main() {
    if (!LogcatLogger.isInstalled) {
        LogcatLogger.install()
        LogcatLogger.loggers += PrintLogger
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Adam Desktop Sample",
        ) {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}
