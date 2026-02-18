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

package com.malinskiy.adam.request.testrunner

public sealed class TestEvent

public data class TestRunStartedEvent(public val testCount: Int) : TestEvent()
public data class TestStarted(public val id: TestIdentifier) : TestEvent()
public data class TestFailed(public val id: TestIdentifier, public val stackTrace: String) : TestEvent()
public data class TestAssumptionFailed(public val id: TestIdentifier, public val stackTrace: String) : TestEvent()
public data class TestIgnored(public val id: TestIdentifier) : TestEvent()
public data class TestEnded(public val id: TestIdentifier, public val metrics: Map<String, String>) : TestEvent()
public data class TestRunFailed(public val error: String) : TestEvent()
public data class TestRunFailing(public val error: String, public val stackTrace: String) : TestEvent()
public data class TestRunStopped(public val elapsedTimeMillis: Long) : TestEvent()
public data class TestRunEnded(
    public val elapsedTimeMillis: Long,
    public val metrics: Map<String, String>,
) : TestEvent()
public data class TestLogcat(public val id: TestIdentifier, public val log: String)
