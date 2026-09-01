package com.noki.vpn

internal data class DeviceSessionSnapshot(
    val id: String,
    val key: String,
    val role: String,
)

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
