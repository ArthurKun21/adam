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

package com.malinskiy.adam.request.forwarding

import com.malinskiy.adam.exception.UnsupportedForwardingSpecException

public sealed class RemotePortSpec {
    public abstract fun toSpec(): String

    public companion object {
        public fun parse(value: String): RemotePortSpec {
            val split = value.split(':')
            val type = split[0]
            return when (type) {
                "tcp" -> RemoteTcpPortSpec(split[1].toInt())
                "localabstract" -> RemoteAbstractPortSpec(split[1])
                "localreserved" -> RemoteReservedPortSpec(split[1])
                "localfilesystem" -> RemoteFilesystemPortSpec(split[1])
                "dev" -> RemoteDevPortSpec(split[1])
                "jdwp" -> JDWPPortSpec(split[1].toInt())
                else -> throw UnsupportedForwardingSpecException(type)
            }
        }
    }
}

public data class RemoteTcpPortSpec(public val port: Int) : RemotePortSpec() {
    override fun toSpec(): String = "tcp:$port"
}

public data class RemoteAbstractPortSpec(public val unixDomainSocketName: String) : RemotePortSpec() {
    override fun toSpec(): String = "localabstract:$unixDomainSocketName"
}

public data class RemoteReservedPortSpec(public val unixDomainSocketName: String) : RemotePortSpec() {
    override fun toSpec(): String = "localreserved:$unixDomainSocketName"
}

public data class RemoteFilesystemPortSpec(public val unixDomainSocketName: String) : RemotePortSpec() {
    override fun toSpec(): String = "localfilesystem:$unixDomainSocketName"
}

public data class RemoteDevPortSpec(public val charDeviceName: String) : RemotePortSpec() {
    override fun toSpec(): String = "dev:$charDeviceName"
}

public data class JDWPPortSpec(public val processId: Int) : RemotePortSpec() {
    override fun toSpec(): String = "jdwp:$processId"
}
