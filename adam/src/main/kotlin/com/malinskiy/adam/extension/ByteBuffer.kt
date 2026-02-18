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

package com.malinskiy.adam.extension

import java.nio.Buffer
import java.nio.ByteBuffer

/**
 * Mitigation of running JDK 9 code on JRE 8
 *
 * java.lang.NoSuchMethodError: java.nio.ByteBuffer.xxx()Ljava/nio/ByteBuffer;
 */
public fun ByteBuffer.compatRewind(): ByteBuffer = ((this as Buffer).rewind() as ByteBuffer)
public fun ByteBuffer.compatLimit(newLimit: Int): ByteBuffer = ((this as Buffer).limit(newLimit) as ByteBuffer)
public fun ByteBuffer.compatPosition(newLimit: Int): ByteBuffer = ((this as Buffer).position(newLimit) as ByteBuffer)
public fun ByteBuffer.compatFlip(): ByteBuffer = ((this as Buffer).flip() as ByteBuffer)
public fun ByteBuffer.compatClear(): ByteBuffer = ((this as Buffer).clear() as ByteBuffer)
