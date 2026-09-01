package com.noki.vpn.ui

internal object PlansLayoutPolicy {
    fun centeredCardTopPaddingDp(
        viewportHeightDp: Float,
        cardHeightDp: Float,
        bottomReservedDp: Float,
        minimumTopDp: Float,
    ): Float {
        val availableHeight = viewportHeightDp - bottomReservedDp
        return ((availableHeight - cardHeightDp) / 2f).coerceAtLeast(minimumTopDp)
    }
}
