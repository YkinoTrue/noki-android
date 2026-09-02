package com.noki.vpn

import com.noki.vpn.data.BackendDevice
import com.noki.vpn.data.DeviceSession

internal data class DeviceSessionSnapshot(
    val id: String,
    val key: String,
    val role: String,
)

internal object DeviceLocalNamePolicy {
    fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(80)

    fun aliasesByDeviceId(
        devices: List<DeviceSession>,
        backendDevices: List<BackendDevice>,
        storedNames: Map<String, String>,
    ): Map<String, String> {
        val backendById = backendDevices.associateBy(BackendDevice::id)
        return buildMap {
            devices.forEach { device ->
                val stableKey = backendById[device.id]?.deviceKey.orEmpty()
                val localName = sequenceOf(stableKey, device.id)
                    .filter(String::isNotBlank)
                    .mapNotNull(storedNames::get)
                    .map(::normalize)
                    .firstOrNull(String::isNotBlank)
                if (localName != null) put(device.id, localName)
            }
        }
    }

    fun apply(devices: List<DeviceSession>, names: Map<String, String>): List<DeviceSession> =
        devices.map { device ->
            val localName = names[device.id]?.let(::normalize).orEmpty()
            if (localName.isBlank()) device else device.copy(title = localName)
        }
}

internal class DeviceWorkflowCoordinator {
    private var generation = 0L
    private var owner: WorkflowOwner? = null

    @Synchronized
    fun begin(snapshot: DeviceSessionSnapshot): WorkflowOwner {
        generation += 1L
        return WorkflowOwner(
            generation = generation,
            requestKey = "${snapshot.id}:${snapshot.key}:${snapshot.role}",
        ).also { owner = it }
    }

    @Synchronized
    fun accepts(candidate: WorkflowOwner): Boolean = owner == candidate

    @Synchronized
    fun invalidate() {
        generation += 1L
        owner = null
    }
}
