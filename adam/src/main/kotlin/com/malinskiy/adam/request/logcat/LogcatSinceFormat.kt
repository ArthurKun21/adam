/*
 * Copyright (C) 2022 Anton Malinskiy
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

package com.malinskiy.adam.request.logcat

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val sinceFormatter = LocalDateTime.Format {
    byUnicodePattern("MM-dd HH:mm:ss.SSS")
}

private val sinceYearFormatter = LocalDateTime.Format {
    byUnicodePattern("yyyy-MM-dd HH:mm:ss.SSS")
}

/**
 * Accepts the same zone ids as [TimeZone.of] (IANA ids, `UTC`, `Z`, `±hh:mm` offsets) plus the
 * `GMT[±hh[:mm]]` / `UTC[±hh[:mm]]` forms historically accepted by `java.util.TimeZone`
 */
private fun String.toTimeZone(): TimeZone = when {
    this == "GMT" || this == "UTC" -> TimeZone.UTC

    startsWith("GMT") || startsWith("UTC") -> {
        val offset = substring(3)
        offset.toIntOrNull()?.let { UtcOffset(hours = it).asTimeZone() } ?: TimeZone.of(offset)
    }

    else -> TimeZone.of(this)
}

public sealed class LogcatSinceFormat(public val text: String) {
    // It formats with 'MM-dd HH:mm:ss.SSS'
    public class DateString(instant: Instant, timezone: String) :
        LogcatSinceFormat("'${sinceFormatter.format(instant.toLocalDateTime(timezone.toTimeZone()))}'")

    // It formats with 'yyyy-MM-dd HH:mm:ss.SSS'
    public class DateStringYear(instant: Instant, timezone: String) :
        LogcatSinceFormat("'${sinceYearFormatter.format(instant.toLocalDateTime(timezone.toTimeZone()))}'")

    // It formats with 'SSS.0'
    public class TimeStamp(instant: Instant) :
        LogcatSinceFormat("${instant.toEpochMilliseconds()}.0")
}
