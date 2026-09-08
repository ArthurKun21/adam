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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.android.emulator.control.VmRunState
import com.android.emulator.control.VmRunStateInternal
import com.android.emulator.control.invoke
import kotlinx.rpc.internal.utils.ExperimentalRpcApi
import kotlinx.rpc.internal.utils.InternalRpcApi
import org.junit.Test

@OptIn(ExperimentalRpcApi::class, InternalRpcApi::class)
class GeneratedMessageMarshallerTest {
    /**
     * Exercises the generated message marshaller decode path outside of a live gRPC call.
     *
     * The kotlinx-rpc `checkForPlatformDecodeException` helper is inlined into every generated
     * `*Internal$MARSHALLER.decode` and catches `com.google.protobuf.InvalidProtocolBufferException`,
     * so verifying that method requires protobuf-java on the runtime classpath (declared as a
     * runtime dependency of kotlinx-rpc-protobuf-lite). Dropping that dependency only surfaced as
     * NoClassDefFoundError on a live emulator call; this test fails fast without one.
     */
    @Test
    fun testGeneratedMarshallerRoundTrip() {
        val message = VmRunState { state = VmRunState.RunState.RUNNING }

        val encoded = VmRunStateInternal.MARSHALLER.encode(message, null)
        val decoded = VmRunStateInternal.MARSHALLER.decode(encoded, null)

        assertThat(decoded.state).isEqualTo(VmRunState.RunState.RUNNING)
    }
}
