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

package com.arthurkun21.adam.instrumentation

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/**
 * Kotlin model of the `am instrument` protobuf output, mirroring the proto2 schema of
 * `instrumentation-data.proto` (kept as a wire-format reference under `docs/reference/`).
 *
 * The schema originates from AOSP
 * `frameworks/base/cmds/am/src/com/android/commands/am/InstrumentData.proto` (proto2).
 *
 * proto2 `optional` presence is modeled with nullable properties (`null` = field absent on the
 * wire); `sint32`/`sint64` use zigzag encoding ([ProtoIntegerType.SIGNED]). Scalar fields whose
 * consumers rely on the proto2 default (absent = 0) are modeled as non-null with that default.
 * The `status_code` enum is decoded as a plain [Int] so that unknown codes never break parsing,
 * see [SessionStatusCode].
 */
@Serializable
public data class Session(
    @ProtoNumber(1) public val testStatus: List<TestStatus> = emptyList(),
    @ProtoNumber(2) public val sessionStatus: SessionStatus? = null,
)

@Serializable
public data class SessionStatus(
    @ProtoNumber(1) public val statusCode: Int = SessionStatusCode.SESSION_FINISHED,
    @ProtoNumber(2) public val errorText: String? = null,
    @ProtoNumber(3) @ProtoType(ProtoIntegerType.SIGNED) public val resultCode: Int = 0,
    @ProtoNumber(4) public val results: ResultsBundle? = null,
)

@Serializable
public data class TestStatus(
    @ProtoNumber(3) @ProtoType(ProtoIntegerType.SIGNED) public val resultCode: Int = 0,
    @ProtoNumber(4) public val results: ResultsBundle? = null,
    @ProtoNumber(5) public val logcat: String? = null,
)

@Serializable
public data class ResultsBundle(
    @ProtoNumber(1) public val entries: List<ResultsBundleEntry> = emptyList(),
)

@Serializable
public data class ResultsBundleEntry(
    @ProtoNumber(1) public val key: String? = null,
    @ProtoNumber(2) public val valueString: String? = null,
    @ProtoNumber(3) @ProtoType(ProtoIntegerType.SIGNED) public val valueInt: Int? = null,
    @ProtoNumber(4) public val valueFloat: Float? = null,
    @ProtoNumber(5) public val valueDouble: Double? = null,
    @ProtoNumber(6) @ProtoType(ProtoIntegerType.SIGNED) public val valueLong: Long? = null,
    @ProtoNumber(7) public val valueBundle: ResultsBundle? = null,
    @ProtoNumber(8) public val valueBytes: ByteArray? = null,
)

/**
 * Values of the proto `SessionStatusCode` enum. Modeled as constants instead of a Kotlin enum so
 * that decoders never fail on codes added by newer platforms (enum values serialize by ordinal).
 */
public object SessionStatusCode {
    /**
     * The command ran successfully. This does not imply that the tests passed.
     */
    public const val SESSION_FINISHED: Int = 0

    /**
     * There was an unrecoverable error running the tests.
     */
    public const val SESSION_ABORTED: Int = 1
}
