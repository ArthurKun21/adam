/*
 * Copyright (C) 2026 Arthurkun21
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arthurkun21.adam.samples.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun MainScreen(
    viewModel: MainViewModel? = null,
) {
    val actualViewModel = viewModel ?: remember { MainViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }
    val state by actualViewModel.uiState.collectAsState()

    DisposableEffect(actualViewModel) {
        onDispose {
            actualViewModel.close()
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            actualViewModel.onSnackbarShown()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ConnectionHeader(state)
                ConnectionCard(
                    state = state,
                    onHostChanged = actualViewModel::onAdbHostChanged,
                    onPortChanged = actualViewModel::onAdbPortChanged,
                    onConnect = actualViewModel::connect,
                )
                DeviceActionsCard(
                    state = state,
                    onTakeScreenshot = actualViewModel::takeScreenshot,
                    onCommandChanged = actualViewModel::onCommandChanged,
                    onExecuteCommand = actualViewModel::executeCommand,
                )
            }
        }
    }
}

@Composable
private fun ConnectionHeader(state: MainScreenState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectionIndicator(connected = state.isConnected)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Adam ADB connection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.connectionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = state.deviceLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    state: MainScreenState,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onConnect: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "ADB server",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.host,
                    onValueChange = onHostChanged,
                    label = { Text("Host") },
                    supportingText = { Text("Usually 127.0.0.1 for local adb") },
                    enabled = !state.inProgress,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.port,
                    onValueChange = onPortChanged,
                    label = { Text("Port") },
                    isError = state.portNumber == null,
                    supportingText = { Text("ADB server port") },
                    enabled = !state.inProgress,
                    singleLine = true,
                    modifier = Modifier.width(160.dp),
                )
                Button(
                    enabled = !state.inProgress && state.host.isNotBlank() && state.portNumber != null,
                    onClick = onConnect,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(if (state.isConnected) "Reconnect" else "Connect")
                }
            }
            if (state.inProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DeviceActionsCard(
    state: MainScreenState,
    onTakeScreenshot: () -> Unit,
    onCommandChanged: (String) -> Unit,
    onExecuteCommand: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Connected device tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (state.isConnected) state.deviceLabel else "Connect to an ADB server first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    enabled = state.deviceSerial != null && !state.inProgress,
                    onClick = onTakeScreenshot,
                ) {
                    Text("Take screenshot")
                }
            }

            ScreenshotPreview(state.screenshot)

            HorizontalDivider()

            OutlinedTextField(
                value = state.command,
                onValueChange = onCommandChanged,
                label = { Text("Shell command") },
                enabled = state.deviceSerial != null && !state.inProgress,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                enabled = state.deviceSerial != null && state.command.isNotBlank() && !state.inProgress,
                onClick = onExecuteCommand,
            ) {
                Text("Execute command")
            }

            TerminalOutput(state.commandOutput)
        }
    }
}

@Composable
private fun ScreenshotPreview(screenshot: ByteArray?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (screenshot == null) {
            Text(
                text = "Screenshot preview will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = screenshot,
                contentDescription = "ADB screenshot",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TerminalOutput(output: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = output.ifBlank { "Command output will appear here." },
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun ConnectionIndicator(connected: Boolean) {
    val color = if (connected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(color, CircleShape),
    )
}
