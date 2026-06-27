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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
internal fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.close()
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    MainScreenContent(
        state = state,
        actions = MainScreenActions(
            onHostChanged = viewModel::onAdbHostChanged,
            onPortChanged = viewModel::onAdbPortChanged,
            onConnect = viewModel::connect,
            onDeviceSelect = viewModel::updateSerial,
            onTakeScreenshot = viewModel::takeScreenshot,
            onFetchLogcat = viewModel::fetchLogcat,
            onCommandChanged = viewModel::onCommandChanged,
            onExecuteCommand = viewModel::executeCommand,
        ),
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun MainScreenContent(
    state: MainScreenState,
    actions: MainScreenActions,
    snackbarHostState: SnackbarHostState,
) {
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
                    onHostChanged = actions.onHostChanged,
                    onPortChanged = actions.onPortChanged,
                    onConnect = actions.onConnect,
                )
                DeviceActionsCard(
                    state = state,
                    onDeviceSelect = actions.onDeviceSelect,
                    onTakeScreenshot = actions.onTakeScreenshot,
                    onFetchLogcat = actions.onFetchLogcat,
                    onCommandChanged = actions.onCommandChanged,
                    onExecuteCommand = actions.onExecuteCommand,
                )
            }
        }
    }
}

private data class MainScreenActions(
    val onHostChanged: (String) -> Unit,
    val onPortChanged: (String) -> Unit,
    val onConnect: () -> Unit,
    val onDeviceSelect: (String) -> Unit,
    val onTakeScreenshot: () -> Unit,
    val onFetchLogcat: () -> Unit,
    val onCommandChanged: (String) -> Unit,
    val onExecuteCommand: () -> Unit,
)

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
            verticalArrangement = Arrangement.spacedBy(
                12.dp,
                Alignment.CenterVertically,
            ),
        ) {
            Text(
                text = "ADB over Wi-Fi device",
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
                    label = { Text("Device host") },
                    supportingText = { Text("Use 127.0.0.1 for a local emulator") },
                    enabled = !state.inProgress,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.port,
                    onValueChange = onPortChanged,
                    label = { Text("Device port") },
                    isError = state.portNumber == null,
                    supportingText = { Text("$DEFAULT_ADB_PORT is the emulator TCP default") },
                    enabled = !state.inProgress,
                    singleLine = true,
                    modifier = Modifier.width(160.dp),
                )
                Button(
                    enabled = state.canConnect,
                    onClick = onConnect,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(state.connectButtonLabel)
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
    onDeviceSelect: (String) -> Unit,
    onTakeScreenshot: () -> Unit,
    onFetchLogcat: () -> Unit,
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
                        text = state.deviceToolsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    OutlinedButton(
                        enabled = state.canRunDeviceAction,
                        onClick = onTakeScreenshot,
                    ) {
                        Text("Take screenshot")
                    }
                    OutlinedButton(
                        enabled = state.canRunDeviceAction,
                        onClick = onFetchLogcat,
                    ) {
                        Text("Fetch logcat")
                    }
                }
            }
            DeviceList(
                devices = state.deviceSerialList,
                onDeviceSelect = onDeviceSelect,
            )

            ScreenshotPreview(state.screenshot)

            HorizontalDivider()

            OutlinedTextField(
                value = state.command,
                onValueChange = onCommandChanged,
                label = { Text("Shell command") },
                enabled = state.canRunDeviceAction,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                enabled = state.canExecuteCommand,
                onClick = onExecuteCommand,
            ) {
                Text("Execute command")
            }

            TerminalOutput(state.commandOutput)

            HorizontalDivider()

            Text(
                text = "Recent device logcat",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            TerminalOutput(
                output = state.logcatOutput,
                placeholder = "Recent logcat output will appear here.",
            )
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<String>,
    onDeviceSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        devices.forEach { device ->
            OutlinedButton(
                onClick = { onDeviceSelect(device) },
            ) {
                Text(device)
            }
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
private fun TerminalOutput(
    output: String,
    placeholder: String = "Command output will appear here.",
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = output.ifBlank { placeholder },
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
