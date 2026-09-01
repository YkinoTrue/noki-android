package com.noki.vpn.ui

import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalResources
import androidx.core.graphics.withScale
import com.caverock.androidsvg.SVG

private val FigmaCssColorFallback = Regex("""var\(--[^,]+,\s*([^)]+)\)""")

@Composable
internal fun FigmaSvgAsset(
    @RawRes resId: Int,
    viewportWidth: Int,
    viewportHeight: Int,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val safeViewportWidth = viewportWidth.coerceAtLeast(1)
    val safeViewportHeight = viewportHeight.coerceAtLeast(1)
    val picture = remember(resources, resId, safeViewportWidth, safeViewportHeight) {
        val source = resources.openRawResource(resId)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
            .replace(FigmaCssColorFallback) { match -> match.groupValues[1].trim() }
        SVG.getFromString(source).renderToPicture(safeViewportWidth, safeViewportHeight)
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.withScale(
                x = size.width / safeViewportWidth.toFloat(),
                y = size.height / safeViewportHeight.toFloat(),
            ) {
                picture.draw(this)
            }
        }
    }
}
