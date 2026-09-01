package com.noki.vpn.data

import com.noki.vpn.BuildConfig

object NokiBackendConfig {
    val apiBaseUrl: String = BuildConfig.NOKI_API_BASE_URL.trim().trimEnd('/')
    val backendProbeHost: String = BuildConfig.NOKI_BACKEND_PROBE_HOST.trim()
    val backendProbeHealthUrl: String = BuildConfig.NOKI_BACKEND_PROBE_HEALTH_URL.trim()
}
