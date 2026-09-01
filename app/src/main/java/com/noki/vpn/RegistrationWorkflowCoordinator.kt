package com.noki.vpn

internal data class WorkflowOwner(
    val generation: Long,
    val requestKey: String,
)

internal class RegistrationWorkflowCoordinator {
    private var generation = 0L
    private var activeOwner: WorkflowOwner? = null

    @Synchronized
    fun begin(requestKey: String): WorkflowOwner {
        generation += 1L
        return WorkflowOwner(generation, requestKey).also { activeOwner = it }
    }

    @Synchronized
    fun accepts(owner: WorkflowOwner, currentRequestKey: String): Boolean {
        return activeOwner == owner && owner.requestKey == currentRequestKey
    }

    @Synchronized
    fun invalidate() {
        generation += 1L
        activeOwner = null
    }
}
