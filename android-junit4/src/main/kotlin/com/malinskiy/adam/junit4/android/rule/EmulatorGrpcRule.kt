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

package com.malinskiy.adam.junit4.rule

import androidx.test.platform.app.InstrumentationRegistry
import com.android.emulator.control.EmulatorController
import com.arthurkun21.adam.emulator.emulatorController
import com.arthurkun21.adam.emulator.emulatorGrpcClient
import com.arthurkun21.adam.emulator.shutdownAndAwaitTermination
import com.malinskiy.adam.android.contract.TestRunnerContract
import com.malinskiy.adam.junit4.android.rule.Mode
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.grpc.client.GrpcClient
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

public class EmulatorGrpcRule(
    public val mode: Mode = Mode.ASSERT,
) : TestRule {
    public lateinit var grpc: EmulatorController
    private var client: GrpcClient? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val arguments = InstrumentationRegistry.getArguments()
                val grpcPort = arguments.getString(TestRunnerContract.grpcPortArgumentName)?.toIntOrNull()
                val grpcHost = arguments.getString(TestRunnerContract.grpcHostArgumentName)

                if (grpcPort != null && grpcHost != null) {
                    val localClient = emulatorGrpcClient(grpcHost, grpcPort)
                    client = localClient
                    grpc = localClient.emulatorController()
                } else {
                    when (mode) {
                        Mode.SKIP -> return

                        Mode.ASSUME -> throw AssumptionViolatedException(
                            "No access to emulator's gRPC port has been provided: host = $grpcHost, port = $grpcPort",
                        )

                        Mode.ASSERT -> throw AssertionError(
                            "No access to emulator's gRPC port has been provided: host = $grpcHost, port = $grpcPort",
                        )
                    }
                }

                try {
                    base.evaluate()
                } finally {
                    runBlocking {
                        client?.shutdownAndAwaitTermination()
                    }
                }
            }
        }
    }
}
