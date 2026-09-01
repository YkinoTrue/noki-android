package com.noki.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

@Composable
internal fun NokiLiquidToggle(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier = Modifier,
    sizeFactor: Float = 1f,
) {
    SecurityLiquidToggle(
        selected = selected,
        onSelectedChange = onSelectedChange,
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        sizeFactor = sizeFactor,
        modifier = modifier,
    )
}

@Composable
internal fun NokiLiquidThumb(
    modifier: Modifier,
    animation: DampedDragAnimation,
    backdrop: LayerBackdrop?,
    trackBackdrop: LayerBackdrop,
    liveGlassEnabled: Boolean,
    inactiveColor: Color,
    activeColor: Color,
    surfaceProgress: Float = animation.value,
    sizeFactor: Float = 1f,
) {
    val shape = RoundedCornerShape(percent = 50)
    if (liveGlassEnabled && backdrop != null) {
        Box(
            modifier = modifier.drawBackdrop(
                backdrop = rememberCombinedBackdrop(
                    backdrop,
                    rememberBackdrop(trackBackdrop) { drawBackdrop ->
                        val progress = animation.pressProgress
                        val scaleX = lerpFloat(2f / 3f, 0.92f, progress)
                        val scaleY = lerpFloat(0f, 0.75f, progress)
                        scale(scaleX, scaleY) {
                            drawBackdrop()
                        }
                    },
                ),
                shape = { shape },
                effects = {
                    val progress = animation.pressProgress
                    lens(
                        (5f * sizeFactor).dp.toPx() * progress,
                        (10f * sizeFactor).dp.toPx() * progress,
                        chromaticAberration = true,
                    )
                },
                highlight = {
                    Highlight.Ambient.copy(
                        width = Highlight.Ambient.width / 1.5f,
                        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                        alpha = animation.pressProgress,
                    )
                },
                shadow = {
                    Shadow(
                        radius = (4f * sizeFactor).dp,
                        color = Color.Black.copy(alpha = 0.05f),
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = (4f * sizeFactor).dp * animation.pressProgress,
                        alpha = animation.pressProgress,
                    )
                },
                layerBlock = {
                    scaleX = animation.scaleX
                    scaleY = animation.scaleY
                    val velocity = animation.velocity / 50f
                    scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                },
                onDrawSurface = {
                    val animationMix = maxOf(animation.pressProgress, surfaceProgress)
                    drawRect(
                        lerpColor(inactiveColor, activeColor, animationMix)
                            .copy(alpha = 1f - animation.pressProgress),
                    )
                },
            ),
        )
    } else {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = animation.scaleX
                    scaleY = animation.scaleY
                }
                .background(lerpColor(inactiveColor, activeColor, surfaceProgress), shape),
        )
    }
}
