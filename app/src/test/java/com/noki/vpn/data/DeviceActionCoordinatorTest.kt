package com.noki.vpn.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DeviceActionCoordinatorTest {
    @Test
    fun missingRefreshContextDoesNotRequestLogout() = runBlocking {
        val coordinator = DeviceActionCoordinator(
            api = UnauthorizedDeviceActionApi(),
            refreshContextAfterUnauthorized = { null },
        )

        val result = coordinator.setFullAccess(deviceContext(), "device-2", fullAccess = true)

        assertTrue(result is DeviceActionCoordinator.ActionResult.Failure)
        assertTrue((result as DeviceActionCoordinator.ActionResult.Failure).error is BackendException)
    }

    @Test
    fun repeatedResourceUnauthorizedDoesNotRequestLogout() = runBlocking {
        val coordinator = DeviceActionCoordinator(
            api = UnauthorizedDeviceActionApi(),
            refreshContextAfterUnauthorized = { deviceContext(token = "rotated-access") },
        )

        val result = coordinator.setFullAccess(deviceContext(), "device-2", fullAccess = true)

        assertTrue(result is DeviceActionCoordinator.ActionResult.Failure)
        assertTrue((result as DeviceActionCoordinator.ActionResult.Failure).error is BackendException)
    }

    @Test
    fun confirmedRefreshRejectionRequestsLogout() = runBlocking {
        val coordinator = DeviceActionCoordinator(
            api = UnauthorizedDeviceActionApi(),
            refreshContextAfterUnauthorized = {
                throw AuthRefreshRejectedException(BackendException("invalid_refresh_token", 401))
            },
        )

        val result = coordinator.setFullAccess(deviceContext(), "device-2", fullAccess = true)

        assertTrue(result is DeviceActionCoordinator.ActionResult.LogoutRequired)
    }

    @Test
    fun transientRefreshFailureDoesNotRequestLogout() = runBlocking {
        val coordinator = DeviceActionCoordinator(
            api = UnauthorizedDeviceActionApi(),
            refreshContextAfterUnauthorized = { throw IOException("offline") },
        )

        val result = coordinator.setFullAccess(deviceContext(), "device-2", fullAccess = true)

        assertTrue(result is DeviceActionCoordinator.ActionResult.Failure)
        assertTrue((result as DeviceActionCoordinator.ActionResult.Failure).error is IOException)
    }

    @Test
    fun initialDeviceCallCancellationPropagates() = runBlocking {
        val coordinator = DeviceActionCoordinator(CancellingDeviceActionApi())

        val result = runCatching {
            coordinator.setFullAccess(deviceContext(), "device-2", fullAccess = true)
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun retriedDeviceCallCancellationPropagates() = runBlocking {
        val api = UnauthorizedThenCancellingDeviceActionApi()
        val coordinator = DeviceActionCoordinator(
            api = api,
            refreshContextAfterUnauthorized = { deviceContext(token = "rotated-access") },
        )

        val result = runCatching {
            coordinator.setFullAccess(deviceContext(), "device-2", fullAccess = true)
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun logoutCancellationPropagates() = runBlocking {
        val coordinator = DeviceActionCoordinator(CancellingDeviceActionApi())

        val result = runCatching {
            coordinator.logoutCurrent(deviceContext())
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun offlineLogoutStillRequiresLocalLogout() = runBlocking {
        val coordinator = DeviceActionCoordinator(OfflineDeviceActionApi())

        val result = coordinator.logoutCurrent(deviceContext())

        assertTrue(result is DeviceActionCoordinator.ActionResult.LogoutRequired)
    }

    private fun deviceContext(token: String = "old-access") =
        DeviceActionCoordinator.DeviceContext(
            token = token,
            currentDeviceId = "device-1",
            currentDeviceKey = "device-key-1",
        )

    private open class CancellingDeviceActionApi : DeviceActionApi {
        override suspend fun clearOtherDevices(
            token: String,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): List<BackendDevice> = throw CancellationException("cancelled")

        override suspend fun deleteCurrentDevice(
            token: String,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): Unit = throw CancellationException("cancelled")

        override suspend fun deleteDevice(
            token: String,
            deviceId: String,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): List<BackendDevice> = throw CancellationException("cancelled")

        override suspend fun setDeviceFullAccess(
            token: String,
            deviceId: String,
            fullAccess: Boolean,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): List<BackendDevice> = throw CancellationException("cancelled")
    }

    private class UnauthorizedThenCancellingDeviceActionApi : CancellingDeviceActionApi() {
        private var callCount = 0

        override suspend fun setDeviceFullAccess(
            token: String,
            deviceId: String,
            fullAccess: Boolean,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): List<BackendDevice> {
            callCount += 1
            if (callCount == 1) throw BackendException("unauthorized", 401)
            throw CancellationException("cancelled")
        }
    }

    private class UnauthorizedDeviceActionApi : CancellingDeviceActionApi() {
        override suspend fun setDeviceFullAccess(
            token: String,
            deviceId: String,
            fullAccess: Boolean,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): List<BackendDevice> = throw BackendException("unauthorized", 401)
    }

    private class OfflineDeviceActionApi : CancellingDeviceActionApi() {
        override suspend fun deleteCurrentDevice(
            token: String,
            currentDeviceId: String?,
            currentDeviceKey: String?,
        ): Unit = throw IOException("offline")
    }
}
