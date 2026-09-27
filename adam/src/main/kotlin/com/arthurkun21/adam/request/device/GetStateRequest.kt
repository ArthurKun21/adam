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

package com.arthurkun21.adam.request.device

import com.malinskiy.adam.extension.readProtocolString
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.SerialTarget
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.transport.Socket

/**
 * Returns the state of a specific device via the `host-serial:<serial>:get-state` service.
 *
 * Useful as an online probe: the request is rejected by the adb server when the serial is unknown
 *
 * Note: execute with `serial = null` since the serial is part of the request itself
 */
public class GetStateRequest(serial: String) : ComplexRequest<DeviceState>(target = SerialTarget(serial)) {
    override fun serialize(): ByteArray = createBaseRequest("get-state")

    override suspend fun readElement(socket: Socket): DeviceState =
        DeviceState.from(socket.readProtocolString().trim())
}
