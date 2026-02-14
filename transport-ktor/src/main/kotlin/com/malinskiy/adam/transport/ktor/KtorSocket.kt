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

package com.malinskiy.adam.transport.ktor

import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.transport.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import java.nio.ByteBuffer
import io.ktor.network.sockets.Socket as RealKtorSocket
import io.ktor.utils.io.readByte as ktorReadByte
import io.ktor.utils.io.writeByte as ktorWriteByte

public class KtorSocket(private val ktorSocket: RealKtorSocket) : Socket {
    private val readChannel: ByteReadChannel = ktorSocket.openReadChannel()
    private val writeChannel: ByteWriteChannel = ktorSocket.openWriteChannel(autoFlush = true)
    override val isClosedForWrite: Boolean
        get() = writeChannel.isClosedForWrite
    override val isClosedForRead: Boolean
        get() = readChannel.isClosedForRead

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
        writeChannel.flush()
    }

    override suspend fun writeFully(byteArray: ByteArray, offset: Int, limit: Int) {
        writeChannel.writeByteArray(byteArray.copyOfRange(offset, offset + limit))
        writeChannel.flush()
    }

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        if (readChannel.isClosedForRead && readChannel.availableForRead == 0) return -1
        val avail = readChannel.availableForRead
        if (avail == 0) return 0
        val toRead = avail.coerceAtMost(limit)
        val tmp = readChannel.readByteArray(toRead)
        System.arraycopy(tmp, 0, buffer, offset, toRead)
        return toRead
    }

    override suspend fun readByte(): Byte = readChannel.ktorReadByte()

    override suspend fun readIntLittleEndian(): Int = Integer.reverseBytes(readChannel.readInt())

    override suspend fun writeByte(value: Int) {
        writeChannel.ktorWriteByte(value.toByte())
        writeChannel.flush()
    }

    override suspend fun writeIntLittleEndian(value: Int) {
        writeChannel.writeInt(Integer.reverseBytes(value))
        writeChannel.flush()
    }

    override suspend fun close() {
        try {
            writeChannel.flushAndClose()
            readChannel.cancel()
            ktorSocket.close()
        } catch (e: Exception) {
            log.debug(e) { "Exception during cleanup. Ignoring" }
        }
    }

    private companion object {
        private val log = AdamLogging.logger {}
    }
}
