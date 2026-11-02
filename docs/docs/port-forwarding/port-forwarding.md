---
title: "Port forwarding"
---

## Port forwarding

### Create port forwarding rule

This request forwards a local port to a remote device port.

The local port is an instance of `LocalPortSpec`:

- `LocalTcpPortSpec(val port: Int)`. Maps a local TCP port.
- `LocalUnixSocketPortSpec(val path: String)`. Maps a local Unix domain socket path.

The remote port is an instance of `RemotePortSpec`:

- `RemoteTcpPortSpec(val port: Int)`. Maps a remote TCP port.
- `RemoteAbstractPortSpec(val unixDomainSocketName: String)`
- `RemoteReservedPortSpec(val unixDomainSocketName: String)`
- `RemoteFilesystemPortSpec(val unixDomainSocketName: String)`
- `RemoteDevPortSpec(val charDeviceName: String)`
- `JDWPPortSpec(val processId: Int)`

```kotlin
adb.execute(request = PortForwardRequest(
    local = LocalTcpPortSpec(12042),
    remote = RemoteTcpPortSpec(12042),
    serial = "emulator-5554",
    mode = DEFAULT)
)
```

The `DEFAULT` mode rebinds the local port when a rule already exists. The `NO_REBIND` mode fails the request when the local port already has a rule.

### List port forwarding rules

The following request retrieves the list of port forwarding rules:

```kotlin
val rules: List<PortForwardingRule> = adb.execute(ListPortForwardsRequest("emulator-5554"))
```

### Remove a port forwarding rule

The removal request does not need the remote port spec:

```kotlin
adb.execute(request = RemovePortForwardRequest(
    local = LocalTcpPortSpec(12042),
    serial = "emulator-5554")
)
```

### Remove all port forwards

The following request removes all port forwarding rules:

```kotlin
adb.execute(request = RemoveAllPortForwardsRequest(
    serial = "emulator-5554"
))
```

## Reverse port forwarding

### Create reverse port forwarding rule

This request forwards a port on the device to a port on the host.

The `local` parameter defines the port on the device. It is an instance of `RemotePortSpec`:

- `RemoteTcpPortSpec(val port: Int)`. Maps a TCP port on the device.
- `RemoteAbstractPortSpec(val unixDomainSocketName: String)`
- `RemoteReservedPortSpec(val unixDomainSocketName: String)`
- `RemoteFilesystemPortSpec(val unixDomainSocketName: String)`
- `RemoteDevPortSpec(val charDeviceName: String)`
- `JDWPPortSpec(val processId: Int)`

The `remote` parameter defines the port on the host. It is an instance of `LocalPortSpec`:

- `LocalTcpPortSpec(val port: Int)`. Maps a TCP port on the host.
- `LocalUnixSocketPortSpec(val path: String)`. Maps a Unix domain socket path on the host.

```kotlin
adb.execute(request = ReversePortForwardRequest(
    local = RemoteTcpPortSpec(12042),
    remote = LocalTcpPortSpec(12042),
    serial = "emulator-5554",
    mode = DEFAULT)
)
```

The `DEFAULT` mode rebinds the device port when a rule already exists. The `NO_REBIND` mode fails the request when the device port already has a rule.

### List reverse port forwarding rules

The following request retrieves the list of reverse port forwarding rules:

```kotlin
val rules: List<ReversePortForwardingRule> = adb.execute(ListReversePortForwardsRequest(), "emulator-5554")
```

### Remove a reverse port forwarding rule

The removal request needs only the `local` port spec. The `serial` goes to the `execute` call:

```kotlin
adb.execute(request = RemoveReversePortForwardRequest(
    local = RemoteTcpPortSpec(12042)),
    serial = "emulator-5554"
)
```

### Remove all reverse port forwards

The following request removes all reverse port forwarding rules:

```kotlin
adb.execute(request = RemoveAllReversePortForwardsRequest(), serial = "emulator-5554")
```
