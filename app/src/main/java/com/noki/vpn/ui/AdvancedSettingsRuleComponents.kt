package com.noki.vpn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.noki.vpn.data.AppFilterMode
import com.noki.vpn.data.AppLanguage

@Composable
internal fun AdvancedFilterCard(
    mode: AppFilterMode,
    language: AppLanguage,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier,
    onModeChanged: (AppFilterMode) -> Unit,
    onConfigure: () -> Unit,
) {
    val panelBackdrop = rememberLayerBackdrop()
    val options = listOf(
        AppFilterMode.ALL_APPS to tr(language, "Все приложения", "All apps"),
        AppFilterMode.ONLY_SELECTED to tr(language, "Только отмеченные", "Selected only"),
        AppFilterMode.ALL_EXCEPT_SELECTED to tr(language, "Кроме отмеченных", "Except selected"),
    )
    AdvancedPanelSurface(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        exportedBackdrop = panelBackdrop,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AdvancedText(
                    text = tr(language, "Фильтр по приложениям", "App filter"),
                    fontSize = 17f,
                    lineHeight = 22f,
                    color = AdvancedTextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                AdvancedText(
                    text = tr(
                        language,
                        "Маршрутизация vpn трафика по приложениям",
                        "Route VPN traffic by applications",
                    ),
                    fontSize = 12f,
                    lineHeight = 14.4f,
                    color = AdvancedTextSecondary,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(NokiUiKitPolicy.advancedFilterOptionGapDp.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEach { (optionMode, label) ->
                    AdvancedFilterModeOption(
                        label = label,
                        selected = mode == optionMode,
                        onClick = { onModeChanged(optionMode) },
                    )
                }
            }
            AdvancedSmallButton(
                text = tr(language, "Выбор приложений", "Choose apps"),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NokiUiKitPolicy.advancedFilterConfigureButtonHeightDp.dp),
                fontSize = 13f,
                cornerRadius = 18.dp,
                backdrop = panelBackdrop,
                liveGlassEnabled = liveGlassEnabled,
                glassEnabled = true,
                onClick = onConfigure,
            )
        }
    }
}

@Composable
internal fun AdvancedYoutubeNoAdsRow(
    language: AppLanguage,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier,
) {
    AdvancedPanelSurface(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdvancedText(
                text = tr(language, "YouTube без рекламы", "YouTube without ads"),
                fontSize = 18f,
                lineHeight = 21.6f,
                color = AdvancedTextPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            NokiLiquidToggle(
                selected = enabled,
                onSelectedChange = onEnabledChanged,
                backdrop = backdrop,
                liveGlassEnabled = liveGlassEnabled,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}

@Composable
internal fun AdvancedFilterModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NokiUiKitPolicy.advancedFilterOptionRadiusDp.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NokiUiKitPolicy.advancedFilterOptionHeightDp.dp)
            .clip(shape)
            .background(AdvancedBgLighter.copy(alpha = 0.75f), shape)
            .border(BorderStroke(1.dp, AdvancedStroke.copy(alpha = 0.45f)), shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdvancedText(
            text = label,
            fontSize = 16f,
            lineHeight = 20f,
            color = AdvancedTextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        AdvancedRadioIndicator(selected = selected)
    }
}

@Composable
internal fun AdvancedRadioIndicator(selected: Boolean) {
    val shape = RoundedCornerShape(40.dp)
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
            .clip(shape)
            .background(AdvancedBgSoft.copy(alpha = 0.44f), shape)
            .border(
                BorderStroke(
                    1.dp,
                    if (selected) AdvancedAccentPrimary.copy(alpha = 0.72f) else AdvancedStroke.copy(alpha = 0.9f),
                ),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(14.dp)
                    .clip(shape)
                    .background(AdvancedAccentPrimary, shape),
            )
        }
    }
}

@Composable
internal fun AdvancedDomainRuleWideCard(
    title: String,
    count: Int,
    detail: String,
    language: AppLanguage,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier,
    onConfigure: () -> Unit,
) {
    val panelBackdrop = rememberLayerBackdrop()
    AdvancedPanelSurface(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        exportedBackdrop = panelBackdrop,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                AdvancedText(
                    text = title,
                    fontSize = 13f,
                    lineHeight = 15.6f,
                    color = AdvancedTextPrimary,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AdvancedText(
                        text = count.toString(),
                        fontSize = 13f,
                        lineHeight = 14f,
                        color = AdvancedTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier,
                    )
                    AdvancedText(
                        text = "${domainLabel(language, count)} $detail",
                        fontSize = 10f,
                        lineHeight = 12f,
                        color = AdvancedTextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AdvancedSmallButton(
                text = tr(language, "Настроить", "Set up"),
                modifier = Modifier
                    .width(90.dp)
                    .height(36.dp),
                backdrop = panelBackdrop,
                liveGlassEnabled = liveGlassEnabled,
                glassEnabled = true,
                onClick = onConfigure,
            )
        }
    }
}

internal fun domainLabel(language: AppLanguage, count: Int): String {
    if (language != AppLanguage.RU) {
        return if (count == 1) "domain" else "domains"
    }
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "домен"
        mod10 in 2..4 && mod100 !in 12..14 -> "домена"
        else -> "доменов"
    }
}
