package com.noki.vpn.data

internal object AppLogStorageMigrationPolicy {
    sealed interface RawSource {
        data class Encrypted(val raw: String) : RawSource
        data class Legacy(val raw: String) : RawSource
        data object Empty : RawSource
    }

    fun selectRawSource(
        encryptedRaw: String?,
        legacyRaw: String?,
    ): RawSource {
        encryptedRaw?.takeIf { it.isNotBlank() }?.let { return RawSource.Encrypted(it) }
        legacyRaw?.takeIf { it.isNotBlank() }?.let { return RawSource.Legacy(it) }
        return RawSource.Empty
    }
}
