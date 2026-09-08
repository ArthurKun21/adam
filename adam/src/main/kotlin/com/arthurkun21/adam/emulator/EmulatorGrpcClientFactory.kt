/*
 * Copyright (C) 2021 Anton Malinskiy
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

package com.arthurkun21.adam.emulator

import com.android.emulator.control.EmulatorController
import kotlinx.rpc.grpc.client.GrpcClient
import kotlinx.rpc.withService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a plaintext [GrpcClient] for the emulator's gRPC bridge.
 *
 * The bridge is exposed by the emulator on the console port + 3000, see
 * `docs/docs/emulator/emulator.md`.
 *
 * @param host host the emulator is reachable at
 * @param port the emulator's gRPC bridge port (console port + 3000)
 */
public fun emulatorGrpcClient(host: String, port: Int): GrpcClient {
    return GrpcClient(host, port) {
        credentials = plaintext()
    }
}

/**
 * Obtains a proxy of the emulator's [EmulatorController] service backed by this client.
 *
 * Server-streaming methods return a cold [kotlinx.coroutines.flow.Flow] and must be collected
 * within the caller's coroutine scope.
 */
public fun GrpcClient.emulatorController(): EmulatorController = withService()

/**
 * Initiates an orderly shutdown of this client and waits up to [timeout] for termination.
 *
 * Mirrors the `ManagedChannel.shutdown` + `awaitTermination` idiom; pending RPCs are allowed to
 * finish within the timeout, after which the call simply returns.
 */
public suspend fun GrpcClient.shutdownAndAwaitTermination(timeout: Duration = 5.seconds) {
    shutdown()
    awaitTermination(timeout)
}
