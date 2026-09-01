package com.noki.vpn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.ServerLocation
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
@Composable
internal fun HomeServerDropdownOverlay(
    modifier: Modifier,
    visible: Boolean,
    scale: Float,
    language: AppLanguage,
    locations: List<ServerLocation>,
    backdrop: Backdrop?,
    serverRowsBackdrop: LayerBackdrop?,
    liveGlassEnabled: Boolean,
    animateRows: Boolean,
    onCollapse: () -> Unit,
    onLocationSelected: (String) -> Unit,
) {
    val menuLocations = remember(locations) { serverMenuLocations(locations) }
    NokiGlassSheetOverlay(
        modifier = modifier,
        visible = visible,
        scale = scale,
        backdrop = backdrop,
        liveGlassEnabled = liveGlassEnabled,
        onCollapse = onCollapse,
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxWidth()
                .padding(horizontal = designDp(NokiUiKitPolicy.homeServerItemHorizontalPaddingDp, scale))
                .padding(top = designDp(HOME_SERVER_DROPDOWN_FIRST_ITEM_TOP_DP, scale))
                .padding(bottom = designDp(NokiUiKitPolicy.homeServerDropdownListBottomReserveDp, scale)),
            verticalArrangement = Arrangement.spacedBy(designDp(NokiUiKitPolicy.homeServerItemGapDp, scale)),
            contentPadding = PaddingValues(bottom = designDp(0f, scale)),
        ) {
            items(
                items = menuLocations,
                key = { it.key },
                contentType = { "home-server-row" },
            ) { entry ->
                HomeServerMenuItem(
                    location = entry.location,
                    language = language,
                    scale = scale,
                    backdrop = serverRowsBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    animateOnlineIndicator = animateRows,
                    onClick = { onLocationSelected(entry.location.code) },
                )
            }
        }
    }
}

@Composable
internal fun NokiGlassSheetOverlay(
    modifier: Modifier,
    visible: Boolean,
    scale: Float,
    backdrop: Backdrop?,
    liveGlassEnabled: Boolean,
    onCollapse: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val sheetDismissThresholdPx = with(density) { 96.dp.toPx() }
    val dismissDirection = -1f
    val firstServerItemTopPx = with(density) {
        designDp(HOME_SERVER_DROPDOWN_FIRST_ITEM_TOP_DP, scale).toPx()
    }
    val sheetDragOffset = remember { Animatable(0f) }
    val sheetDragScope = rememberCoroutineScope()
    var sheetHeightPx by remember(visible) { mutableFloatStateOf(0f) }
    val passthroughBlockerInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(visible, sheetDragOffset) {
        if (visible) {
            sheetDragOffset.snapTo(0f)
        }
    }

    fun animateSheetBack() {
        sheetDragScope.launch {
            sheetDragOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            )
        }
    }

    fun sheetExitDistancePx(sheetHeightPx: Float): Float {
        return sheetHeightPx.coerceAtLeast(sheetDismissThresholdPx * 2f)
    }

    fun handleFadeEndDistancePx(sheetHeightPx: Float): Float {
        val exitDistancePx = sheetExitDistancePx(sheetHeightPx)
        val firstItemBoundaryPx = firstServerItemTopPx.coerceIn(0f, exitDistancePx)
        return (exitDistancePx - firstItemBoundaryPx)
            .coerceAtLeast(exitDistancePx * HOME_SERVER_DROPDOWN_HANDLE_FADE_START_FRACTION)
    }

    fun handleAlphaForSheetOffset(offsetPx: Float): Float {
        val fadeEndDistancePx = handleFadeEndDistancePx(sheetHeightPx)
        val fadeStartDistancePx = fadeEndDistancePx * HOME_SERVER_DROPDOWN_HANDLE_FADE_START_FRACTION
        val closeDistancePx = (offsetPx * dismissDirection).coerceAtLeast(0f)
        if (closeDistancePx <= fadeStartDistancePx) {
            return HOME_SERVER_DROPDOWN_HANDLE_VISIBLE_ALPHA
        }
        val fadeRangePx = (fadeEndDistancePx - fadeStartDistancePx).coerceAtLeast(1f)
        val fadeProgress = ((closeDistancePx - fadeStartDistancePx) / fadeRangePx)
            .coerceIn(0f, 1f)
        return HOME_SERVER_DROPDOWN_HANDLE_VISIBLE_ALPHA * (1f - fadeProgress)
    }

    fun handleFadeEndMillis(sheetHeightPx: Float): Int {
        val exitDistancePx = sheetExitDistancePx(sheetHeightPx)
        val fadeEndDistancePx = handleFadeEndDistancePx(sheetHeightPx)
        val fadeEndFraction = (fadeEndDistancePx / exitDistancePx).coerceIn(
            HOME_SERVER_DROPDOWN_HANDLE_FADE_START_FRACTION,
            1f,
        )
        return (HOME_SERVER_DROPDOWN_EXIT_DURATION_MS * fadeEndFraction)
            .roundToInt()
            .coerceAtLeast((HOME_SERVER_DROPDOWN_EXIT_DURATION_MS *
                HOME_SERVER_DROPDOWN_HANDLE_FADE_START_FRACTION).roundToInt() + 1)
    }

    fun settleSheetDrag() {
        sheetDragScope.launch {
            if (sheetDragOffset.value * dismissDirection >= sheetDismissThresholdPx) {
                sheetDragOffset.animateTo(
                    targetValue = dismissDirection * sheetExitDistancePx(sheetHeightPx),
                    animationSpec = tween(
                        durationMillis = HOME_SERVER_DROPDOWN_EXIT_DURATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
                onCollapse()
            } else {
                sheetDragOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(
                durationMillis = HOME_SERVER_DROPDOWN_EXIT_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = HOME_SERVER_DROPDOWN_EXIT_FADE_DURATION_MS,
                delayMillis = HOME_SERVER_DROPDOWN_EXIT_FADE_DELAY_MS,
                easing = FastOutSlowInEasing,
            ),
        ) + slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(
                durationMillis = HOME_SERVER_DROPDOWN_EXIT_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        ),
    ) {
        val transitionHandleAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.PostExit) {
                    keyframes {
                        durationMillis = HOME_SERVER_DROPDOWN_EXIT_DURATION_MS
                        val fadeStartMillis = (HOME_SERVER_DROPDOWN_EXIT_DURATION_MS *
                            HOME_SERVER_DROPDOWN_HANDLE_FADE_START_FRACTION).roundToInt()
                        val fadeEndMillis = handleFadeEndMillis(sheetHeightPx)
                            .coerceAtMost(HOME_SERVER_DROPDOWN_EXIT_DURATION_MS)
                        HOME_SERVER_DROPDOWN_HANDLE_VISIBLE_ALPHA at 0
                        HOME_SERVER_DROPDOWN_HANDLE_VISIBLE_ALPHA at fadeStartMillis
                        0f at fadeEndMillis
                        0f at HOME_SERVER_DROPDOWN_EXIT_DURATION_MS
                    }
                } else {
                    tween(durationMillis = 180, easing = FastOutSlowInEasing)
                }
            },
            label = "homeServerDropdownHandleAlpha",
        ) { state ->
            if (state == EnterExitState.Visible) {
                HOME_SERVER_DROPDOWN_HANDLE_VISIBLE_ALPHA
            } else {
                0f
            }
        }
        val dragHandleAlpha = handleAlphaForSheetOffset(sheetDragOffset.value)
        val handleAlpha = transitionHandleAlpha.coerceAtMost(dragHandleAlpha)
        val sheetRadius = designDp(NokiUiKitPolicy.homeServerDropdownSheetBottomRadiusDp, scale)
        val dropdownSheetShape = RoundedCornerShape(bottomStart = sheetRadius, bottomEnd = sheetRadius)
        val sheetModifier = Modifier
            .fillMaxSize()
            .padding(
                bottom = designDp(NokiUiKitPolicy.homeServerDropdownSheetBottomSpaceDp, scale),
            )
        val sheetVisualModifier = sheetModifier
            .onSizeChanged { sheetHeightPx = it.height.toFloat() }
            .offset { IntOffset(0, sheetDragOffset.value.roundToInt()) }
        val contentClipInset = designDp(NokiUiKitPolicy.homeServerDropdownContentClipBottomInsetDp, scale)
        val sheetContentClipModifier = sheetVisualModifier.padding(bottom = contentClipInset)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = passthroughBlockerInteractionSource,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Box(
                modifier = sheetVisualModifier.homeServerDropdownSheetShadowLayer(
                    shape = dropdownSheetShape,
                    scale = scale,
                ),
            )
            Box(
                modifier = sheetVisualModifier.homeServerDropdownSheetGlassSurface(
                    shape = dropdownSheetShape,
                    backdrop = backdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    scale = scale,
                ),
            )
            Box(
                modifier = sheetContentClipModifier.clip(dropdownSheetShape),
            ) {
                val contentModifier = Modifier
                    .offset { IntOffset(0, -sheetDragOffset.value.roundToInt()) }
                    .fillMaxSize()
                content(contentModifier)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(designDp(NokiUiKitPolicy.homeServerDropdownHandleTouchHeightDp, scale))
                    .offset { IntOffset(0, sheetDragOffset.value.roundToInt()) }
                    .pointerInput(visible, sheetDismissThresholdPx, sheetHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                sheetDragScope.launch {
                                    sheetDragOffset.stop()
                                }
                            },
                            onDragEnd = {
                                settleSheetDrag()
                            },
                            onDragCancel = {
                                animateSheetBack()
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            val targetHeightPx = sheetHeightPx
                                .coerceAtLeast(sheetDismissThresholdPx * 2f)
                            val proposedOffset = sheetDragOffset.value + dragAmount
                            val nextOffset = proposedOffset.coerceIn(-targetHeightPx, 0f)
                            sheetDragScope.launch {
                                sheetDragOffset.snapTo(nextOffset)
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = designDp(
                                NokiUiKitPolicy.homeServerDropdownHandleBottomPaddingDp,
                                scale,
                            ),
                        )
                        .width(designDp(NokiUiKitPolicy.homeServerDropdownHandleWidthDp, scale))
                        .height(designDp(NokiUiKitPolicy.homeServerDropdownHandleHeightDp, scale))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = handleAlpha)),
                )
            }
        }
    }
}
