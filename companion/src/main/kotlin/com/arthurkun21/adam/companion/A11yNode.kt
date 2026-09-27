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

import org.json.JSONArray
import org.json.JSONObject

/**
 * In-memory snapshot of an Accessibility UI element node.
 * Decoupled from live Android AccessibilityNodeInfo objects to prevent
 * Binder proxy memory leaks, avoid stale node crashes, and ensure
 * instant thread-safe serialization to both XML and JSON.
 *
 * `bounds` are the node's *visible* bounds: the raw `getBoundsInScreen` rectangle
 * intersected with the display, the node's window and every scrollable ancestor,
 * exactly like UIAutomator's `getVisibleBoundsInScreen`. They are therefore never
 * negative and never extend past the screen.
 */
class A11yNode {

    var index: Int = 0
    var text: String = ""
    var resourceId: String = ""
    var className: String = ""
    var packageName: String = ""
    var contentDesc: String = ""

    var checkable: Boolean = false
    var checked: Boolean = false
    var clickable: Boolean = false
    var enabled: Boolean = true
    var focusable: Boolean = false
    var focused: Boolean = false
    var scrollable: Boolean = false
    var longClickable: Boolean = false
    var password: Boolean = false
    var selected: Boolean = false
    var visibleToUser: Boolean = true

    // Window metadata for multi-window awareness
    var windowId: Int = -1
    var windowType: String = ""
    var windowLayer: Int = 0
    var windowActive: Boolean = false
    var windowFocused: Boolean = false

    // Advanced semantics for Agent perception & verification
    var editable: Boolean = false
    var isHeading: Boolean = false
    var screenReaderFocusable: Boolean = false
    var stateDescription: String = ""
    var errorText: String = ""
    var paneTitle: String = ""
    var tooltip: String = ""

    var left: Int = 0
    var top: Int = 0
    var right: Int = 0
    var bottom: Int = 0

    var drawingOrder: Int = 0
    var hint: String = ""

    val children: MutableList<A11yNode> = ArrayList(4)

    val width: Int get() = right - left
    val height: Int get() = bottom - top

    val boundsString: String
        get() = "[$left,$top][$right,$bottom]"

    /**
     * Serializes this node and all of its descendants into standard UIAutomator XML.
     */
    fun writeXml(sb: StringBuilder) {
        sb.append("<node")
        XmlUtils.appendIntAttribute(sb, "index", index)
        XmlUtils.appendAttribute(sb, "text", text)
        XmlUtils.appendAttribute(sb, "resource-id", resourceId)
        XmlUtils.appendAttribute(sb, "class", className)
        XmlUtils.appendAttribute(sb, "package", packageName)
        XmlUtils.appendAttribute(sb, "content-desc", contentDesc)
        XmlUtils.appendBooleanAttribute(sb, "checkable", checkable)
        XmlUtils.appendBooleanAttribute(sb, "checked", checked)
        XmlUtils.appendBooleanAttribute(sb, "clickable", clickable)
        XmlUtils.appendBooleanAttribute(sb, "enabled", enabled)
        XmlUtils.appendBooleanAttribute(sb, "focusable", focusable)
        XmlUtils.appendBooleanAttribute(sb, "focused", focused)
        XmlUtils.appendBooleanAttribute(sb, "scrollable", scrollable)
        XmlUtils.appendBooleanAttribute(sb, "long-clickable", longClickable)
        XmlUtils.appendBooleanAttribute(sb, "password", password)
        XmlUtils.appendBooleanAttribute(sb, "selected", selected)
        XmlUtils.appendBooleanAttribute(sb, "visible-to-user", visibleToUser)
        XmlUtils.appendAttribute(sb, "bounds", boundsString)
        XmlUtils.appendIntAttribute(sb, "drawing-order", drawingOrder)
        if (hint.isNotEmpty()) {
            XmlUtils.appendAttribute(sb, "hint", hint)
        }

        // Window metadata (emitted on window root nodes)
        if (windowId >= 0) {
            XmlUtils.appendIntAttribute(sb, "window-id", windowId)
            if (windowType.isNotEmpty()) {
                XmlUtils.appendAttribute(sb, "window-type", windowType)
            }
            XmlUtils.appendIntAttribute(sb, "window-layer", windowLayer)
            if (windowActive) {
                XmlUtils.appendBooleanAttribute(sb, "window-active", true)
            }
            if (windowFocused) {
                XmlUtils.appendBooleanAttribute(sb, "window-focused", true)
            }
        }

        // Extended semantic attributes
        if (editable) {
            XmlUtils.appendBooleanAttribute(sb, "editable", true)
        }
        if (isHeading) {
            XmlUtils.appendBooleanAttribute(sb, "heading", true)
        }
        if (screenReaderFocusable) {
            XmlUtils.appendBooleanAttribute(sb, "screen-reader-focusable", true)
        }
        if (stateDescription.isNotEmpty()) {
            XmlUtils.appendAttribute(sb, "state-description", stateDescription)
        }
        if (errorText.isNotEmpty()) {
            XmlUtils.appendAttribute(sb, "error", errorText)
        }
        if (paneTitle.isNotEmpty()) {
            XmlUtils.appendAttribute(sb, "pane-title", paneTitle)
        }
        if (tooltip.isNotEmpty()) {
            XmlUtils.appendAttribute(sb, "tooltip", tooltip)
        }

        if (children.isEmpty()) {
            sb.append(" />")
        } else {
            sb.append('>')
            for (child in children) {
                child.writeXml(sb)
            }
            sb.append("</node>")
        }
    }

    /** Serializes this node into a hierarchical JSON tree object. */
    fun toTreeJson(): JSONObject {
        val obj = toBaseJson()
        try {
            if (children.isNotEmpty()) {
                val childrenArr = JSONArray()
                for (child in children) {
                    childrenArr.put(child.toTreeJson())
                }
                obj.put("children", childrenArr)
            }
        } catch (ignored: Throwable) {
        }
        return obj
    }

    /**
     * Serializes this node into an element JSON object for the flat list.
     * Does NOT include child references.
     */
    fun toFlatElementJson(): JSONObject = toBaseJson()

    private fun toBaseJson(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("class", className)
            obj.put("package", packageName)
            obj.put("resource-id", resourceId)
            obj.put("text", text)
            obj.put("content-desc", contentDesc)
            obj.put("bounds", boundsString)

            val bounds = JSONObject()
            bounds.put("left", left)
            bounds.put("top", top)
            bounds.put("right", right)
            bounds.put("bottom", bottom)
            obj.put("parsed_bounds", bounds)

            obj.put("clickable", clickable)
            obj.put("scrollable", scrollable)
            obj.put("checkable", checkable)
            obj.put("checked", checked)
            obj.put("enabled", enabled)
            obj.put("focusable", focusable)
            obj.put("focused", focused)
            obj.put("selected", selected)
            obj.put("password", password)
            obj.put("long-clickable", longClickable)
            obj.put("visible-to-user", visibleToUser)
            obj.put("drawing-order", drawingOrder)
            if (hint.isNotEmpty()) {
                obj.put("hint", hint)
            }

            // Window metadata
            if (windowId >= 0) {
                obj.put("window_id", windowId)
                obj.put("window_type", windowType)
                obj.put("window_layer", windowLayer)
                if (windowActive) obj.put("window_active", true)
                if (windowFocused) obj.put("window_focused", true)
            }

            // Extended semantics
            if (editable) {
                obj.put("editable", true)
            }
            if (isHeading) {
                obj.put("is_heading", true)
            }
            if (screenReaderFocusable) {
                obj.put("screen_reader_focusable", true)
            }
            if (stateDescription.isNotEmpty()) {
                obj.put("state_description", stateDescription)
            }
            if (errorText.isNotEmpty()) {
                obj.put("error", errorText)
            }
            if (paneTitle.isNotEmpty()) {
                obj.put("pane_title", paneTitle)
            }
            if (tooltip.isNotEmpty()) {
                obj.put("tooltip", tooltip)
            }
        } catch (ignored: Throwable) {
        }
        return obj
    }

    /**
     * Determines whether this node contains useful information or interactivity
     * and should be included in the flat elements array for agent perception.
     */
    val isInformativeOrInteractive: Boolean
        get() {
            val hasContent = text.isNotEmpty() || contentDesc.isNotEmpty() || resourceId.isNotEmpty() ||
                stateDescription.isNotEmpty() || errorText.isNotEmpty() || paneTitle.isNotEmpty()
            val isInteractive = clickable || scrollable || checkable || focusable || longClickable || editable
            return hasContent || isInteractive || isHeading
        }

    /** Recursively collects all informative or interactive nodes into a flat list. */
    fun collectFlatElements(flatList: MutableList<JSONObject>) {
        if (isInformativeOrInteractive) {
            flatList.add(toFlatElementJson())
        }
        for (child in children) {
            child.collectFlatElements(flatList)
        }
    }
}
