package com.noki.vpn.data

import kotlinx.coroutines.CancellationException

class DeviceActionCoordinator(
    private val api: DeviceActionApi,
    private val refreshContextAfterUnauthorized: suspend () -> DeviceContext? = { null },
) {
    data class DeviceContext(
        val token: String,
        val currentDeviceId: String?,
        val currentDeviceKey: String?,
    )

    sealed interface ActionResult<out T> {
        data class Success<T>(val value: T) : ActionResult<T>
        data object LogoutRequired : ActionResult<Nothing>
        data class Failure(val error: Throwable) : ActionResult<Nothing>
    }

    suspend fun logoutCurrent(context: DeviceContext): ActionResult<Unit> {
        return handleLogoutErrors {
            api.deleteCurrentDevice(
                token = context.token,
                currentDeviceId = context.currentDeviceId,
                currentDeviceKey = context.currentDeviceKey,
            )
        }
    }

    suspend fun clearOtherDevices(context: DeviceContext): ActionResult<List<BackendDevice>> {
        return handleUnauthorized(context) { activeContext ->
            api.clearOtherDevices(
                token = activeContext.token,
                currentDeviceId = activeContext.currentDeviceId,
                currentDeviceKey = activeContext.currentDeviceKey,
            )
        }
    }

    suspend fun deleteDevice(
        context: DeviceContext,
        deviceId: String,
    ): ActionResult<List<BackendDevice>> {
        return handleUnauthorized(context) { activeContext ->
            api.deleteDevice(
                token = activeContext.token,
                deviceId = deviceId,
                currentDeviceId = activeContext.currentDeviceId,
                currentDeviceKey = activeContext.currentDeviceKey,
            )
        }
    }

    suspend fun setFullAccess(
        context: DeviceContext,
        deviceId: String,
        fullAccess: Boolean,
    ): ActionResult<List<BackendDevice>> {
        return handleUnauthorized(context) { activeContext ->
            api.setDeviceFullAccess(
                token = activeContext.token,
                deviceId = deviceId,
                fullAccess = fullAccess,
                currentDeviceId = activeContext.currentDeviceId,
                currentDeviceKey = activeContext.currentDeviceKey,
            )
        }
    }

    private suspend fun <T> handleUnauthorized(
        context: DeviceContext,
        block: suspend (DeviceContext) -> T,
    ): ActionResult<T> {
        return runCatching { block(context) }.fold(
            onSuccess = { ActionResult.Success(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                if (error is BackendException && error.statusCode == 401) {
                    val refreshResult = runCatching { refreshContextAfterUnauthorized() }
                    val refreshError = refreshResult.exceptionOrNull()
                    if (refreshError is CancellationException) throw refreshError
                    if (refreshError is AuthRefreshRejectedException) {
                        ActionResult.LogoutRequired
                    } else if (refreshError != null) {
                        ActionResult.Failure(refreshError)
                    } else {
                        val refreshedContext = refreshResult.getOrNull()
                        if (refreshedContext == null) {
                            ActionResult.Failure(error)
                        } else {
                            runCatching { block(refreshedContext) }.fold(
                                onSuccess = { ActionResult.Success(it) },
                                onFailure = ::deviceActionFailure,
                            )
                        }
                    }
                } else {
                    ActionResult.Failure(error)
                }
            },
        )
    }

    private fun <T> deviceActionFailure(error: Throwable): ActionResult<T> =
        if (error is CancellationException) {
            throw error
        } else if (error is AuthRefreshRejectedException) {
            ActionResult.LogoutRequired
        } else {
            ActionResult.Failure(error)
        }

    private suspend fun handleLogoutErrors(block: suspend () -> Unit): ActionResult<Unit> {
        return runCatching { block() }.fold(
            onSuccess = { ActionResult.LogoutRequired },
            onFailure = { error ->
                if (error is CancellationException) throw error
                ActionResult.LogoutRequired
            },
        )
    }
}
