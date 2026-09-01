package com.noki.vpn.ui

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val AuroraBaseTeal = Color(0xFF087F8D)
private val AuroraSoftTeal = Color(0xFF075F70)
private val AuroraMutedTeal = Color(0xFF0A6B78)
private val AuroraDeepTeal = Color(0xFF0A5968)
private val AuroraSoftMint = Color(0xFF7AE7C7)

@Immutable
internal data class AuroraAnimationSettings(
    val motionSpeed: Float,
    val amplitude: Float,
    val blend: Float,
    val alphaMultiplier: Float,
    val verticalStart: Float,
    val verticalSpan: Float,
    val horizontalFrequency: Float,
    val horizontalDrift: Float,
    val morphRadius: Float,
    val intensityMultiplier: Float,
) {
    companion object {
        val Default = AuroraAnimationSettings(
            motionSpeed = 1.50f,
            amplitude = 0.63f,
            blend = 1.20f,
            alphaMultiplier = 0.40f,
            verticalStart = 0.50f,
            verticalSpan = 0.45f,
            horizontalFrequency = 0.89f,
            horizontalDrift = 0.02f,
            morphRadius = 0.60f,
            intensityMultiplier = 1.40f,
        )
    }
}

private val AuroraRuntimeShaderSource = """
uniform float uTime;
uniform float uAmplitude;
uniform float uBlend;
uniform float uAlpha;
uniform float uVerticalStart;
uniform float uVerticalSpan;
uniform float uHorizontalFrequency;
uniform float uHorizontalDrift;
uniform float uMorphRadius;
uniform float uIntensityMultiplier;
uniform float uTopLayerAlpha;
uniform float2 uResolution;
uniform float3 uColor0;
uniform float3 uColor1;
uniform float3 uColor2;

float3 permute(float3 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(float2 v) {
    const float4 C = float4(
        0.211324865405187, 0.366025403784439,
        -0.577350269189626, 0.024390243902439
    );
    float2 i = floor(v + dot(v, C.yy));
    float2 x0 = v - i + dot(i, C.xx);
    float2 i1 = (x0.x > x0.y) ? float2(1.0, 0.0) : float2(0.0, 1.0);
    float4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod(i, 289.0);

    float3 p = permute(
        permute(i.y + float3(0.0, i1.y, 1.0))
        + i.x + float3(0.0, i1.x, 1.0)
    );

    float3 m = max(
        0.5 - float3(
            dot(x0, x0),
            dot(x12.xy, x12.xy),
            dot(x12.zw, x12.zw)
        ),
        0.0
    );
    m = m * m;
    m = m * m;

    float3 x = 2.0 * fract(p * C.www) - 1.0;
    float3 h = abs(x) - 0.5;
    float3 ox = floor(x + 0.5);
    float3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);

    float3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

float3 colorRamp(float factor) {
    float firstT = clamp(factor / 0.5, 0.0, 1.0);
    float secondT = clamp((factor - 0.5) / 0.5, 0.0, 1.0);
    float3 first = mix(uColor0, uColor1, firstT);
    float3 second = mix(uColor1, uColor2, secondT);
    return mix(first, second, step(0.5, factor));
}

float auroraSample(float x, float y, float time) {
    float horizontalDrift = uHorizontalDrift * sin(time * 0.73);
    float noiseX = (x + horizontalDrift) * uHorizontalFrequency + time * uMorphRadius * 0.18;
    float noiseY = y * 0.34 + cos(time * 0.19) * 0.12 + time * 0.07;
    float height = snoise(float2(noiseX, noiseY)) * 0.5 * uAmplitude;
    height = exp(height);
    height = (y * 2.0 - height + 0.2);
    return 0.6 * height;
}

float auroraLayerAlpha(float screenY, float intensity) {
    float midPoint = 0.20;
    float auroraAlpha = smoothstep(midPoint - uBlend * 0.5, midPoint + uBlend * 0.5, intensity);
    float lowerGate = smoothstep(0.38, 0.76, screenY);
    float upperFeather = smoothstep(0.38, 0.70, screenY);
    float edgeFeather = 1.0 - smoothstep(0.96, 1.0, screenY) * 0.16;
    return auroraAlpha * lowerGate * upperFeather * edgeFeather;
}

half4 main(float2 coord) {
    float2 screenUv = coord / uResolution;

    // ReactBits aurora formula, but compressed into the lower part of Noki's
    // existing background. The top layer mirrors the same math, so it stays
    // visually paired with the lower Aurora without adding a second layer.
    float bottomScreenY = screenUv.y;
    float topScreenY = 1.0 - screenUv.y;
    float bottomLocalY = clamp((bottomScreenY - uVerticalStart) / max(uVerticalSpan, 0.001), 0.0, 1.0);
    float topLocalY = clamp((topScreenY - uVerticalStart) / max(uVerticalSpan, 0.001), 0.0, 1.0);
    float bottomX = screenUv.x;
    float topX = 1.0 - screenUv.x;
    float3 bottomRampColor = colorRamp(bottomX);
    float3 topRampColor = colorRamp(topX);

    float bottomIntensity = auroraSample(bottomX, bottomLocalY, uTime) * uIntensityMultiplier;
    float topIntensity = auroraSample(topX, topLocalY, uTime) * uIntensityMultiplier;

    float bottomAlpha = auroraLayerAlpha(bottomScreenY, bottomIntensity) * uAlpha;
    float topAlpha = auroraLayerAlpha(topScreenY, topIntensity) * uAlpha * uTopLayerAlpha;

    float3 auroraColor = bottomIntensity * bottomRampColor * bottomAlpha;
    auroraColor += topIntensity * topRampColor * topAlpha;
    float combinedAlpha = clamp(bottomAlpha + topAlpha, 0.0, 1.0);
    return half4(auroraColor, combinedAlpha);
}
""".trimIndent()

@Composable
internal fun AuroraOverlay(
    scale: Float,
    visible: Boolean,
    settings: AuroraAnimationSettings = AuroraAnimationSettings.Default,
    showTopLayer: Boolean = true,
) {
    var elapsedNanos by remember { mutableLongStateOf(0L) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1600),
        label = "auroraOverlayAlpha",
    )
    val shader = rememberRuntimeAuroraShader()

    LaunchedEffect(visible) {
        if (!visible) {
            elapsedNanos = 0L
            return@LaunchedEffect
        }
        val startNanos = withFrameNanos { it }
        while (true) {
            elapsedNanos = withFrameNanos { it } - startNanos
        }
    }

    if (alpha <= 0.01f) return

    val seconds = elapsedNanos / 1_000_000_000f
    val motionTime = auroraMotionTime(seconds, settings.motionSpeed)

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
            drawRuntimeAuroraOverlay(
                shader = shader,
                width = size.width,
                height = size.height,
                motionTime = motionTime,
                alpha = alpha,
                settings = settings,
                showTopLayer = showTopLayer,
            )
        } else {
            drawFallbackAuroraOverlay(
                width = size.width,
                height = size.height,
                motionTime = motionTime,
                alpha = alpha,
                scale = scale,
                settings = settings,
                showTopLayer = showTopLayer,
            )
        }
    }
}

internal fun auroraMotionTime(
    seconds: Float,
    motionSpeed: Float = AuroraAnimationSettings.Default.motionSpeed,
): Float {
    return seconds.coerceAtLeast(0f) * motionSpeed.coerceAtLeast(0f)
}

@Composable
private fun rememberRuntimeAuroraShader(): RuntimeShader? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(AuroraRuntimeShaderSource) }
    } else {
        null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawRuntimeAuroraOverlay(
    shader: RuntimeShader,
    width: Float,
    height: Float,
    motionTime: Float,
    alpha: Float,
    settings: AuroraAnimationSettings,
    showTopLayer: Boolean,
) {
    shader.setFloatUniform("uTime", motionTime)
    shader.setFloatUniform("uAmplitude", settings.amplitude)
    shader.setFloatUniform("uBlend", settings.blend)
    shader.setFloatUniform("uAlpha", alpha * settings.alphaMultiplier)
    shader.setFloatUniform("uVerticalStart", settings.verticalStart)
    shader.setFloatUniform("uVerticalSpan", settings.verticalSpan)
    shader.setFloatUniform("uHorizontalFrequency", settings.horizontalFrequency)
    shader.setFloatUniform("uHorizontalDrift", settings.horizontalDrift)
    shader.setFloatUniform("uMorphRadius", settings.morphRadius)
    shader.setFloatUniform("uIntensityMultiplier", settings.intensityMultiplier)
    shader.setFloatUniform("uTopLayerAlpha", if (showTopLayer) 1f else 0f)
    shader.setFloatUniform("uResolution", width, height)
    shader.setFloatUniform("uColor0", AuroraDeepTeal.red, AuroraDeepTeal.green, AuroraDeepTeal.blue)
    shader.setFloatUniform("uColor1", AuroraBaseTeal.red, AuroraBaseTeal.green, AuroraBaseTeal.blue)
    shader.setFloatUniform("uColor2", AuroraSoftMint.red, AuroraSoftMint.green, AuroraSoftMint.blue)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
        this.alpha = 255
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, width, height, paint)
    }
}

private fun DrawScope.drawFallbackAuroraOverlay(
    width: Float,
    height: Float,
    motionTime: Float,
    alpha: Float,
    scale: Float,
    settings: AuroraAnimationSettings,
    showTopLayer: Boolean,
) {
    val wide = max(width, height)
    val constrainedScale = scale.coerceIn(0.88f, 1.18f)
    val pulse = 0.86f + 0.14f * sin(motionTime * 0.65f)
    val effectAlpha = alpha * settings.alphaMultiplier
    val movement = settings.horizontalDrift / AuroraAnimationSettings.Default.horizontalDrift
    val sizeMultiplier = (AuroraAnimationSettings.Default.horizontalFrequency / settings.horizontalFrequency)
        .coerceIn(0.6f, 1.8f)

    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.46f to Color.Transparent,
                0.73f to AuroraSoftTeal.copy(alpha = 0.06f * effectAlpha),
                1.00f to AuroraBaseTeal.copy(alpha = 0.16f * effectAlpha),
            ),
        ),
    )
    if (showTopLayer) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to AuroraBaseTeal.copy(alpha = 0.16f * effectAlpha),
                    0.27f to AuroraSoftTeal.copy(alpha = 0.06f * effectAlpha),
                    0.54f to Color.Transparent,
                    1.00f to Color.Transparent,
                ),
            ),
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AuroraMutedTeal.copy(alpha = 0.16f * pulse * effectAlpha),
                AuroraBaseTeal.copy(alpha = 0.14f * effectAlpha),
                Color.Transparent,
            ),
            center = Offset(width * (0.52f + 0.22f * movement * sin(motionTime * 0.47f)), height * 1.02f),
            radius = wide * 0.72f * constrainedScale * sizeMultiplier,
        ),
        radius = wide * 0.72f * constrainedScale * sizeMultiplier,
        center = Offset(width * (0.52f + 0.22f * movement * sin(motionTime * 0.47f)), height * 1.02f),
    )
    if (showTopLayer) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AuroraMutedTeal.copy(alpha = 0.16f * pulse * effectAlpha),
                    AuroraBaseTeal.copy(alpha = 0.14f * effectAlpha),
                    Color.Transparent,
                ),
                center = Offset(width * (0.48f - 0.22f * movement * sin(motionTime * 0.47f)), height * -0.02f),
                radius = wide * 0.72f * constrainedScale * sizeMultiplier,
            ),
            center = Offset(width * (0.48f - 0.22f * movement * sin(motionTime * 0.47f)), height * -0.02f),
            radius = wide * 0.72f * constrainedScale * sizeMultiplier,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AuroraDeepTeal.copy(alpha = 0.12f * effectAlpha),
                Color.Transparent,
            ),
            center = Offset(width * (0.18f + 0.18f * movement * cos(motionTime * 0.31f)), height * 0.92f),
            radius = wide * 0.56f * constrainedScale * sizeMultiplier,
        ),
        radius = wide * 0.56f * constrainedScale * sizeMultiplier,
        center = Offset(width * (0.18f + 0.18f * movement * cos(motionTime * 0.31f)), height * 0.92f),
    )
    if (showTopLayer) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AuroraDeepTeal.copy(alpha = 0.12f * effectAlpha),
                    Color.Transparent,
                ),
                center = Offset(width * (0.82f - 0.18f * movement * cos(motionTime * 0.31f)), height * 0.08f),
                radius = wide * 0.56f * constrainedScale * sizeMultiplier,
            ),
            center = Offset(width * (0.82f - 0.18f * movement * cos(motionTime * 0.31f)), height * 0.08f),
            radius = wide * 0.56f * constrainedScale * sizeMultiplier,
        )
    }
}
