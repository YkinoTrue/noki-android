package com.noki.vpn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.noki.vpn.AppUiState
import com.noki.vpn.MainViewModel
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.DomainRulePolicy

enum class SiteRulesMode {
    ALWAYS_ON,
    BYPASS,
}

internal fun siteRuleValidationError(
    value: String,
    domains: List<String>,
    editingDomain: String?,
    language: AppLanguage,
): String? {
    if (value.isBlank()) {
        return tr(language, "Введите домен", "Enter a domain")
    }
    val normalized = DomainRulePolicy.normalize(value)
        ?: return tr(language, "Введите корректный домен", "Enter a valid domain")
    val editingNormalized = editingDomain?.let(DomainRulePolicy::normalize)
    val duplicate = normalized != editingNormalized &&
        domains.any { DomainRulePolicy.normalize(it) == normalized }
    return if (duplicate) {
        tr(language, "Этот домен уже добавлен в список", "This domain is already in the list")
    } else {
        null
    }
}

private val SiteRulesBgBase = Color(0xFF07111A)
private val SiteRulesBgSoft = Color(0xFF132635)
private val SiteRulesTextPrimary = Color(0xFFF4FBFF)
private val SiteRulesTextSecondary = Color(0xFF9FB6C5)
private val SiteRulesTextMuted = Color(0xFF6E8797)
private val SiteRulesAccent = Color(0xFF7AE7C7)
private val SiteRulesDanger = Color(0xFFFF646B)
private val SiteRulesStroke = Color(0xFF29404E)
private val SiteRulesNoFontPaddingTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun SiteRulesScreen(
    state: AppUiState,
    viewModel: MainViewModel,
    mode: SiteRulesMode,
    sharedBackdrop: LayerBackdrop? = null,
    liveGlassEnabled: Boolean = true,
    showBackground: Boolean = true,
) {
    val language = state.personalizationSettings.language
    val domains = when (mode) {
        SiteRulesMode.ALWAYS_ON -> state.advancedSettings.alwaysOnDomains
        SiteRulesMode.BYPASS -> state.advancedSettings.bypassDomains
    }
    var input by rememberSaveable { mutableStateOf("") }
    var editingDomain by rememberSaveable { mutableStateOf<String?>(null) }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(mode) {
        input = ""
        editingDomain = null
        inputError = null
    }

    CompositionLocalProvider(LocalTextStyle provides SiteRulesNoFontPaddingTextStyle) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showBackground) Modifier.background(SiteRulesBgBase) else Modifier)
                .statusBarsPadding(),
        ) {
            val metrics = nokiAdaptiveMetrics(maxWidth)
            val headerTop = 58.dp
            val headerTitleLineHeight = 28.8.dp
            val headerGap = 3.dp
            val headerSubtitleLineHeight = 14.4.dp
            val headerToContentGap = 20.dp
            val contentTop = headerTop + headerTitleLineHeight + headerGap + headerSubtitleLineHeight + headerToContentGap
            val contentHeight = (maxHeight - contentTop - 110.dp).coerceAtLeast(0.dp)

            if (showBackground) {
                HomeBackground(liveGlassEnabled = liveGlassEnabled)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                SiteRulesText(
                    text = titleForMode(language, mode),
                    fontSize = 24f,
                    lineHeight = 28.8f,
                    color = SiteRulesTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .offset(x = metrics.contentStart + 18.dp, y = headerTop)
                        .width(284.dp),
                )
                SiteRulesText(
                    text = subtitleForMode(language, mode),
                    fontSize = 12f,
                    lineHeight = 14.4f,
                    color = SiteRulesTextSecondary,
                    maxLines = 2,
                    modifier = Modifier
                        .offset(x = metrics.contentStart + 18.dp, y = headerTop + headerTitleLineHeight + headerGap)
                        .width(284.dp),
                )

                Column(
                    modifier = Modifier
                        .offset(x = metrics.contentStart, y = contentTop)
                        .width(metrics.contentWidth)
                        .height(contentHeight),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SiteRulesEditorCard(
                        value = input,
                        editing = editingDomain != null,
                        error = inputError,
                        language = language,
                        backdrop = sharedBackdrop,
                        liveGlassEnabled = liveGlassEnabled,
                        onValueChange = {
                            input = it
                            inputError = null
                        },
                        onCancel = {
                            input = ""
                            editingDomain = null
                            inputError = null
                        },
                        onSubmit = {
                            val oldDomain = editingDomain
                            val error = siteRuleValidationError(
                                value = input,
                                domains = domains,
                                editingDomain = oldDomain,
                                language = language,
                            )
                            if (error != null) {
                                inputError = error
                            } else {
                                if (oldDomain == null) {
                                    when (mode) {
                                        SiteRulesMode.ALWAYS_ON -> viewModel.addAlwaysOnDomain(input)
                                        SiteRulesMode.BYPASS -> viewModel.addBypassDomain(input)
                                    }
                                } else {
                                    when (mode) {
                                        SiteRulesMode.ALWAYS_ON -> viewModel.updateAlwaysOnDomain(oldDomain, input)
                                        SiteRulesMode.BYPASS -> viewModel.updateBypassDomain(oldDomain, input)
                                    }
                                }
                                input = ""
                                editingDomain = null
                                inputError = null
                            }
                        },
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        if (domains.isEmpty()) {
                            item(contentType = "site-rule-empty") {
                                SiteRulesEmptyCard(
                                    language = language,
                                    backdrop = sharedBackdrop,
                                    liveGlassEnabled = liveGlassEnabled,
                                )
                            }
                        }
                        items(
                            items = domains,
                            key = { it },
                            contentType = { "site-rule-row" },
                        ) { domain ->
                            SiteRuleRow(
                                domain = domain,
                                language = language,
                                backdrop = sharedBackdrop,
                                liveGlassEnabled = liveGlassEnabled,
                                onEdit = {
                                    input = domain
                                    editingDomain = domain
                                    inputError = null
                                },
                                onDelete = {
                                    when (mode) {
                                        SiteRulesMode.ALWAYS_ON -> viewModel.removeAlwaysOnDomain(domain)
                                        SiteRulesMode.BYPASS -> viewModel.removeBypassDomain(domain)
                                    }
                                    if (editingDomain == domain) {
                                        input = ""
                                        editingDomain = null
                                        inputError = null
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteRulesEditorCard(
    value: String,
    editing: Boolean,
    error: String?,
    language: AppLanguage,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    SiteRulesCard(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when {
                    error != null && editing -> 190.dp
                    error != null -> 176.dp
                    editing -> 164.dp
                    else -> 150.dp
                },
            ),
    ) { panelBackdrop ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SiteRulesText(
                text = if (editing) {
                    tr(language, "Редактировать домен", "Edit domain")
                } else {
                    tr(language, "Добавить домен", "Add domain")
                },
                fontSize = 16f,
                lineHeight = 19.2f,
                color = SiteRulesTextPrimary,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
            )
            SiteRulesInput(
                value = value,
                onValueChange = onValueChange,
                placeholder = tr(language, "example.com", "example.com"),
            )
            error?.let {
                SiteRulesText(
                    text = it,
                    fontSize = 11.5f,
                    lineHeight = 13.8f,
                    color = SiteRulesDanger,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (editing) {
                    AdvancedSmallButton(
                        text = tr(language, "Отмена", "Cancel"),
                        modifier = Modifier
                            .weight(1f)
                            .height(NokiUiKitPolicy.advancedFilterConfigureButtonHeightDp.dp),
                        fontSize = 13f,
                        cornerRadius = 18.dp,
                        backdrop = panelBackdrop,
                        liveGlassEnabled = liveGlassEnabled,
                        glassEnabled = true,
                        onClick = onCancel,
                    )
                }
                AdvancedSmallButton(
                    text = tr(language, "Сохранить", "Save"),
                    modifier = Modifier
                        .weight(1f)
                        .height(NokiUiKitPolicy.advancedFilterConfigureButtonHeightDp.dp),
                    fontSize = 13f,
                    cornerRadius = 18.dp,
                    backdrop = panelBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    glassEnabled = true,
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun SiteRuleRow(
    domain: String,
    language: AppLanguage,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SiteRulesCard(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(151.dp),
            ) {
                SiteRulesText(
                    text = if (domain == DomainRulePolicy.RUSSIAN_RESOURCES_RULE) {
                        tr(language, "Российские ресурсы", "Russian resources")
                    } else {
                        domain
                    },
                    fontSize = 15f,
                    lineHeight = 18f,
                    color = SiteRulesTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SiteRulesButton(
                    text = tr(language, "Изм.", "Edit"),
                    modifier = Modifier
                        .width(46.dp)
                        .height(40.dp),
                    onClick = onEdit,
                )
                SiteRulesButton(
                    text = tr(language, "Удалить", "Delete"),
                    danger = true,
                    modifier = Modifier
                        .width(65.dp)
                        .height(40.dp),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun SiteRulesEmptyCard(
    language: AppLanguage,
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
) {
    SiteRulesCard(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            SiteRulesText(
                text = tr(language, "Правил пока нет", "No rules yet"),
                fontSize = 14f,
                lineHeight = 16.8f,
                color = SiteRulesTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
            SiteRulesText(
                text = tr(language, "Добавьте домен выше, чтобы он попал в список.", "Add a domain above to create a rule."),
                fontSize = 10.5f,
                lineHeight = 12.6f,
                color = SiteRulesTextSecondary,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SiteRulesInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val shape = RoundedCornerShape(15.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(SiteRulesAccent),
        textStyle = TextStyle(
            color = SiteRulesTextPrimary,
            fontFamily = ManropeFontFamily,
            fontSize = 12.sp,
            lineHeight = 14.5.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(shape)
            .background(SiteRulesBgSoft, shape)
            .padding(horizontal = 14.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    SiteRulesText(
                        text = placeholder,
                        fontSize = 14f,
                        lineHeight = 16.8f,
                        color = SiteRulesTextMuted,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun SiteRulesCard(
    backdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    modifier: Modifier,
    content: @Composable (LayerBackdrop) -> Unit,
) {
    val panelBackdrop = rememberLayerBackdrop()
    AdvancedPanelSurface(
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        exportedBackdrop = panelBackdrop,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            content(panelBackdrop)
        }
    }
}

@Composable
private fun SiteRulesButton(
    text: String,
    modifier: Modifier,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 520f,
        ),
        label = "SiteRulesButtonScale",
    )
    val shape = RoundedCornerShape(14.dp)
    val color = when {
        danger -> SiteRulesDanger
        else -> SiteRulesBgSoft
    }
    val backgroundAlpha = if (danger) 0.16f else 1f
    val textColor = if (danger) SiteRulesDanger else SiteRulesTextPrimary
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(color.copy(alpha = backgroundAlpha), shape)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.55f)), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SiteRulesText(
            text = text,
            fontSize = 13f,
            lineHeight = 15.6f,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SiteRulesText(
    text: String,
    fontSize: Float,
    lineHeight: Float,
    color: Color,
    modifier: Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val contentScale = nokiAdaptiveMetrics(configuration.screenWidthDp.dp).contentScale
    Text(
        text = text,
        color = color,
        fontFamily = ManropeFontFamily,
        fontWeight = fontWeight,
        fontSize = (fontSize * contentScale / density.fontScale).sp,
        lineHeight = (lineHeight * contentScale / density.fontScale).sp,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        modifier = modifier,
    )
}

private fun titleForMode(language: AppLanguage, mode: SiteRulesMode): String {
    return when (mode) {
        SiteRulesMode.ALWAYS_ON -> tr(language, "Всегда включать", "Always on")
        SiteRulesMode.BYPASS -> tr(language, "Всегда выключать", "Always off")
    }
}

private fun subtitleForMode(language: AppLanguage, mode: SiteRulesMode): String {
    return when (mode) {
        SiteRulesMode.ALWAYS_ON -> tr(language, "Эти домены всегда будут идти через VPN", "These domains always use VPN")
        SiteRulesMode.BYPASS -> tr(language, "Эти домены всегда будут идти без VPN", "These domains always bypass VPN")
    }
}
