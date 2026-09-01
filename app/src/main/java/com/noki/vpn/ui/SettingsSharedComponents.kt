package com.noki.vpn.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noki.vpn.data.PlanSummary
import java.util.Locale

@Composable
internal fun SettingsText(
    text: String,
    fontSize: Float,
    lineHeight: Float,
    letterSpacing: Float,
    color: Color,
    scale: Float,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
) {
    val density = LocalDensity.current
    Text(
        text = text,
        color = color,
        fontFamily = ManropeFontFamily,
        fontWeight = fontWeight,
        fontSize = settingsSp(fontSize, scale, density.fontScale),
        lineHeight = settingsSp(lineHeight, scale, density.fontScale),
        letterSpacing = letterSpacing.sp,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        modifier = modifier,
    )
}

internal fun settingsDp(value: Float, scale: Float): Dp = (value * scale).dp

internal fun settingsSp(value: Float, scale: Float, fontScale: Float) = (value * scale / fontScale).sp

internal fun settingsCleanPlanTitle(value: String): String {
    return value
        .replace(Regex("([_-])(monthly|yearly)$", RegexOption.IGNORE_CASE), "")
        .replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
}

internal fun settingsPlanBadgeFontSize(title: String): Float {
    return when {
        title.length > 10 -> 12f
        title.length > 8 -> 13f
        title.length > 6 -> 14f
        else -> 18f
    }
}

internal fun settingsPlanColor(plan: PlanSummary?): Color? {
    val raw = plan?.badgeColor?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val hex = raw.removePrefix("#")
    return runCatching {
        when (hex.length) {
            6 -> Color(0xFF000000 or hex.toLong(16))
            8 -> Color(hex.toLong(16))
            else -> null
        }
    }.getOrNull()
}
