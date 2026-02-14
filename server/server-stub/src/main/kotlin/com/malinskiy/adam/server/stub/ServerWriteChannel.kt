/*
 * Copyright (C) 2020 Anton Malinskiy
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

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.shell.v2.MessageType
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeStringUtf8
import java.nio.ByteBuffer
import io.ktor.utils.io.writeInt as channelWriteInt
import io.ktor.utils.io.writeLong as channelWriteLong
import io.ktor.utils.io.writeByte as channelWriteByte

public class ServerWriteChannel(private val delegate: ByteWriteChannel) : ByteWriteChannel by delegate {
    private suspend fun write(request: ByteArray, length: Int? = null) {
        delegate.writeByteArray(request.copyOfRange(0, length ?: request.size))
    }

    public suspend fun writeIntLittleEndian(value: Int) {
        delegate.channelWriteInt(Integer.reverseBytes(value))
    }

    private suspend fun writeLongLittleEndian(value: Long) {
        delegate.channelWriteLong(java.lang.Long.reverseBytes(value))
    }

    public suspend fun writeFully(src: ByteArray) {
        delegate.writeByteArray(src)
    }

    public suspend fun writeFully(src: ByteArray, offset: Int, length: Int) {
        delegate.writeByteArray(src.copyOfRange(offset, offset + length))
    }

    public suspend fun writeByte(value: Byte) {
        delegate.channelWriteByte(value)
    }

    public suspend fun respond(request: ByteArray, length: Int? = null) {
        write(request, length)
    }

    public suspend fun respondOkay() {
        respond(Const.Message.OKAY)
    }

    public suspend fun respondStat(size: Int, mode: Int = 0, lastModified: Int = 0) {
        respond(Const.Message.LSTAT_V1)
        writeIntLittleEndian(mode)
        writeIntLittleEndian(size)
        writeIntLittleEndian(lastModified)
    }

    public suspend fun respondStatV2(
        mode: Int,
        size: Int,
        error: Int,
        dev: Int,
        ino: Int,
        nlink: Int,
        uid: Int,
        gid: Int,
        atime: Int,
        mtime: Int,
        ctime: Int
    ) {
        respond(Const.Message.LSTAT_V2)
        writeIntLittleEndian(error)
        writeLongLittleEndian(dev.toLong())
        writeLongLittleEndian(ino.toLong())
        writeIntLittleEndian(mode)
        writeIntLittleEndian(nlink)
        writeIntLittleEndian(uid)
        writeIntLittleEndian(gid)
        writeLongLittleEndian(size.toLong())
        writeLongLittleEndian(atime.toLong())
        writeLongLittleEndian(mtime.toLong())
        writeLongLittleEndian(ctime.toLong())
    }

    public suspend fun respondData(byteArray: ByteArray) {
        respond(Const.Message.DATA)
        writeIntLittleEndian(byteArray.size)
        writeFully(byteArray, 0, byteArray.size)
    }

    public suspend fun respondDone() {
        respond(Const.Message.DONE)
    }

    public suspend fun respondDoneDone() {
        respond(Const.Message.DONEDONE)
    }

    public suspend fun respondFailFail() {
        respond(Const.Message.FAILFAIL)
    }

    public suspend fun respondList(size: Int, mode: Int = 0, lastModified: Int = 0, name: String) {
        respond(Const.Message.DENT_V1)
        writeIntLittleEndian(mode)
        writeIntLittleEndian(size)
        writeIntLittleEndian(lastModified)
        writeIntLittleEndian(name.length)
        writeStringUtf8(name)
    }

    public suspend fun respondListV2(
        name: String,
        mode: Int = 0,
        size: Int,
        error: Int,
        dev: Int,
        ino: Int,
        nlink: Int,
        uid: Int,
        gid: Int,
        atime: Int,
        mtime: Int,
        ctime: Int
    ) {
        respond(Const.Message.DENT_V2)
        writeIntLittleEndian(error)
        writeLongLittleEndian(dev.toLong())
        writeLongLittleEndian(ino.toLong())
        writeIntLittleEndian(mode)
        writeIntLittleEndian(nlink)
        writeIntLittleEndian(uid)
        writeIntLittleEndian(gid)
        writeLongLittleEndian(size.toLong())
        writeLongLittleEndian(atime.toLong())
        writeLongLittleEndian(mtime.toLong())
        writeLongLittleEndian(ctime.toLong())
        writeIntLittleEndian(name.length)
        writeStringUtf8(name)
    }

    public suspend fun respondStringV1(message: String) {
        val lengthString = message.length.toString(16)
        val prepend = 4 - lengthString.length
        assert(prepend >= 0)
        var size = ""
        for (i in 0 until prepend) {
            size += "0"
        }
        size += lengthString
        writeFully(size.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
        writeFully(message.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
    }

    public suspend fun respondStringV2(message: String) {
        val bytes = message.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        writeIntLittleEndian(bytes.size)
        writeFully(bytes)
    }

    public suspend fun respondStringRaw(message: String) {
        respond(message.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
    }

    public suspend fun respondFail(message: String) {
        respond(Const.Message.FAIL)
        writeIntLittleEndian(message.length)
        respondStringRaw(message)
    }

    public suspend fun respondShellV1(stdout: String) {
        respondStringRaw(stdout)
    }

    public suspend fun respondShellV2(stdout: String, stderr: String, exitCode: Int) {
        respondShellV2Stdout(stdout)
        respondShellV2Stderr(stderr)
        respondShellV2Exit(exitCode)
    }

    public suspend fun respondShellV2Exit(exitCode: Int) {
        delegate.channelWriteByte(MessageType.EXIT.toValue().toByte())
        delegate.channelWriteInt(1)
        delegate.channelWriteByte(exitCode.toByte())
    }

    public suspend fun respondShellV2Stderr(stderr: String) {
        delegate.channelWriteByte(MessageType.STDERR.toValue().toByte())
        respondStringV2(stderr)
    }

    public suspend fun respondShellV2Stdout(stdout: String) {
        delegate.channelWriteByte(MessageType.STDOUT.toValue().toByte())
        respondStringV2(stdout)
    }

    public suspend fun respondShellV2WindowSizeChange() {
        delegate.channelWriteByte(MessageType.WINDOW_SIZE_CHANGE.toValue().toByte())
    }

    public suspend fun respondShellV2Invalid() {
        delegate.channelWriteByte(MessageType.INVALID.toValue().toByte())
    }
}
