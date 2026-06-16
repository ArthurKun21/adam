package com.arthurkun21.adam.samples.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arthurkun21.adam.samples.desktop.ui.MainScreen

fun main() {
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

