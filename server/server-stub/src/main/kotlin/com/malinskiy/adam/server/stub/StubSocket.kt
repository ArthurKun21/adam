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

package com.malinskiy.adam.server.stub

import com.malinskiy.adam.transport.Socket
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import java.nio.ByteBuffer
import io.ktor.utils.io.readByte as channelReadByte
import io.ktor.utils.io.writeByte as channelWriteByte

public class StubSocket(
    public val readChannel: ByteReadChannel = ByteChannel(false),
    public val writeChannel: ByteWriteChannel = ByteChannel(false),
) : Socket {
    override val isClosedForWrite: Boolean
        get() = writeChannel.isClosedForWrite
    override val isClosedForRead: Boolean
        get() = readChannel.isClosedForRead

    public constructor(content: ByteArray) : this(readChannel = ByteReadChannel(content))

    override suspend fun readFully(buffer: ByteBuffer): Int {
        val count = buffer.remaining()
        readChannel.readFully(buffer)
        return count
    }

    override suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int) {
        readChannel.readFully(buffer, offset, offset + limit)
    }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        writeChannel.writeFully(byteBuffer)
    }

    override suspend fun writeFully(byteArray: ByteArray, offset: Int, limit: Int) {
        writeChannel.writeFully(byteArray, offset, offset + limit)
    }

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        return readChannel.readAvailable(buffer, offset, limit)
    }

    override suspend fun readByte(): Byte = readChannel.channelReadByte()
    override suspend fun readIntLittleEndian(): Int = Integer.reverseBytes(readChannel.readInt())
    override suspend fun writeByte(value: Int): Unit = writeChannel.channelWriteByte(value.toByte())
    override suspend fun writeIntLittleEndian(value: Int): Unit = writeChannel.writeInt(Integer.reverseBytes(value))

    override suspend fun close() {
        try {
            writeChannel.flushAndClose()
            readChannel.cancel()
        } catch (e: Exception) {
            println("Exception during cleanup. Ignoring")
            e.printStackTrace()
        }
    }
}
