package com.noki.vpn.ui

import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

private const val InviteQrPrefix = "noki://invite/"

fun inviteQrPayload(inviteCode: String): String = InviteQrPrefix + inviteCode.trim()

fun inviteCodeQrBitmap(
    inviteCode: String,
    sizePx: Int,
): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(
        inviteQrPayload(inviteCode),
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        hints,
    )
    val bitmap = createBitmap(sizePx, sizePx)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return bitmap.asImageBitmap()
}
