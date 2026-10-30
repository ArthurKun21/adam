---
title: "Screenshot"
---

## Capture a screenshot

Capture screenshots with the `ScreenCaptureRequest`. The request checks the remote protocol version. It fails when the format is unsupported.

```kotlin
val adapter = RawImageScreenCaptureAdapter()
val image = adb.execute(
    request = ScreenCaptureRequest(adapter),
    serial = "emulator-5554"
).toBufferedImage()

if (!ImageIO.write(image, "png", File("/tmp/screen.png"))) {
    throw IOException("Failed to find png writer")
}
```

### Image adapter

You must transform the framebuffer bytes into a usable image. Two adapters are available: `RawImageScreenCaptureAdapter` and `BufferedImageScreenCaptureAdapter`.

`RawImageScreenCaptureAdapter` provides the metadata and the `byte[]` that holds the screenshot. Its return type is `RawImage`. `RawImage` supports reading the pixel value by index with `RawImage#getARGB(index: Int)`. You can also transform the image into a Java `BufferedImage`.

If you capture many screenshots from a particular device, use the `BufferedImageScreenCaptureAdapter`. It reduces the additional memory allocations during the image transformation.

All adapters reduce memory consumption by reusing their internal buffers. If you use the same adapter on multiple threads in parallel, set the buffer to `null` every time or provide an external buffer allocated per thread.
