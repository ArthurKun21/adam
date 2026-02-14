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
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import java.nio.ByteBuffer
import io.ktor.utils.io.readByte as channelReadByte
import io.ktor.utils.io.writeByte as channelWriteByte

public class StubSocket(
    public val readChannel: ByteReadChannel = ByteChannel(false),
    public val writeChannel: ByteWriteChannel = ByteChannel(false)
) : Socket {
    override val isClosedForWrite: Boolean
        get() = writeChannel.isClosedForWrite
    override val isClosedForRead: Boolean
        get() = readChannel.isClosedForRead

    public constructor(content: ByteArray) : this(readChannel = ByteReadChannel(content))

    override suspend fun readFully(buffer: ByteBuffer): Int {
        val count = buffer.remaining()
        val dst = readChannel.readByteArray(count)
        buffer.put(dst)
        return count
    }

    override suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int) {
        val tmp = readChannel.readByteArray(limit)
        System.arraycopy(tmp, 0, buffer, offset, limit)
    }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        val src = ByteArray(byteBuffer.remaining())
        byteBuffer.get(src)
        writeChannel.writeByteArray(src)
    }

    override suspend fun writeFully(byteArray: ByteArray, offset: Int, limit: Int) {
        writeChannel.writeByteArray(byteArray.copyOfRange(offset, offset + limit))
    }

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        if (readChannel.isClosedForRead) return -1
        val tmp = readChannel.readByteArray(limit.coerceAtMost(1))
        System.arraycopy(tmp, 0, buffer, offset, tmp.size)
        return tmp.size
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
