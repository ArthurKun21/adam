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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun MainScreen(
    viewModel: MainViewModel = MainViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.host,
                        onValueChange = viewModel::onAdbHostChanged,
                        label = { Text("ADB server host") },
                        enabled = !state.inProgress,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = "${state.port}",
                        onValueChange = viewModel::onAdbPortChanged,
                        label = { Text("ADB server port") },
                        enabled = !state.inProgress,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        enabled = !state.inProgress,
                        onClick = viewModel::connect,
                    ) {
                        Text("Connect")
                    }
                }

                Text(
                    text = state.connectionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    enabled = state.isConnected && !state.inProgress,
                    onClick = viewModel::takeScreenshot,
                ) {
                    Text("Take screenshot")
                }

                AsyncImage(
                    model = state.screenshot,
                    contentDescription = "ADB screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )

                OutlinedTextField(
                    value = state.command,
                    onValueChange = viewModel::onCommandChanged,
                    label = { Text("Shell command") },
                    enabled = state.isConnected && !state.inProgress,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    enabled = state.isConnected && state.command.isNotBlank() && !state.inProgress,
                    onClick = viewModel::executeCommand,
                ) {
                    Text("Execute command")
                }

                Text(
                    text = state.commandOutput.ifBlank { "Command output will appear here." },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
