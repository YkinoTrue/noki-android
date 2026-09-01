package com.noki.vpn.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noki.vpn.R
import com.noki.vpn.vpn.VpnRuntimeMode
@Composable
internal fun AuthBackground(
    @DrawableRes backgroundRes: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(backgroundRes),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
    )
}

internal fun welcomeInlineMessageForDisplay(
    inlineMessage: String?,
    vpnRuntimeMode: VpnRuntimeMode,
): String? {
    return inlineMessage.takeUnless { vpnRuntimeMode == VpnRuntimeMode.AUTH_TEMP }
}

@Composable
internal fun AuthLogo(
    top: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = top)
            .height(78.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(R.drawable.login_logo_mark_vector),
            contentDescription = null,
            modifier = Modifier
                .width(68.4.dp)
                .height(78.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

