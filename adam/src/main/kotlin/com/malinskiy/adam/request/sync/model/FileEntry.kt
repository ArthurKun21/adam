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

package com.malinskiy.adam.request.sync.model

import com.malinskiy.adam.Const
import java.time.Instant

public sealed class FileEntry {
    public abstract val mode: UInt
    public abstract val name: String?
    public abstract val mtime: Instant

    public fun isDirectory(): Boolean = (mode and Const.FileType.S_IFDIR) == Const.FileType.S_IFDIR
    public fun isRegularFile(): Boolean = (mode and Const.FileType.S_IFREG) == Const.FileType.S_IFREG
    public fun isBlockDevice(): Boolean = (mode and Const.FileType.S_IFBLK) == Const.FileType.S_IFBLK
    public fun isCharDevice(): Boolean = (mode and Const.FileType.S_IFCHR) == Const.FileType.S_IFCHR
    public fun isLink(): Boolean = (mode and Const.FileType.S_IFLNK) == Const.FileType.S_IFLNK

    public fun size(): ULong = when (this) {
        is FileEntryV1 -> size.toLong().toULong()
        is FileEntryV2 -> size
    }

    public abstract fun exists(): Boolean
}

public data class FileEntryV1(
    override val name: String? = null,
    override val mode: UInt,
    public val size: UInt,
    override val mtime: Instant,
) : FileEntry() {
    override fun exists(): Boolean = !(size == 0.toUInt() && mode == 0.toUInt() && mtime.epochSecond == 0L)
}

public data class FileEntryV2(
    public val error: UInt,
    public val dev: ULong,
    public val ino: ULong,
    override val mode: UInt,
    public val nlink: UInt,
    public val uid: UInt,
    public val gid: UInt,
    public val size: ULong,
    public val atime: Instant,
    override val mtime: Instant,
    public val ctime: Instant,
    override val name: String? = null,
) : FileEntry() {
    override fun exists(): Boolean = !(size == 0.toULong() && mode == 0.toUInt() && mtime.epochSecond == 0L)
}
