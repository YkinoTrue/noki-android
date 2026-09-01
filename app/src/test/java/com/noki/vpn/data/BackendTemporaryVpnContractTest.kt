package com.noki.vpn.data

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendTemporaryVpnContractTest {
    @Test
    fun `temporary vpn requests carry one stable device key before auth`() {
        val requestBodies = mutableListOf<JSONObject>()
        val client = BackendApiClient(
            client = OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    val buffer = Buffer()
                    chain.request().body?.writeTo(buffer)
                    requestBodies += JSONObject(buffer.readUtf8())
                    val body = if (chain.request().url.encodedPath.endsWith("/challenge")) {
                        """{"nonce":"signed-value","expires_in":60}"""
                    } else {
                        validSessionJson().toString()
                    }
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(body.toResponseBody("application/json".toMediaType()))
                        .build()
                })
                .build(),
            baseUrl = "https://api.example.test",
        )

        runBlocking {
            client.createTemporaryVpnChallenge(
                publicKey = "public-key",
                deviceKey = "stable-device-key",
                deviceName = "Pixel test",
                platform = "android",
            )
            client.createTemporaryVpnSession(
                publicKey = "public-key",
                nonce = "signed-value",
                signature = "signature",
                deviceKey = "stable-device-key",
            )
        }

        assertEquals("stable-device-key", requestBodies[0].getString("device_key"))
        assertEquals("Pixel test", requestBodies[0].getString("device_name"))
        assertEquals("android", requestBodies[0].getString("platform"))
        assertEquals("stable-device-key", requestBodies[1].getString("device_key"))
    }

    @Test
    fun `challenge parser keeps nonce and short ttl`() {
        val challenge = BackendTemporaryVpnResponseParser.parseChallenge(
            JSONObject("""{"nonce":"signed-value","expires_in":60}"""),
        )

        assertEquals("signed-value", challenge.nonce)
        assertEquals(60L, challenge.expiresInSeconds)
    }

    @Test
    fun `session parser separates lease controls from vless credentials`() {
        val response = JSONObject(
            """
            {
              "mode":"auth_temp",
              "session_id":"5d330ebf-6204-4773-99c2-f219f808b056",
              "control_token":"opaque-control-token",
              "traffic_limit_bytes":104857600,
              "expires_at":"2026-07-14T12:10:00Z",
              "can_connect":true,
              "profile_code":"auto",
              "location_code":"lv",
              "location_name":"Латвия",
              "endpoint_code":"auth-temp-lv",
              "entry_host":"vpn.example.test",
              "connect_ip":"203.0.113.10",
              "entry_port":443,
              "server_name":"cdn.example.test",
              "proxy_type":"vless",
              "transport":"tcp",
              "security":"reality",
              "fingerprint":"chrome",
              "allow_insecure":false,
              "enable_mux":false,
              "random_user_agent":false,
              "public_key":"server-public-key",
              "short_id":"0123456789abcdef",
              "vpn_username":"temporary-user",
              "vpn_secret":"b3e85e7a-c160-4dd0-9b26-86f32b9aa5d6",
              "endpoint_candidates":[{
                "code":"auth-temp-lv",
                "node_id":"node-lv",
                "label":"Latvia",
                "location_code":"lv",
                "location_name":"Латвия",
                "entry_host":"vpn.example.test",
                "connect_ip":"203.0.113.10",
                "entry_port":443,
                "server_name":"cdn.example.test",
                "proxy_type":"vless",
                "transport":"tcp",
                "security":"reality"
              }]
            }
            """.trimIndent(),
        )

        val parsed = BackendTemporaryVpnResponseParser.parseSession(response)

        assertEquals("auth_temp", parsed.mode)
        assertEquals("opaque-control-token", parsed.controlToken)
        assertEquals(104857600L, parsed.trafficLimitBytes)
        assertEquals("vpn.example.test", parsed.vpnSession.entryHost)
        assertEquals("203.0.113.10", parsed.vpnSession.connectIp)
        assertEquals(
            "203.0.113.10",
            parsed.vpnSession.endpointCandidates.single().connectionHost(),
        )
        assertEquals(
            "203.0.113.10",
            EndpointSelector.profileFromCandidate(
                parsed.vpnSession,
                parsed.vpnSession.endpointCandidates.single(),
            ).host,
        )
        assertEquals("b3e85e7a-c160-4dd0-9b26-86f32b9aa5d6", parsed.vpnSession.vpnSecret)
        assertTrue(parsed.expiresAtEpochMillis > 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `session parser rejects non temporary mode`() {
        val response = JSONObject(
            """
            {
              "mode":"account",
              "session_id":"session",
              "control_token":"control",
              "traffic_limit_bytes":1,
              "expires_at":"2026-07-14T12:10:00Z"
            }
            """.trimIndent(),
        )

        BackendTemporaryVpnResponseParser.parseSession(response)
    }

    private fun validSessionJson() = JSONObject(
        """
        {
          "mode":"auth_temp",
          "session_id":"session",
          "control_token":"control",
          "traffic_limit_bytes":104857600,
          "expires_at":"2026-07-14T12:10:00Z",
          "can_connect":true,
          "location_code":"lv",
          "location_name":"Latvia",
          "entry_host":"vpn.example.test",
          "server_name":"cdn.example.test",
          "security":"reality",
          "vpn_username":"temporary-user",
          "vpn_secret":"b3e85e7a-c160-4dd0-9b26-86f32b9aa5d6"
        }
        """.trimIndent(),
    )
}
