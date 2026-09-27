/*
 * Copyright 2026 Google LLC
 * Portions Copyright (C) 2026 ArthurKun21
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

// Ported to Kotlin from google/artemis packages/artemis-accessibility-helper (Apache 2.0)

package com.arthurkun21.adam.companion

/**
 * High-performance XML serialization and character sanitization utilities.
 * Ensures the output strictly conforms to the W3C XML 1.0 specification,
 * preventing XML parsing errors caused by unprintable control characters,
 * NUL bytes, or unescaped entities.
 */
object XmlUtils {

    /**
     * Appends an escaped attribute to the StringBuilder: `name="value"`.
     * If the value is null or empty, appends `name=""`.
     */
    fun appendAttribute(sb: StringBuilder, name: String, value: CharSequence?) {
        sb.append(' ').append(name).append("=\"")
        if (!value.isNullOrEmpty()) {
            escapeXmlAttr(sb, value)
        }
        sb.append('"')
    }

    /** Appends a boolean attribute: `name="true"` or `name="false"`. */
    fun appendBooleanAttribute(sb: StringBuilder, name: String, value: Boolean) {
        sb.append(' ').append(name)
        sb.append(if (value) "=\"true\"" else "=\"false\"")
    }

    /** Appends an integer attribute: `name="123"`. */
    fun appendIntAttribute(sb: StringBuilder, name: String, value: Int) {
        sb.append(' ').append(name).append("=\"").append(value).append('"')
    }

    /**
     * Escapes and sanitizes an attribute value according to XML 1.0 rules.
     * Drops illegal XML characters (e.g. ASCII control characters 0x00-0x08, 0x0B, 0x0C, 0x0E-0x1F)
     * and escapes standard entities (&amp;, &lt;, &gt;, &quot;, &apos;).
     */
    fun escapeXmlAttr(sb: StringBuilder, text: CharSequence) {
        val len = text.length
        var i = 0
        while (i < len) {
            val c = text[i]
            when (c) {
                '&' -> sb.append("&amp;")

                '<' -> sb.append("&lt;")

                '>' -> sb.append("&gt;")

                '"' -> sb.append("&quot;")

                '\'' -> sb.append("&apos;")

                '\t' -> sb.append("&#9;")

                '\n' -> sb.append("&#10;")

                '\r' -> sb.append("&#13;")

                else -> {
                    // XML 1.0 legal character range check:
                    // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
                    when {
                        c in ' '..'\uD7FF' -> sb.append(c)

                        c >= '\uE000' && c <= '\uFFFD' -> sb.append(c)

                        Character.isHighSurrogate(c) -> {
                            if (i + 1 < len && Character.isLowSurrogate(text[i + 1])) {
                                sb.append(c).append(text[i + 1])
                                i++
                            }
                        }
                        // Characters outside these ranges (e.g. \u0000 through \u0008, \u000B, \u000C,
                        // \u000E-\u001F, unpaired surrogates, \uFFFE, \uFFFF) are invalid XML 1.0 and
                        // silently omitted to guarantee well-formedness.
                    }
                }
            }
            i++
        }
    }

    /** Sanitizes a string for safe XML attribute inclusion, returning the escaped string. */
    fun escapeXmlAttr(text: CharSequence?): String {
        if (text.isNullOrEmpty()) return ""
        val sb = StringBuilder(text.length + 16)
        escapeXmlAttr(sb, text)
        return sb.toString()
    }
}
