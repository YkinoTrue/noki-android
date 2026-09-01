package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.BackendException
import com.noki.vpn.data.BackendIncyDevice
import com.noki.vpn.data.BackendIncyDeviceCreate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncyDeviceWorkflowTest {
    @Test
    fun `duplicate device name is explained instead of generic validation`() {
        val error = BackendException("Устройство INCY с таким названием уже существует", 422)

        assertEquals(
            "Устройство INCY с таким названием уже существует",
            AppErrorMapper.readableNetworkError(AppLanguage.RU, error),
        )
        assertEquals(
            "An INCY device with this name already exists",
            AppErrorMapper.readableNetworkError(AppLanguage.EN, error),
        )
    }

    @Test
    fun `unknown validation detail is not exposed as a duplicate name`() {
        val error = BackendException("Internal validation details", 422)

        assertEquals(
            "Проверьте введенные данные",
            AppErrorMapper.readableNetworkError(AppLanguage.RU, error),
        )
        assertEquals(
            "Check the entered data",
            AppErrorMapper.readableNetworkError(AppLanguage.EN, error),
        )
    }

    @Test
    fun `create trims name and returns one validated import value`() = runBlocking {
        val gateway = FakeGateway()
        val result = IncyDeviceWorkflow(gateway).create("  Планшет  ")

        assertEquals("Планшет", gateway.lastName)
        assertTrue(result.importLink.value.startsWith("incy://crypt1/"))
    }

    private class FakeGateway : IncyDeviceGateway {
        var lastName = ""

        override suspend fun create(name: String): BackendIncyDeviceCreate {
            lastName = name
            return BackendIncyDeviceCreate(
                device = BackendIncyDevice(id = "d1", name = name),
                importLink = "incy://crypt1/AAECAwQFBgcICQoLNyIQL3rDwRZqnyoD8pGKSLXP6o8NdSXQVSSALNbbUyIr__tWGFUexdIfKvvmDnuDGbmBvuppfNef6aKNZUwOm4c-Sg",
            )
        }
    }
}
