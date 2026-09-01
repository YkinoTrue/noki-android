package com.noki.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.noki.vpn.AppUiState
import com.noki.vpn.MainViewModel
import com.noki.vpn.manualEndpointOptionsForCurrentCountry
import com.noki.vpn.data.EndpointSelectionMode
import kotlinx.coroutines.delay

private val AdvancedBgBase = Color(0xFF07111A)
internal val AdvancedBgLighter = Color(0xFF0D1B2A)
internal val AdvancedBgSoft = Color(0xFF132635)
internal val AdvancedTextPrimary = Color(0xFFF4FBFF)
internal val AdvancedTextSecondary = Color(0xFF9FB6C5)
internal val AdvancedAccentPrimary = Color(0xFF7AE7C7)
internal val AdvancedStroke = Color(0xFF29404E)
private val AdvancedNoFontPaddingTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun AdvancedSettingsScreen(
    state: AppUiState,
    viewModel: MainViewModel,
    sharedBackdrop: LayerBackdrop,
    cardBackdrop: LayerBackdrop? = null,
    liveGlassEnabled: Boolean = true,
    showBackground: Boolean = true,
    onAppFilterClicked: () -> Unit = {},
    onAlwaysOnRulesClicked: () -> Unit = {},
    onBypassRulesClicked: () -> Unit = {},
) {
    CompositionLocalProvider(LocalTextStyle provides AdvancedNoFontPaddingTextStyle) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showBackground) Modifier.background(AdvancedBgBase) else Modifier),
        ) {
            val backdrop = if (showBackground || liveGlassEnabled) sharedBackdrop else null
            val surfaceBackdrop = cardBackdrop ?: backdrop
            val metrics = nokiAdaptiveMetrics(maxWidth)
            val context = LocalContext.current
            val language = state.personalizationSettings.language
            val autoEndpointSelection = state.advancedSettings.endpointSelectionMode == EndpointSelectionMode.AUTO
            val alwaysOnCount = state.advancedSettings.alwaysOnDomains.size
            val bypassCount = state.advancedSettings.bypassDomains.size
            var showProtocolSheet by rememberSaveable { mutableStateOf(false) }
            var showProtocolNotice by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(showProtocolNotice) {
                if (showProtocolNotice) {
                    delay(2200)
                    showProtocolNotice = false
                }
            }
            LaunchedEffect(state.isAuthenticated, state.userProfile.selectedCountryCode) {
                if (state.isAuthenticated) {
                    viewModel.refreshEndpointOptions(context)
                }
            }

            if (showBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                ) {
                    HomeBackground(liveGlassEnabled = liveGlassEnabled)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 21.dp)
                        .padding(top = 58.dp, bottom = 150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                AdvancedText(
                    text = tr(language, "Расширенные настройки", "Advanced settings"),
                    fontSize = 24f,
                    lineHeight = 28.8f,
                    color = AdvancedTextPrimary,
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                )
                AdvancedText(
                    text = tr(
                        language,
                        "Протоколы, защита и правила маршрутизации",
                        "Protocols, protection and routing rules",
                    ),
                    fontSize = 12f,
                    lineHeight = 14.4f,
                    color = AdvancedTextSecondary,
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                AdvancedProtocolToggleCard(
                    autoEndpointSelection = autoEndpointSelection,
                    language = language,
                    backdrop = surfaceBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .height(NokiUiKitPolicy.advancedProtocolToggleCardHeightDp.dp),
                    onAutoEndpointSelectionChanged = { enabled ->
                        viewModel.toggleAutoEndpointSelection(enabled)
                        if (!enabled) {
                            viewModel.refreshEndpointOptions(context, force = true)
                        }
                    },
                )

                if (!autoEndpointSelection) {
                    Spacer(modifier = Modifier.height(25.dp))

                    AdvancedManualProtocolCard(
                        selectedEndpoint = protocolCardLabel(state, autoEndpointSelection),
                        language = language,
                        backdrop = surfaceBackdrop,
                        liveGlassEnabled = liveGlassEnabled,
                        modifier = Modifier
                            .widthIn(max = 370.dp)
                            .fillMaxWidth()
                            .height(NokiUiKitPolicy.advancedManualProtocolCardHeightDp.dp),
                        onChangeProtocol = {
                            if (!autoEndpointSelection) {
                                viewModel.refreshEndpointOptions(context, force = true)
                                showProtocolSheet = true
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                AdvancedFilterCard(
                    mode = state.filterMode,
                    language = language,
                    backdrop = surfaceBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .height(NokiUiKitPolicy.advancedFilterCardHeightDp.dp),
                    onModeChanged = viewModel::updateFilterMode,
                    onConfigure = onAppFilterClicked,
                )

                Spacer(modifier = Modifier.height(15.dp))

                AdvancedYoutubeNoAdsRow(
                    language = language,
                    enabled = state.advancedSettings.youtubeDirectDpiEnabled,
                    onEnabledChanged = viewModel::setYoutubeDirectDpiEnabled,
                    backdrop = surfaceBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .height(80.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                AdvancedText(
                    text = tr(language, "Умные правила для сайтов", "Smart site rules"),
                    fontSize = 18f,
                    lineHeight = 21.6f,
                    color = AdvancedTextPrimary,
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
                AdvancedText(
                    text = tr(
                        language,
                        "Домены для которых всегда включен или выключен VPN",
                        "Domains where VPN is always on or off",
                    ),
                    fontSize = 10.5f,
                    lineHeight = 12.6f,
                    color = AdvancedTextSecondary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(15.dp))

                Column(
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                AdvancedDomainRuleWideCard(
                    title = tr(language, "Всегда включать", "Always on"),
                    count = alwaysOnCount,
                    detail = tr(language, "всегда с VPN", "always with VPN"),
                    language = language,
                    backdrop = surfaceBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    onConfigure = onAlwaysOnRulesClicked,
                )

                AdvancedDomainRuleWideCard(
                    title = tr(language, "Всегда выключать", "Always off"),
                    count = bypassCount,
                    detail = tr(language, "всегда без VPN", "always without VPN"),
                    language = language,
                    backdrop = surfaceBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    onConfigure = onBypassRulesClicked,
                )
                }
                }

                if (showProtocolNotice) {
                    AdvancedProtocolNotice(
                        text = tr(
                            language,
                            "Протокол применится после переподключения VPN",
                            "Protocol applies after reconnecting VPN",
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = metrics.dp(150f))
                            .width(328.dp)
                            .height(58.dp),
                    )
                }

                if (showProtocolSheet) {
                    val manualOptions = manualEndpointOptionsForCurrentCountry(state)
                    AdvancedProtocolSheet(
                        selectedProtocol = state.advancedSettings.protocol,
                        endpointOptions = manualOptions,
                        selectedEndpointGroupKey = selectedEndpointGroupKey(state),
                        language = language,
                        backdrop = surfaceBackdrop,
                        liveGlassEnabled = liveGlassEnabled,
                        bottomNavigationClearance = metrics.dp(80f) + 10.dp,
                        onDismiss = { showProtocolSheet = false },
                        onProtocolSelected = { selected ->
                            showProtocolSheet = false
                            if (selected != null) {
                                viewModel.selectManualEndpoint(selected)
                                showProtocolNotice = true
                            }
                        },
                    )
                }
            }
        }
    }
}
