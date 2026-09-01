package com.noki.vpn.ui

import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.ServerLocation
import com.kyant.backdrop.backdrops.LayerBackdrop
import java.util.Locale

internal data class HomeServerMenuEntry(
    val key: String,
    val location: ServerLocation,
)

internal fun serverMenuLocations(locations: List<ServerLocation>): List<HomeServerMenuEntry> {
    return locations.map { location ->
        HomeServerMenuEntry(
            key = location.code,
            location = location,
        )
    }
}

@Composable
internal fun HomeDropdownArrowButton(
    modifier: Modifier,
    scale: Float,
    arrowRotation: Float,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .nokiGlassSurface(
                shape = shape,
                backdrop = backdrop,
                liveGlassEnabled = liveGlassEnabled,
                scale = 1f,
                elevationDp = 14f,
                shadowAlpha = if (liveGlassEnabled) 0.05f else NokiUiKitPolicy.simpleSurfaceShadowAlpha,
                highlightAlpha = 0.30f,
                surfaceColor = SettingsTextMuted.copy(alpha = 0.05f),
                simpleSurfaceColor = SettingsBgSoft,
                blurAndLensEnabled = false,
                backdropShadowsEnabled = false,
                dropShadowRadiusDp = 4f,
            ),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = arrowRotation
                },
        ) {
            val strokeWidth = designDp(2.2f, scale).toPx()
            val arrowHalfWidth = designDp(6.2f, scale).toPx()
            val arrowHalfHeight = designDp(4.0f, scale).toPx()
            val arrowCenterY = center.y + designDp(1f, scale).toPx()
            drawLine(
                color = HomeAccentPrimary,
                start = Offset(center.x - arrowHalfWidth, arrowCenterY - arrowHalfHeight),
                end = Offset(center.x, arrowCenterY + arrowHalfHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = HomeAccentPrimary,
                start = Offset(center.x, arrowCenterY + arrowHalfHeight),
                end = Offset(center.x + arrowHalfWidth, arrowCenterY - arrowHalfHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun HomeServerMenuItem(
    location: ServerLocation,
    language: AppLanguage,
    scale: Float,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    animateOnlineIndicator: Boolean,
    onClick: () -> Unit,
) {
    val countryText = localizedServerCountry(location, language)
    val loadText = location.loadPercent?.let { "$it %" } ?: "-- %"
    val latencyText = location.latencyMs?.let { "$it ms" } ?: "-- ms"
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 520f,
        ),
        label = "homeServerMenuItemPress",
    )
    val shape = RoundedCornerShape(settingsDp(20f, 1f))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(designDp(NokiUiKitPolicy.homeServerItemHeightDp, scale))
            .homeServerItemGlassSurface(
                shape = shape,
                backdrop = backdrop,
                liveGlassEnabled = liveGlassEnabled,
                scale = scale,
                layerBlock = {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = designDp(NokiUiKitPolicy.homeServerItemHorizontalPaddingDp, scale)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountryFlagMarker(
                location = location,
                scale = scale,
                sizeDp = NokiUiKitPolicy.homeServerItemFlagSizeDp,
            )
            Spacer(modifier = Modifier.width(designDp(15f, scale)))
            Text(
                text = countryText.ifBlank { location.code.uppercase(Locale.ROOT) },
                color = HomeTextPrimary,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = designSp(NokiUiKitPolicy.homeServerItemNameTextSp, scale),
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(designDp(12f, scale)))
            Text(
                text = loadText,
                color = HomeTextSecondary,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = designSp(NokiUiKitPolicy.homeServerItemMetricTextSp, scale),
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Right,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
            Spacer(modifier = Modifier.width(designDp(8f, scale)))
            Text(
                text = latencyText,
                color = HomeTextSecondary,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = designSp(NokiUiKitPolicy.homeServerItemMetricTextSp, scale),
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Right,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
            Spacer(modifier = Modifier.width(designDp(9f, scale)))
            Box(
                modifier = Modifier.size(designDp(24f, scale)),
                contentAlignment = Alignment.Center,
            ) {
                OnlineStatusIndicator(
                    online = location.isOnline,
                    animate = animateOnlineIndicator,
                )
            }
        }
    }
}

internal fun Modifier.homeServerItemGlassSurface(
    shape: Shape,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    scale: Float,
    layerBlock: GraphicsLayerScope.() -> Unit,
): Modifier {
    return nokiGlassSurface(
        shape = shape,
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        scale = scale,
        surfaceColor = SettingsTextMuted.copy(alpha = NokiUiKitPolicy.homeServerLazyItemSurfaceAlpha),
        simpleSurfaceColor = SettingsBgSoft,
        layerBlock = layerBlock,
    )
}

@Composable
internal fun CountryFlagMarker(
    location: ServerLocation,
    scale: Float,
    sizeDp: Float = 46f,
) {
    val context = LocalContext.current
    val resourceName = countryFlagResourceName(location)
    val resourceId = remember(context, resourceName) {
        resourceName?.let { context.resources.getIdentifier(it, "raw", context.packageName) } ?: 0
    }
    val markerSize = designDp(sizeDp, scale)
    Box(
        modifier = Modifier
            .size(markerSize)
            .clip(RoundedCornerShape(percent = 50))
            .background(HomeStroke.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        if (resourceId != 0) {
            FigmaSvgAsset(
                resId = resourceId,
                viewportWidth = 512,
                viewportHeight = 512,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "??",
                color = HomeTextPrimary,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = designSp(15f, scale),
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true)),
            )
        }
    }
}

@Composable
internal fun OnlineStatusIndicator(
    online: Boolean,
    animate: Boolean,
) {
    if (!online) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = HomeTextSecondary.copy(alpha = 0.56f),
                radius = size.minDimension * 0.18f,
                center = center,
            )
        }
        return
    }

    if (!animate) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = HomeAccentPrimary,
                radius = size.minDimension * 0.2f,
                center = center,
            )
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "serverOnlinePulse")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "serverOnlinePulseProgress",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val baseRadius = size.minDimension * 0.2f
        val maxRadius = size.minDimension * 0.48f

        fun drawPulse(phase: Float) {
            val radius = baseRadius + (maxRadius - baseRadius) * phase
            val alpha = (1f - phase).coerceIn(0f, 1f) * 0.24f
            if (alpha > 0.01f) {
                drawCircle(
                    color = HomeAccentPrimary.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                )
            }
        }

        drawPulse(pulseProgress)
        drawPulse((pulseProgress + 0.5f) % 1f)
        drawCircle(
            color = HomeAccentPrimary.copy(alpha = 0.18f),
            radius = size.minDimension * 0.32f,
            center = center,
        )
        drawCircle(
            color = HomeAccentPrimary,
            radius = baseRadius,
            center = center,
        )
    }
}

internal fun countryMarkerCode(location: ServerLocation): String? {
    return location.countryCode
        .trim()
        .uppercase(Locale.ROOT)
        .takeIf { it.length == 2 && it.all { char -> char in 'A'..'Z' } }
}

internal fun countryFlagResourceName(location: ServerLocation): String? =
    countryMarkerCode(location)?.let { code -> "flag_${code.lowercase(Locale.ROOT)}" }

internal fun localizedServerCountry(location: ServerLocation, language: AppLanguage): String {
    if (location.code.equals("lv", ignoreCase = true)) {
        return tr(language, "Латвия", "Latvia")
    }
    val normalized = location.country.trim().lowercase()
    return when {
        location.code.equals("lv", ignoreCase = true) ||
            normalized == "latvia" ||
            normalized == "латвия" -> tr(language, "Латвия", "Latvia")
        else -> location.country
    }
}
