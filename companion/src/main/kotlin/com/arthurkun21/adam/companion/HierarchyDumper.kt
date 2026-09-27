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

import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Universal, ultra-stable UI hierarchy dumper for Android (API 24 - 36+).
 *
 * Capabilities:
 * 1. Zero WaitForIdle hangs: Bypasses UIAutomator's waitForIdle() timeout.
 * 2. Multi-window penetration: Captures App, Dialog, System UI, IME Keyboard, Split-Screen.
 * 3. UIAutomator parity: invisible nodes are skipped and bounds are clipped to the
 *    display, the window and scrollable ancestors, so the tree describes what is on
 *    screen and nothing else. `include_invisible=1` keeps everything (debugging).
 * 4. Rich semantic extraction: Captures errorText, isHeading, editable, paneTitle, tooltip.
 * 5. Atomic screenshot snapshot: On API 30+, captures hardware bitmap and DOM tree simultaneously.
 * 6. W3C XML 1.0 compliance: Strict character sanitization prevents parse failures.
 * 7. Batched IPC on API 33+: descendants are prefetched per getChild call instead of one
 *    Binder round trip per node.
 */
object HierarchyDumper {

    private const val TAG = "AdamHierarchyDumper"
    private const val MAX_DEPTH = 75
    private const val MAX_NODES = 8000

    private val SCREENSHOT_EXECUTOR = Executors.newSingleThreadExecutor()

    private const val ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT = 3
    private const val SCREENSHOT_INTERVAL_RETRY_MS = 350L

    /**
     * What a dump should contain. Every field defaults to the cheapest useful answer.
     */
    class DumpOptions {
        var includeInvisible: Boolean = false
        var wantXml: Boolean = true
        var wantElements: Boolean = false
        var wantTree: Boolean = false

        /** Applies `fields=xml,elements,tree` and `include_invisible=1`. */
        fun applyQuery(query: Map<String, String>?): DumpOptions {
            if (query == null) return this
            query["fields"]?.trim()?.takeIf { it.isNotEmpty() }?.let { fields ->
                wantXml = false
                wantElements = false
                wantTree = false
                for (f in fields.split(",")) {
                    when (f.trim().lowercase()) {
                        "xml" -> wantXml = true
                        "elements" -> wantElements = true
                        "tree" -> wantTree = true
                    }
                }
            }
            query["include_invisible"]?.let { inv ->
                includeInvisible = inv == "1" || inv.equals("true", ignoreCase = true)
            }
            return this
        }

        companion object {
            fun forDump(): DumpOptions = DumpOptions().apply {
                wantElements = true
                wantTree = true
            }

            fun forSnapshot(): DumpOptions = DumpOptions()
        }
    }

    /** Per-dump counters reported back to the host. */
    private class DumpStats {
        var nodes = 0
        var skippedInvisible = 0
        var truncated = false
    }

    /**
     * Dumps the hierarchy as a JSON response containing the requested representations.
     */
    fun dump(service: AccessibilityService, options: DumpOptions): JSONObject {
        val startTime = System.currentTimeMillis()
        val result = JSONObject()

        try {
            val displayInfo = DisplayUtils.getDisplayInfo(service)
            val stats = DumpStats()
            val rootSnapshots = captureRootSnapshots(service, displayInfo, options, stats)

            result.put("rotation", displayInfo.rotation)
            result.put("width", displayInfo.width)
            result.put("height", displayInfo.height)

            if (rootSnapshots.isEmpty()) {
                result.put("success", false)
                result.put("error", "No active window or root node found")
                result.put("xml", "")
                result.put("elements", JSONArray())
                return result
            }

            if (options.wantXml) {
                result.put("xml", buildXml(rootSnapshots, displayInfo.rotation))
            }
            if (options.wantElements) {
                val elementsJson = JSONArray()
                val flatList = ArrayList<JSONObject>()
                for (root in rootSnapshots) {
                    root.collectFlatElements(flatList)
                }
                for (elem in flatList) {
                    elementsJson.put(elem)
                }
                result.put("elements", elementsJson)
            }
            if (options.wantTree) {
                val trees = JSONArray()
                for (root in rootSnapshots) {
                    trees.put(root.toTreeJson())
                }
                result.put("tree", if (trees.length() == 1) trees.getJSONObject(0) else trees)
            }

            result.put("success", true)
            result.put("node_count", stats.nodes)
            result.put("skipped_invisible", stats.skippedInvisible)
            result.put("truncated", stats.truncated)
            result.put("window_count", rootSnapshots.size)
            result.put("elapsed_ms", System.currentTimeMillis() - startTime)

            if (service is CompanionAccessibilityService) {
                result.put("package", service.currentPackageName)
                result.put("activity", service.currentActivityName)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Dump failed with exception", t)
            try {
                result.put("success", false)
                result.put("error", "Dump failed: ${t.message}")
                result.put("xml", "")
                result.put("elements", JSONArray())
            } catch (ignored: Throwable) {
            }
        }

        return result
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun requestScreenshot(
        service: AccessibilityService,
        bitmapRef: AtomicReference<Bitmap?>,
        errorRef: AtomicInteger,
        latch: CountDownLatch,
        allowRetry: Boolean,
    ) {
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                SCREENSHOT_EXECUTOR,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                        try {
                            val buffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val hwBitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                            if (hwBitmap != null) {
                                val swBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                hwBitmap.recycle()
                                buffer.close()
                                bitmapRef.set(swBitmap)
                            }
                        } catch (t: Throwable) {
                            Log.w(TAG, "Error copying screenshot buffer", t)
                        } finally {
                            latch.countDown()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        errorRef.set(errorCode)
                        if (allowRetry && errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT) {
                            Log.i(
                                TAG,
                                "takeScreenshot rate-limited; retrying after $SCREENSHOT_INTERVAL_RETRY_MS ms",
                            )
                            SCREENSHOT_EXECUTOR.execute {
                                try {
                                    Thread.sleep(SCREENSHOT_INTERVAL_RETRY_MS)
                                } catch (e: InterruptedException) {
                                    Thread.currentThread().interrupt()
                                }
                                requestScreenshot(service, bitmapRef, errorRef, latch, false)
                            }
                            return
                        }
                        Log.w(TAG, "takeScreenshot failed, errorCode: $errorCode")
                        latch.countDown()
                    }
                },
            )
        } catch (t: Throwable) {
            Log.w(TAG, "takeScreenshot invocation error", t)
            latch.countDown()
        }
    }

    /**
     * Dumps an atomic snapshot combining hardware screenshot (JPEG base64) and UI hierarchy
     * at the exact same clock tick, eliminating temporal phase mismatch. When the screenshot
     * is present, `width` / `height` are the bitmap's own dimensions so the host
     * normalizes coordinates against the very image it is looking at.
     */
    fun dumpAtomicSnapshot(service: AccessibilityService, options: DumpOptions): JSONObject {
        val startTime = System.currentTimeMillis()

        // 1. Trigger hardware screenshot asynchronously. The framework rate-limits
        //    takeScreenshot to one call per ~333 ms (ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT = 3);
        //    back-to-back observations therefore wait out the interval and retry once
        //    here, which is far cheaper than the host falling back to adb screencap.
        val bitmapRef = AtomicReference<Bitmap?>(null)
        val errorRef = AtomicInteger(0)
        val screenshotLatch = CountDownLatch(1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestScreenshot(service, bitmapRef, errorRef, screenshotLatch, true)
        } else {
            screenshotLatch.countDown()
        }

        // 2. Concurrently capture the UI hierarchy
        val dumpData = dump(service, options)

        // 3. Wait for the screenshot (one retry after the rate-limit interval fits inside)
        try {
            screenshotLatch.await(2500L, TimeUnit.MILLISECONDS)
        } catch (ignored: InterruptedException) {
        }

        val bitmap = bitmapRef.get()
        if (bitmap != null) {
            try {
                val baos = java.io.ByteArrayOutputStream(bitmap.width * bitmap.height / 4)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val jpegBytes = baos.toByteArray()
                val base64Str = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                dumpData.put("screenshot_base64", base64Str)
                dumpData.put("has_screenshot", true)
                dumpData.put("width", bitmap.width)
                dumpData.put("height", bitmap.height)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to compress screenshot to JPEG Base64", t)
                try {
                    dumpData.put("has_screenshot", false)
                } catch (ignored: Throwable) {
                }
            } finally {
                bitmap.recycle()
            }
        } else {
            try {
                dumpData.put("has_screenshot", false)
                dumpData.put("screenshot_error_code", errorRef.get())
                dumpData.put(
                    "screenshot_error",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (errorRef.get() != 0) {
                            "takeScreenshot failed with errorCode ${errorRef.get()}"
                        } else {
                            "Screenshot capture timed out"
                        }
                    } else {
                        "takeScreenshot not supported on Android < 11"
                    },
                )
            } catch (ignored: Throwable) {
            }
        }

        try {
            dumpData.put("atomic_elapsed_ms", System.currentTimeMillis() - startTime)
        } catch (ignored: Throwable) {
        }

        return dumpData
    }

    /**
     * Dumps the hierarchy directly as a raw standard UIAutomator XML string.
     */
    fun dumpXml(service: AccessibilityService, options: DumpOptions): String {
        val displayInfo = DisplayUtils.getDisplayInfo(service)
        val rootSnapshots = captureRootSnapshots(service, displayInfo, options, DumpStats())
        if (rootSnapshots.isEmpty()) {
            return "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n" +
                "<hierarchy rotation=\"${displayInfo.rotation}\" />\n"
        }
        return buildXml(rootSnapshots, displayInfo.rotation)
    }

    /**
     * Builds the standard Android UIAutomator XML string from root snapshots.
     */
    private fun buildXml(roots: List<A11yNode>, rotation: Int): String {
        val sb = StringBuilder(roots.size * 1024 + 256)
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n")
        sb.append("<hierarchy rotation=\"$rotation\">")

        for ((i, root) in roots.withIndex()) {
            root.index = i
            root.writeXml(sb)
        }

        sb.append("</hierarchy>\n")
        return sb.toString()
    }

    private class RawRootEntry(
        val root: AccessibilityNodeInfo,
        val windowId: Int,
        val windowType: String,
        val windowLayer: Int,
        val windowActive: Boolean,
        val windowFocused: Boolean,
        /** Screen rectangle of the window, or null when unknown (display bounds are used). */
        val windowBounds: Rect?,
    )

    /**
     * Captures snapshots of all active and interactive windows with adaptive progressive retry support.
     * Guaranteed to capture the complete hierarchy even during activity transitions and cold starts.
     */
    private fun captureRootSnapshots(
        service: AccessibilityService,
        displayInfo: DisplayUtils.DisplayInfo,
        options: DumpOptions,
        stats: DumpStats,
    ): List<A11yNode> {
        val retryBackoff = longArrayOf(40L, 80L, 120L, 160L, 220L, 300L)
        var rawRoots: List<RawRootEntry> = emptyList()
        for (attempt in 0..retryBackoff.size) {
            rawRoots = getActiveRawRoots(service)
            if (rawRoots.isNotEmpty()) {
                break
            }
            if (attempt < retryBackoff.size) {
                SystemClock.sleep(retryBackoff[attempt])
            }
        }

        val displayRect = Rect(0, 0, displayInfo.width, displayInfo.height)
        val snapshots = ArrayList<A11yNode>(rawRoots.size)

        for ((i, entry) in rawRoots.withIndex()) {
            try {
                var clip = Rect(displayRect)
                val windowBounds = entry.windowBounds
                if (windowBounds != null && !clip.intersect(windowBounds)) {
                    // Window entirely off the display (e.g. a second display): keep the
                    // display rect so the root still serializes with sane bounds.
                    clip = Rect(displayRect)
                }
                val snapshot = snapshotNode(entry.root, 0, i, clip, options, stats)
                if (snapshot != null) {
                    snapshot.windowId = entry.windowId
                    snapshot.windowType = entry.windowType
                    snapshot.windowLayer = entry.windowLayer
                    snapshot.windowActive = entry.windowActive
                    snapshot.windowFocused = entry.windowFocused
                    snapshots.add(snapshot)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to snapshot root window ${entry.windowId}", t)
            } finally {
                safeRecycle(entry.root)
            }
        }

        return snapshots
    }

    private fun windowRoot(window: AccessibilityWindowInfo): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                return window.getRoot(AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID)
            } catch (ignored: Throwable) {
            }
        }
        return window.root
    }

    private fun childOf(node: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                return node.getChild(index, AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID)
            } catch (ignored: Throwable) {
            }
        }
        return node.getChild(index)
    }

    /**
     * Multi-tier hierarchy root discovery strategy:
     * Tier 1: Multi-window enumeration (sorted by Z-layer descending).
     * Tier 2: Active window direct fallback (if getWindows() is empty or missing foreground app).
     * Tier 3: Focused input / accessibility node backtracking (walks up parent chain to top root).
     */
    private fun getActiveRawRoots(service: AccessibilityService): List<RawRootEntry> {
        val roots = ArrayList<RawRootEntry>()
        val seenHashes = HashSet<Int>()
        var hasAppWindow = false

        // Tier 1: Multi-window enumeration (App, Dialogs, Popups, Keyboards, Split-screen)
        try {
            val windows = service.windows
            if (!windows.isNullOrEmpty()) {
                val sortedWindows = windows.sortedByDescending { it.layer }

                for (window in sortedWindows) {
                    try {
                        val root = windowRoot(window)
                        if (root != null) {
                            val hash = root.hashCode()
                            if (seenHashes.add(hash)) {
                                val typeStr = resolveWindowType(window.type)
                                if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                                    hasAppWindow = true
                                }
                                val bounds = Rect()
                                window.getBoundsInScreen(bounds)
                                roots.add(
                                    RawRootEntry(
                                        root,
                                        window.id,
                                        typeStr,
                                        window.layer,
                                        window.isActive,
                                        window.isFocused,
                                        if (bounds.isEmpty) null else bounds,
                                    ),
                                )
                            } else {
                                safeRecycle(root)
                            }
                        }
                    } catch (ignored: Throwable) {
                    }
                }
            }
        } catch (ignored: Throwable) {
        }

        // Tier 2: Active window fallback (if getWindows() returned empty or lacked active app window)
        if (!hasAppWindow) {
            try {
                val activeRoot = service.rootInActiveWindow
                if (activeRoot != null) {
                    val hash = activeRoot.hashCode()
                    if (seenHashes.add(hash)) {
                        roots.add(
                            0,
                            RawRootEntry(
                                activeRoot,
                                activeRoot.windowId,
                                "application",
                                0,
                                true,
                                true,
                                null,
                            ),
                        )
                        hasAppWindow = true
                    } else {
                        safeRecycle(activeRoot)
                    }
                }
            } catch (ignored: Throwable) {
            }
        }

        // Tier 3: Focused node backtracking (recovers window tree during transient transitions)
        if (roots.isEmpty()) {
            var focused: AccessibilityNodeInfo? = null
            try {
                focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            } catch (ignored: Throwable) {
            }
            if (focused == null) {
                try {
                    focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                } catch (ignored: Throwable) {
                }
            }

            if (focused != null) {
                try {
                    var current: AccessibilityNodeInfo = focused
                    var parent: AccessibilityNodeInfo? = current.parent
                    while (parent != null) {
                        if (current !== focused) {
                            safeRecycle(current)
                        }
                        current = parent
                        parent = current.parent
                    }
                    val hash = current.hashCode()
                    if (seenHashes.add(hash)) {
                        roots.add(
                            RawRootEntry(
                                current,
                                current.windowId,
                                "application",
                                0,
                                true,
                                true,
                                null,
                            ),
                        )
                    } else {
                        safeRecycle(current)
                    }
                } catch (ignored: Throwable) {
                } finally {
                    safeRecycle(focused)
                }
            }
        }

        return roots
    }

    private fun resolveWindowType(type: Int): String = when (type) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "application"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "input_method"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "system"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "accessibility_overlay"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "split_screen_divider"
        else -> "unknown"
    }

    /**
     * Recursively snapshots an AccessibilityNodeInfo into an immutable A11yNode,
     * extracting rich semantic properties (error, heading, editable, paneTitle, tooltip, stateDescription).
     *
     * `clip` is the rectangle the node may be visible in: the display intersected with the
     * window and every scrollable ancestor. Children that are not visible to the user are skipped
     * unless `options.includeInvisible` is set; the window root itself is always kept, as in
     * UIAutomator.
     *
     * Protected against recursion loops via MAX_DEPTH and MAX_NODES without flat hash collision drops.
     */
    @Suppress("DEPRECATION")
    private fun snapshotNode(
        node: AccessibilityNodeInfo?,
        depth: Int,
        childIndex: Int,
        clip: Rect,
        options: DumpOptions,
        stats: DumpStats,
    ): A11yNode? {
        if (node == null) return null
        if (depth > MAX_DEPTH || stats.nodes >= MAX_NODES) {
            stats.truncated = true
            return null
        }

        val visible = try {
            node.isVisibleToUser
        } catch (ignored: Throwable) {
            true
        }
        if (!visible && depth > 0 && !options.includeInvisible) {
            stats.skippedInvisible++
            return null
        }

        stats.nodes++
        val snapshot = A11yNode()
        snapshot.index = childIndex
        snapshot.visibleToUser = visible
        var childClip = clip

        try {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (!bounds.intersect(clip)) {
                bounds.setEmpty()
            }
            snapshot.left = bounds.left
            snapshot.top = bounds.top
            snapshot.right = bounds.right
            snapshot.bottom = bounds.bottom

            val text = node.text
            val desc = node.contentDescription
            val pkg = node.packageName
            val cls = node.className
            val resId = node.viewIdResourceName

            snapshot.text = text?.toString() ?: ""
            snapshot.contentDesc = desc?.toString() ?: ""
            snapshot.packageName = pkg?.toString() ?: ""
            snapshot.className = cls?.toString() ?: ""
            snapshot.resourceId = resId ?: ""

            snapshot.clickable = node.isClickable
            snapshot.checkable = node.isCheckable
            snapshot.checked = node.isChecked
            snapshot.enabled = node.isEnabled
            snapshot.focusable = node.isFocusable
            snapshot.focused = node.isFocused
            snapshot.scrollable = node.isScrollable
            snapshot.longClickable = node.isLongClickable
            snapshot.password = node.isPassword
            snapshot.selected = node.isSelected

            // Children of a scrollable container are clipped by it (UIAutomator's
            // trimScrollableParent): a list row half under the toolbar keeps only its
            // visible part, so its center is a point the user can actually touch.
            if (snapshot.scrollable && !bounds.isEmpty) {
                childClip = Rect(bounds)
            }

            // 1. Editable property
            snapshot.editable = node.isEditable

            // 2. Error message (crucial for form validation detection)
            val err = node.error
            if (!err.isNullOrEmpty()) {
                snapshot.errorText = err.toString()
            }

            // 3. Drawing order (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    snapshot.drawingOrder = node.drawingOrder
                } catch (ignored: Throwable) {
                }
            }

            // 4. Hint text (API 26+). When the field is empty the framework reports the
            //    hint as the text; keep text and hint apart so "empty input" stays detectable.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    node.hintText?.let { snapshot.hint = it.toString() }
                    if (node.isShowingHintText) {
                        snapshot.text = ""
                    }
                } catch (ignored: Throwable) {
                }
            }

            // 5. Heading, PaneTitle, Tooltip, ScreenReaderFocusable (API 28+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    snapshot.isHeading = node.isHeading
                } catch (ignored: Throwable) {
                }

                try {
                    snapshot.screenReaderFocusable = node.isScreenReaderFocusable
                } catch (ignored: Throwable) {
                }

                try {
                    node.paneTitle?.let { snapshot.paneTitle = it.toString() }
                } catch (ignored: Throwable) {
                }

                try {
                    node.tooltipText?.let { snapshot.tooltip = it.toString() }
                } catch (ignored: Throwable) {
                }
            }

            // 6. State Description (API 30+ Jetpack Compose semantics: expanded, collapsed, etc.)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    node.stateDescription?.let { snapshot.stateDescription = it.toString() }
                } catch (ignored: Throwable) {
                }
            }

            // Recursively process children
            val childCount = node.childCount
            for (i in 0 until childCount) {
                var childNode: AccessibilityNodeInfo? = null
                try {
                    childNode = childOf(node, i)
                    if (childNode != null) {
                        val childSnapshot = snapshotNode(childNode, depth + 1, i, childClip, options, stats)
                        if (childSnapshot != null) {
                            snapshot.children.add(childSnapshot)
                        }
                    }
                } catch (ignored: Throwable) {
                } finally {
                    childNode?.let { safeRecycle(it) }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error reading node properties", t)
        }

        return snapshot
    }

    /**
     * Safely recycles node info on Android API < 30 to prevent Binder pool exhaustion.
     */
    fun safeRecycle(node: AccessibilityNodeInfo?) {
        if (node == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                @Suppress("DEPRECATION")
                node.recycle()
            } catch (ignored: Throwable) {
            }
        }
    }

    /**
     * Finds the currently focused or editable input node for text entry.
     * Window roots are enumerated once and recycled; only the returned node stays alive.
     */
    fun findInputNode(service: AccessibilityService): AccessibilityNodeInfo? {
        val roots = getActiveRawRoots(service)
        var found: AccessibilityNodeInfo? = null
        try {
            for (entry in roots) {
                try {
                    val focused = entry.root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null) {
                        found = focused
                        return found
                    }
                } catch (ignored: Throwable) {
                }
            }
            for (entry in roots) {
                val editable = findFirstEditable(entry.root, 0)
                if (editable != null) {
                    found = editable
                    return found
                }
            }
            return null
        } finally {
            for (entry in roots) {
                if (entry.root !== found) {
                    safeRecycle(entry.root)
                }
            }
        }
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (node == null || depth > MAX_DEPTH) return null
        try {
            if (node.isEditable && node.isFocusable && node.isEnabled && node.isVisibleToUser) {
                return node
            }
            val count = node.childCount
            for (i in 0 until count) {
                val child = childOf(node, i)
                if (child != null) {
                    val res = findFirstEditable(child, depth + 1)
                    if (res != null) return res
                    safeRecycle(child)
                }
            }
        } catch (ignored: Throwable) {
        }
        return null
    }
}
