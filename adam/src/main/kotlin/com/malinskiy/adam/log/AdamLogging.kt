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

package com.malinskiy.adam.log

import logcat.LogPriority
import logcat.logcat
import logcat.asLog

public object AdamLogging {
    public fun logger(func: () -> Unit): AdamLogger = AdamLogger()
}

public class AdamLogger {
    public inline fun debug(throwable: Throwable? = null, crossinline message: () -> String) {
        logcat(LogPriority.DEBUG) {
            buildString {
                throwable?.let {
                    append(it.asLog())
                    append("\n")
                }
                append(message())
            }
        }
    }

    public inline fun info(throwable: Throwable? = null, crossinline message: () -> String) {
        logcat(LogPriority.INFO) {
            buildString {
                throwable?.let {
                    append(it.asLog())
                    append("\n")
                }
                append(message())
            }
        }
    }

    public inline fun warn(throwable: Throwable? = null, crossinline message: () -> String) {
        logcat(LogPriority.WARN) {
            buildString {
                throwable?.let {
                    append(it.asLog())
                    append("\n")
                }
                append(message())
            }
        }
    }

    public inline fun error(throwable: Throwable? = null, crossinline message: () -> String) {
        logcat(LogPriority.ERROR) {
            buildString {
                throwable?.let {
                    append(it.asLog())
                    append("\n")
                }
                append(message())
            }
        }
    }

    public inline fun trace(throwable: Throwable? = null, crossinline message: () -> String) {
        logcat(LogPriority.VERBOSE) {
            buildString {
                throwable?.let {
                    append(it.asLog())
                    append("\n")
                }
                append(message())
            }
        }
    }
}