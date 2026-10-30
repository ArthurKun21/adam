---
title: Files
---

The requests in this section transfer files or information about files.

Use the [recommended requests][1] to push or pull without handling device features and protocols.

The sync v1 and sync v2 protocols require a fallback when the device does not support all features. The [compatibility requests][2] implement this fallback.

To work with the sync v1 protocol directly, see [sync v1][3]. For sync v2, see [sync v2][4].

For compatibility with older devices, you can use plain `ls`. The [ls wrapper][5] `ListFilesRequest` provides this option.

[1]: recommended.md
[2]: compat.md
[3]: sync-v1.md
[4]: sync-v2.md
[5]: ls.md
