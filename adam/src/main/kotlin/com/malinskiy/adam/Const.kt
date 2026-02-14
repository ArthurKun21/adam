/*
 * Copyright (C) 2019 Anton Malinskiy
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

package com.malinskiy.adam

public object Const {
    public const val MAX_REMOTE_PATH_LENGTH: Int = 1024
    public const val DEFAULT_BUFFER_SIZE: Int = 1024
    public val DEFAULT_TRANSPORT_ENCODING: java.nio.charset.Charset = Charsets.UTF_8
    public const val DEFAULT_ADB_HOST: String = "127.0.0.1"
    public const val DEFAULT_ADB_PORT: Int = 5037

    public const val SERVER_PORT_ENV_VAR: String = "ANDROID_ADB_SERVER_PORT"
    public const val MAX_PACKET_LENGTH: Int = 16384
    public const val MAX_FILE_PACKET_LENGTH: Int = 64 * 1024

    public const val MAX_PROTOBUF_LOGCAT_LENGTH: Int = 10_000
    public const val MAX_PROTOBUF_PACKET_LENGTH: Long = 10 * 1024 * 1024L //10Mb
    public const val TEST_LOGCAT_METRIC: String = "com.malinskiy.adam.logcat"

    public const val ANDROID_FILE_SEPARATOR: String = "/"
    public val SYNC_IGNORED_FILES: Set<String> = setOf(".", "..")

    public object Message {
        public val OKAY: ByteArray = byteArrayOf('O'.code.toByte(), 'K'.code.toByte(), 'A'.code.toByte(), 'Y'.code.toByte())
        public val FAIL: ByteArray = byteArrayOf('F'.code.toByte(), 'A'.code.toByte(), 'I'.code.toByte(), 'L'.code.toByte())

        public val DATA: ByteArray = byteArrayOf('D'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte())
        public val DONE: ByteArray = byteArrayOf('D'.code.toByte(), 'O'.code.toByte(), 'N'.code.toByte(), 'E'.code.toByte())

        public val LSTAT_V1: ByteArray = byteArrayOf('S'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte())
        public val LIST_V1: ByteArray = byteArrayOf('L'.code.toByte(), 'I'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte())
        public val DENT_V1: ByteArray = byteArrayOf('D'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'T'.code.toByte())
        public val SEND_V1: ByteArray = byteArrayOf('S'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte())
        public val RECV_V1: ByteArray = byteArrayOf('R'.code.toByte(), 'E'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte())

        public val LIST_V2: ByteArray = byteArrayOf('L'.code.toByte(), 'I'.code.toByte(), 'S'.code.toByte(), '2'.code.toByte())
        public val DENT_V2: ByteArray = byteArrayOf('D'.code.toByte(), 'N'.code.toByte(), 'T'.code.toByte(), '2'.code.toByte())
        public val LSTAT_V2: ByteArray = byteArrayOf('L'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), '2'.code.toByte())
        public val RECV_V2: ByteArray = byteArrayOf('R'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte(), '2'.code.toByte())
        public val SEND_V2: ByteArray = byteArrayOf('S'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte(), '2'.code.toByte())

        public val DONEDONE: ByteArray =
            byteArrayOf(
                'D'.code.toByte(),
                'O'.code.toByte(),
                'N'.code.toByte(),
                'E'.code.toByte(),
                'D'.code.toByte(),
                'O'.code.toByte(),
                'N'.code.toByte(),
                'E'.code.toByte()
            )
        public val FAILFAIL: ByteArray =
            byteArrayOf(
                'F'.code.toByte(),
                'A'.code.toByte(),
                'I'.code.toByte(),
                'L'.code.toByte(),
                'F'.code.toByte(),
                'A'.code.toByte(),
                'I'.code.toByte(),
                'L'.code.toByte()
            )
    }

    public object FileType {
        public val S_IFMT: UInt = "170000".toUInt(8)
        public val S_IFIFO: UInt = "10000".toUInt(8)
        public val S_IFCHR: UInt = "20000".toUInt(8)
        public val S_IFDIR: UInt = "40000".toUInt(8)
        public val S_IFBLK: UInt = "60000".toUInt(8)
        public val S_IFREG: UInt = "100000".toUInt(8)
        public val S_IFLNK: UInt = "120000".toUInt(8)
        public val S_IFSOCK: UInt = "140000".toUInt(8)
    }
}
