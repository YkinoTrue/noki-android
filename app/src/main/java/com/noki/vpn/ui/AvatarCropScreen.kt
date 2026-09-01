package com.noki.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.AvatarBitmapDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AvatarCropScreen(
    sourceUri: String,
    language: AppLanguage,
    isUploading: Boolean,
    message: String?,
    onCancel: () -> Unit,
    onConfirm: (
        previewWidthPx: Float,
        previewHeightPx: Float,
        cropCircleSizePx: Float,
        cropScale: Float,
        cropOffsetX: Float,
        cropOffsetY: Float,
    ) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val preview by produceState<AvatarPreviewState>(initialValue = AvatarPreviewState.Loading, sourceUri) {
        var decoded: Bitmap? = null
        try {
            withContext(Dispatchers.IO) {
                AvatarBitmapDecoder.decode {
                    context.contentResolver.openInputStream(sourceUri.toUri())
                }.also { decoded = it }
            }
            value = AvatarPreviewState.Ready(checkNotNull(decoded))
            awaitCancellation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = AvatarPreviewState.Failed
        } finally {
            decoded?.recycle()
        }
    }
    val bitmap = (preview as? AvatarPreviewState.Ready)?.bitmap
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    var cropScale by remember(sourceUri) { mutableFloatStateOf(1f) }
    var cropOffset by remember(sourceUri) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        val previewWidth = maxWidth
        val availablePreviewHeight = maxHeight - 170.dp
        val previewHeight = when {
            availablePreviewHeight < 360.dp -> availablePreviewHeight
            availablePreviewHeight > 590.dp -> 590.dp
            else -> availablePreviewHeight
        }
        val cropCircleSize = minOf(previewWidth, previewHeight) * 0.78f
        val previewWidthPx = with(density) { previewWidth.toPx() }
        val previewHeightPx = with(density) { previewHeight.toPx() }
        val cropCircleSizePx = with(density) { cropCircleSize.toPx() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(width = previewWidth, height = previewHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black)
                    .pointerInput(imageBitmap, previewWidthPx, previewHeightPx, cropCircleSizePx) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (cropScale * zoom).coerceIn(1f, 5f)
                            val nextOffset = cropOffset + pan
                            cropScale = nextScale
                            cropOffset = imageBitmap?.let { image ->
                                clampAvatarCropOffset(
                                    offset = nextOffset,
                                    previewWidthPx = previewWidthPx,
                                    previewHeightPx = previewHeightPx,
                                    cropCircleSizePx = cropCircleSizePx,
                                    image = image,
                                    cropScale = nextScale,
                                )
                            } ?: nextOffset
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val image = imageBitmap
                if (image == null) {
                    AvatarCropText(
                        text = if (preview == AvatarPreviewState.Failed) {
                            tr(language, "Не удалось открыть изображение", "Could not open the image")
                        } else {
                            tr(language, "Загрузка...", "Loading...")
                        },
                        color = Color(0xFFB5C6D2),
                        fontSize = 13f,
                        lineHeight = 16f,
                    )
                } else {
                    AvatarCropCanvas(
                        image = image,
                        cropScale = cropScale,
                        cropOffset = cropOffset,
                        cropCircleSizePx = cropCircleSizePx,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            message?.takeIf { it.isNotBlank() }?.let {
                AvatarCropText(
                    text = it,
                    color = Color(0xFFB5C6D2),
                    fontSize = 12f,
                    lineHeight = 15f,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarCropActionButton(
                    text = tr(language, "Отмена", "Cancel"),
                    enabled = !isUploading,
                    onClick = onCancel,
                )
                AvatarCropActionButton(
                    text = if (isUploading) "..." else tr(language, "Сохранить", "Save"),
                    enabled = !isUploading && imageBitmap != null,
                    primary = true,
                    onClick = {
                        onConfirm(
                            previewWidthPx,
                            previewHeightPx,
                            cropCircleSizePx,
                            cropScale,
                            cropOffset.x,
                            cropOffset.y,
                        )
                    },
                )
            }
        }
    }
}

private sealed interface AvatarPreviewState {
    data object Loading : AvatarPreviewState
    data object Failed : AvatarPreviewState
    data class Ready(val bitmap: Bitmap) : AvatarPreviewState
}

@Composable
private fun AvatarCropCanvas(
    image: ImageBitmap,
    cropScale: Float,
    cropOffset: Offset,
    cropCircleSizePx: Float,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val baseScale = min(size.width / image.width.toFloat(), size.height / image.height.toFloat())
        val displayWidth = image.width * baseScale * cropScale
        val displayHeight = image.height * baseScale * cropScale
        val left = (size.width - displayWidth) / 2f + cropOffset.x
        val top = (size.height - displayHeight) / 2f + cropOffset.y
        val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
        val dstSize = IntSize(displayWidth.roundToInt(), displayHeight.roundToInt())
        val circleRect = Rect(
            left = (size.width - cropCircleSizePx) / 2f,
            top = (size.height - cropCircleSizePx) / 2f,
            right = (size.width + cropCircleSizePx) / 2f,
            bottom = (size.height + cropCircleSizePx) / 2f,
        )
        val circlePath = Path().apply { addOval(circleRect) }

        drawImage(
            image = image,
            dstOffset = dstOffset,
            dstSize = dstSize,
            alpha = 0.36f,
        )
        clipPath(circlePath) {
            drawImage(
                image = image,
                dstOffset = dstOffset,
                dstSize = dstSize,
                alpha = 1f,
            )
        }
        drawPath(
            path = circlePath,
            color = Color(0xCCF4FBFF),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun clampAvatarCropOffset(
    offset: Offset,
    previewWidthPx: Float,
    previewHeightPx: Float,
    cropCircleSizePx: Float,
    image: ImageBitmap,
    cropScale: Float,
): Offset {
    val baseScale = min(
        previewWidthPx / image.width.toFloat(),
        previewHeightPx / image.height.toFloat(),
    )
    val scaledWidth = image.width * baseScale * cropScale
    val scaledHeight = image.height * baseScale * cropScale
    val maxX = max(0f, (scaledWidth - cropCircleSizePx) / 2f)
    val maxY = max(0f, (scaledHeight - cropCircleSizePx) / 2f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

@Composable
private fun AvatarCropActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val background = if (primary) Color(0xFF75E7C3) else Color(0xFF132635)
    val content = if (primary) Color(0xFF07111A) else Color(0xFFF4FBFF)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) background else background.copy(alpha = 0.35f))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        AvatarCropText(
            text = text,
            color = if (enabled) content else content.copy(alpha = 0.5f),
            fontSize = 15f,
            lineHeight = 18f,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AvatarCropText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF4FBFF),
    fontSize: Float,
    lineHeight: Float,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        modifier = modifier,
    )
}
