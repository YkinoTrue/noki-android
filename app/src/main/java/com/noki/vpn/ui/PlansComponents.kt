package com.noki.vpn.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.noki.vpn.AppUiState
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.BillingCycle
import com.noki.vpn.data.PlanCatalogPolicy
import com.noki.vpn.data.PlanCode
import com.noki.vpn.data.PlanSummary
import androidx.compose.runtime.getValue

@Composable
internal fun PlanBillingCycleSelector(
    cycle: BillingCycle,
    language: AppLanguage,
    activeColor: Color,
    scale: Float,
    modifier: Modifier = Modifier,
    onCycleChanged: (BillingCycle) -> Unit,
) {
    val cycles = listOf(BillingCycle.MONTHLY, BillingCycle.YEARLY)
    val selectedIndex = cycles.indexOf(cycle).coerceAtLeast(0)
    val interactionSource = remember { MutableInteractionSource() }
    val containerWidth = settingsDp(126f, scale)
    val containerHeight = settingsDp(32f, scale)
    val edgePadding = settingsDp(3f, scale)
    val activeWidth = (containerWidth - edgePadding * 2f) / 2f
    val activeHeight = containerHeight - edgePadding * 2f
    val leftHitWidth = activeWidth
    val rightHitWidth = containerWidth - activeWidth
    val activeOffset by animateDpAsState(
        targetValue = if (selectedIndex == 0) edgePadding else containerWidth - activeWidth - edgePadding,
        animationSpec = spring(),
        label = "planBillingCycleOffset",
    )
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .width(containerWidth)
            .height(containerHeight)
            .background(Color(0xFF07111A).copy(alpha = 0.34f), shape)
            .border(BorderStroke(1.dp, SettingsStroke.copy(alpha = 0.62f)), shape)
            .clip(shape),
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = activeOffset.roundToPx(),
                        y = edgePadding.roundToPx(),
                    )
                }
                .width(activeWidth)
                .height(activeHeight)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            activeColor.copy(alpha = 0.22f),
                        ),
                    ),
                    shape,
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)), shape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = edgePadding)
                .width(activeWidth)
                .height(containerHeight),
            contentAlignment = Alignment.Center,
        ) {
            SettingsText(
                text = tr(language, "месяц", "month"),
                fontSize = 11f,
                lineHeight = 13f,
                letterSpacing = 0f,
                color = if (selectedIndex == 0) SettingsTextPrimary else SettingsTextSecondary,
                scale = scale,
                textAlign = TextAlign.Center,
                fontWeight = if (selectedIndex == 0) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = edgePadding)
                .width(activeWidth)
                .height(containerHeight),
            contentAlignment = Alignment.Center,
        ) {
            SettingsText(
                text = tr(language, "год", "year"),
                fontSize = 11f,
                lineHeight = 13f,
                letterSpacing = 0f,
                color = if (selectedIndex == 1) SettingsTextPrimary else SettingsTextSecondary,
                scale = scale,
                textAlign = TextAlign.Center,
                fontWeight = if (selectedIndex == 1) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(leftHitWidth)
                .height(containerHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCycleChanged(BillingCycle.MONTHLY) },
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(rightHitWidth)
                .height(containerHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCycleChanged(BillingCycle.YEARLY) },
                ),
        )
    }
}

@Composable
internal fun CheckoutBillingCycleSelector(
    cycle: BillingCycle,
    language: AppLanguage,
    activeColor: Color,
    scale: Float,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier = Modifier,
    onCycleChanged: (BillingCycle) -> Unit,
) {
    val cycles = listOf(BillingCycle.MONTHLY, BillingCycle.YEARLY)
    val selectedIndex = cycles.indexOf(cycle).coerceAtLeast(0)
    val interactionSource = remember { MutableInteractionSource() }
    val containerHeight = settingsDp(52f, scale)
    val edgePadding = settingsDp(3f, scale)
    val shape = RoundedCornerShape(percent = 50)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .nokiGlassSurface(
                shape = shape,
                backdrop = backdrop,
                liveGlassEnabled = liveGlassEnabled,
                scale = scale,
                elevationDp = 2f,
                shadowAlpha = 0.12f,
                highlightAlpha = 0.18f,
                blurRadiusDp = 5f,
                lensRadiusDp = 4f,
                lensRefractionDp = 4f,
                innerShadowAlpha = 0.12f,
                surfaceColor = Color(0xFF07111A).copy(alpha = 0.28f),
            )
            .clip(shape),
    ) {
        val activeWidth = (maxWidth - edgePadding * 2f) / 2f
        val activeHeight = containerHeight - edgePadding * 2f
        val activeOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) {
                edgePadding
            } else {
                maxWidth - activeWidth - edgePadding
            },
            animationSpec = spring(),
            label = "checkoutBillingCycleOffset",
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = activeOffset.roundToPx(),
                        y = edgePadding.roundToPx(),
                    )
                }
                .width(activeWidth)
                .height(activeHeight)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            activeColor.copy(alpha = 0.22f),
                        ),
                    ),
                    shape,
                )
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
                    shape,
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(edgePadding),
        ) {
            listOf(
                BillingCycle.MONTHLY to tr(language, "месяц", "month"),
                BillingCycle.YEARLY to tr(language, "год", "year"),
            ).forEachIndexed { index, (option, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onCycleChanged(option) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    SettingsText(
                        text = label,
                        fontSize = 14f,
                        lineHeight = 17f,
                        letterSpacing = 0f,
                        color = if (selectedIndex == index) SettingsTextPrimary else SettingsTextSecondary,
                        scale = scale,
                        textAlign = TextAlign.Center,
                        fontWeight = if (selectedIndex == index) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlanTariffCard(
    plan: PlanSummary,
    cycle: BillingCycle,
    language: AppLanguage,
    scale: Float,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier,
    onCycleChanged: (BillingCycle) -> Unit,
) {
    val shape = RoundedCornerShape(settingsDp(25f, scale))
    val planColor = settingsPlanColor(plan) ?: SettingsAccentPrimary
    Column(
        modifier = modifier
            .nokiSettingsPanelGlassSurface(
                shape = shape,
                backdrop = backdrop,
                liveGlassEnabled = liveGlassEnabled,
                scale = scale,
                innerShadowAlpha = 0f,
                surfaceColor = SettingsBgLighter.copy(alpha = 0.2f),
            )
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to planColor.copy(alpha = 0.63f),
                            0.18f to planColor.copy(alpha = 0.28f),
                            0.34f to planColor.copy(alpha = 0.12f),
                            0.50f to planColor.copy(alpha = 0.04f),
                            0.60f to Color.Transparent,
                        ),
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                )
            }
            .padding(
                start = settingsDp(20f, scale),
                top = settingsDp(35f, scale),
                end = settingsDp(20f, scale),
                bottom = settingsDp(20f, scale),
            ),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(settingsDp(40f, scale)),
            horizontalAlignment = Alignment.Start,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(settingsDp(15f, scale))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(settingsDp(12f, scale)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsText(
                        text = settingsCleanPlanTitle(plan.title),
                        fontSize = 24f,
                        lineHeight = 29f,
                        letterSpacing = 0.24f,
                        color = SettingsTextPrimary,
                        scale = scale,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    PlanBillingCycleSelector(
                        cycle = cycle,
                        language = language,
                        activeColor = planColor,
                        scale = scale,
                        onCycleChanged = onCycleChanged,
                    )
                }
                PlanPriceLine(
                    plan = plan,
                    cycle = cycle,
                    language = language,
                    scale = scale,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(settingsDp(20f, scale)),
            ) {
                val features = plan.features.ifEmpty { listOf(plan.trafficLabel) }
                features.forEach { feature ->
                    PlanFeatureRow(
                        text = feature,
                        scale = scale,
                    )
                }
            }
        }

        plan.headline?.trim()?.takeIf { it.isNotBlank() }?.let { headline ->
            SettingsText(
                text = headline,
                fontSize = 14f,
                lineHeight = 18f,
                letterSpacing = 0.14f,
                color = planColor,
                scale = scale,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PlanPriceLine(
    plan: PlanSummary,
    cycle: BillingCycle,
    language: AppLanguage,
    scale: Float,
) {
    val label = PlanCatalogPolicy.priceLabel(plan, cycle, language)
    val primaryModifier = if (label.secondary == null) Modifier.fillMaxWidth() else Modifier
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(settingsDp(10f, scale)),
        verticalAlignment = Alignment.Bottom,
    ) {
        SettingsText(
            text = label.primary,
            fontSize = 30f,
            lineHeight = 34f,
            letterSpacing = 0.18f,
            color = SettingsTextPrimary,
            scale = scale,
            textAlign = TextAlign.Start,
            modifier = primaryModifier,
        )
        label.secondary?.let { secondary ->
            SettingsText(
                text = secondary,
                fontSize = 16f,
                lineHeight = 20f,
                letterSpacing = 0.12f,
                color = SettingsTextSecondary,
                scale = scale,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun PlanFeatureRow(
    text: String,
    scale: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(settingsDp(10f, scale)),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = settingsDp(7f, scale))
                .width(settingsDp(5f, scale))
                .height(settingsDp(5f, scale))
                .clip(CircleShape)
                .background(SettingsTextPrimary),
        )
        SettingsText(
            text = text,
            fontSize = 16f,
            lineHeight = 21f,
            letterSpacing = 0.14f,
            color = SettingsTextPrimary,
            scale = scale,
            textAlign = TextAlign.Start,
            maxLines = 3,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun PlanIndicatorDots(
    count: Int,
    selectedIndex: Int,
    activeColor: Color,
    scale: Float,
) {
    Row(
        modifier = Modifier
            .width(settingsDp(70f, scale))
            .height(settingsDp(10f, scale)),
        horizontalArrangement = Arrangement.spacedBy(settingsDp(10f, scale), Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count.coerceAtLeast(1)) { index ->
            Box(
                modifier = Modifier
                    .width(settingsDp(10f, scale))
                    .height(settingsDp(10f, scale))
                    .clip(CircleShape)
                    .background(
                        if (index == selectedIndex) {
                            activeColor
                        } else {
                            SettingsStroke.copy(alpha = 0.88f)
                        },
                    ),
            )
        }
    }
}

@Composable
internal fun PlanActionButton(
    text: String,
    scale: Float,
    modifier: Modifier,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(settingsDp(NokiUiKitPolicy.actionCornerRadiusDp, scale))
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(settingsDp(NokiUiKitPolicy.planActionButtonHeightDp, scale))
            .nokiTintedActionSurface(
                shape = shape,
                color = HomeAccentPrimary,
                backdrop = backdrop,
                liveGlassEnabled = liveGlassEnabled,
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SettingsText(
            text = text,
            fontSize = 15f,
            lineHeight = 18f,
            letterSpacing = 0.12f,
            color = SettingsTextPrimary,
            scale = scale,
            fontWeight = FontWeight.Medium,
            modifier = Modifier,
        )
    }
}

@Composable
internal fun PlanCheckoutScreen(
    plan: PlanSummary,
    currentPlanTitle: String,
    cycle: BillingCycle,
    language: AppLanguage,
    promoCode: String,
    onPromoCodeChanged: (String) -> Unit,
    onCycleChanged: (BillingCycle) -> Unit,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    scale: Float,
    modifier: Modifier,
) {
    val planColor = settingsPlanColor(plan) ?: SettingsAccentPrimary
    val inputShape = RoundedCornerShape(settingsDp(NokiUiKitPolicy.actionCornerRadiusDp, scale))
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(settingsDp(96f, scale))
                .padding(horizontal = settingsDp(18f, scale)),
            horizontalArrangement = Arrangement.spacedBy(settingsDp(18f, scale)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SettingsText(
                    text = tr(language, "Текущий тариф", "Current plan"),
                    fontSize = 12f,
                    lineHeight = 15f,
                    letterSpacing = 0.12f,
                    color = SettingsTextSecondary,
                    scale = scale,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsText(
                    text = settingsCleanPlanTitle(currentPlanTitle),
                    fontSize = 18f,
                    lineHeight = 22f,
                    letterSpacing = 0.18f,
                    color = SettingsTextPrimary,
                    scale = scale,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = SettingsTextSecondary,
                modifier = Modifier.size(settingsDp(18f, scale)),
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SettingsText(
                    text = tr(language, "Новый тариф", "New plan"),
                    fontSize = 12f,
                    lineHeight = 15f,
                    letterSpacing = 0.12f,
                    color = SettingsTextSecondary,
                    scale = scale,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsText(
                    text = settingsCleanPlanTitle(plan.title),
                    fontSize = 18f,
                    lineHeight = 22f,
                    letterSpacing = 0.18f,
                    color = SettingsTextPrimary,
                    scale = scale,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(modifier = Modifier.height(settingsDp(48f, scale)))
        SettingsText(
            text = tr(language, "К оплате", "Total"),
            fontSize = 13f,
            lineHeight = 16f,
            letterSpacing = 0.12f,
            color = SettingsTextSecondary,
            scale = scale,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsText(
            text = PlanCatalogPolicy.checkoutTotalLabel(plan, cycle, language),
            fontSize = 48f,
            lineHeight = 54f,
            letterSpacing = 0.18f,
            color = SettingsTextPrimary,
            scale = scale,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(settingsDp(32f, scale)))
        CheckoutBillingCycleSelector(
            cycle = cycle,
            language = language,
            activeColor = planColor,
            scale = scale,
            backdrop = backdrop,
            liveGlassEnabled = liveGlassEnabled,
            onCycleChanged = onCycleChanged,
        )
        Spacer(modifier = Modifier.height(settingsDp(40f, scale)))
        SettingsText(
            text = tr(
                language,
                "После оплаты тариф изменится сразу. Оставшийся срок не складывается с новым, текущий тариф будет заменён.",
                "The plan changes immediately after payment. Remaining time is not added to the new term, the current plan is replaced.",
            ),
            fontSize = 12f,
            lineHeight = 17f,
            letterSpacing = 0.12f,
            color = SettingsTextSecondary,
            scale = scale,
            textAlign = TextAlign.Start,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = settingsDp(16f, scale)),
        )
        Spacer(modifier = Modifier.height(settingsDp(24f, scale)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(settingsDp(48f, scale))
                .nokiGlassSurface(
                    shape = inputShape,
                    backdrop = backdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    scale = scale,
                    elevationDp = 2f,
                    shadowAlpha = 0.12f,
                    highlightAlpha = 0.18f,
                    blurRadiusDp = 5f,
                    lensRadiusDp = 4f,
                    lensRefractionDp = 4f,
                    innerShadowAlpha = 0.12f,
                    surfaceColor = Color(0xFF07111A).copy(alpha = 0.28f),
                ),
        ) {
            SettingsCompactInputField(
                value = promoCode,
                onValueChange = onPromoCodeChanged,
                placeholder = tr(language, "Введите промокод", "Enter promo code"),
                scale = scale,
                backgroundColor = Color.Transparent,
            )
        }
        Spacer(modifier = Modifier.height(settingsDp(24f, scale)))
        PlanActionButton(
            text = tr(language, "Оплатить", "Pay"),
            scale = scale,
            modifier = Modifier.fillMaxWidth(),
            backdrop = backdrop,
            liveGlassEnabled = liveGlassEnabled,
            onClick = {},
        )
    }
}

internal fun checkoutPlanForCycle(
    plans: List<PlanSummary>,
    selectedCode: String,
    cycle: BillingCycle,
): PlanSummary? {
    val selected = plans.firstOrNull { it.code.equals(selectedCode, ignoreCase = true) } ?: return null
    val selectedKnownCode = knownPlanCodeFromBackend(selected.code)
        ?: knownPlanCodeFromBackend(selected.tier)
    return PlanCatalogPolicy.visiblePlans(plans, cycle).firstOrNull { candidate ->
        if (selectedKnownCode != null) {
            knownPlanCodeFromBackend(candidate.code) == selectedKnownCode ||
                knownPlanCodeFromBackend(candidate.tier) == selectedKnownCode
        } else {
            candidate.tier.equals(selected.tier, ignoreCase = true)
        }
    } ?: selected
}

internal fun isCurrentPlan(
    plan: PlanSummary,
    state: AppUiState,
): Boolean {
    val rawCode = state.userProfile.selectedPlanCodeRaw.trim()
    if (rawCode.isNotBlank()) {
        val currentKnownCode = knownPlanCodeFromBackend(rawCode)
        val planKnownCode = knownPlanCodeFromBackend(plan.code)
            ?: knownPlanCodeFromBackend(plan.tier)
        if (currentKnownCode != null && planKnownCode != null) {
            return currentKnownCode == planKnownCode
        }
        val currentTier = rawCode.substringBefore('-').substringBefore('_')
        return plan.code.substringBefore('-').substringBefore('_').equals(currentTier, ignoreCase = true) ||
            plan.tier.substringBefore('-').substringBefore('_').equals(currentTier, ignoreCase = true)
    }
    return knownPlanCodeFromBackend(plan.code) == state.userProfile.selectedPlanCode ||
        knownPlanCodeFromBackend(plan.tier) == state.userProfile.selectedPlanCode
}

internal fun knownPlanCodeFromBackend(code: String?): PlanCode? {
    val normalized = code?.lowercase() ?: return null
    return when {
        normalized.startsWith("premium") -> PlanCode.PREMIUM
        normalized.startsWith("pro") -> PlanCode.PRO
        normalized.startsWith("plus") -> PlanCode.PLUS
        normalized.startsWith("standard") -> PlanCode.PLUS
        normalized.startsWith("free") -> PlanCode.FREE
        else -> null
    }
}

@Composable
internal fun PlansEmptyCard(
    language: AppLanguage,
    scale: Float,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = settingsDp(260f, scale))
            .clip(RoundedCornerShape(settingsDp(25f, scale)))
            .background(SettingsBgLighter, RoundedCornerShape(settingsDp(25f, scale)))
            .border(BorderStroke(1.dp, SettingsStroke.copy(alpha = 0.9f)), RoundedCornerShape(settingsDp(25f, scale)))
            .padding(settingsDp(24f, scale)),
        verticalArrangement = Arrangement.spacedBy(settingsDp(8f, scale)),
    ) {
        SettingsText(
            text = tr(language, "Тарифы недоступны", "Plans unavailable"),
            fontSize = 18f,
            lineHeight = 22f,
            letterSpacing = 0f,
            color = SettingsTextPrimary,
            scale = scale,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsText(
            text = tr(language, "Обновите данные позже.", "Refresh data later."),
            fontSize = 12f,
            lineHeight = 16f,
            letterSpacing = 0f,
            color = SettingsTextSecondary,
            scale = scale,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
