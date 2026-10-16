package id.umkm.pos.pro.hardware.receipt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import id.umkm.pos.pro.core.contract.ReceiptDocument
import id.umkm.pos.pro.core.contract.ReceiptRenderer
import java.io.ByteArrayOutputStream

class AndroidReceiptRenderer : ReceiptRenderer {

    override fun renderText(receipt: ReceiptDocument, widthChars: Int): String =
        ReceiptLayout.renderText(receipt, widthChars)

    override fun renderBitmap(receipt: ReceiptDocument, widthPx: Int): ByteArray {
        val receiptBitmap = drawReceipt(receipt, widthPx)
        return try {
            val pixelBuffer = IntArray(receiptBitmap.width * receiptBitmap.height)
            receiptBitmap.getPixels(pixelBuffer, 0, receiptBitmap.width, 0, 0, receiptBitmap.width, receiptBitmap.height)
            val rasterBytes = MonochromeRasterizer.toRasterBytes(pixelBuffer, receiptBitmap.width, receiptBitmap.height)
            EscPosCommandBuilder.buildRasterImage(
                rasterBytes = rasterBytes,
                widthBytes = (receiptBitmap.width + 7) / 8,
                height = receiptBitmap.height,
            )
        } finally {
            receiptBitmap.recycle()
        }
    }

    override fun renderPng(receipt: ReceiptDocument, widthPx: Int): ByteArray {
        val receiptBitmap = drawReceipt(receipt, widthPx)
        return try {
            val output = ByteArrayOutputStream()
            receiptBitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            output.toByteArray()
        } finally {
            receiptBitmap.recycle()
        }
    }

    private fun drawReceipt(receipt: ReceiptDocument, widthPx: Int): Bitmap {
        require(widthPx > 0) { "Receipt width must be positive" }
        val charactersPerLine = if (widthPx >= WIDE_BITMAP_THRESHOLD_PX) {
            ReceiptPaperWidth.MILLIMETER_80.characters
        } else {
            ReceiptPaperWidth.MILLIMETER_58.characters
        }
        val receiptTextSize = widthPx.toFloat() / charactersPerLine / MONOSPACE_WIDTH_RATIO
        val lineHeight = receiptTextSize * LINE_SPACING_RATIO
        val laidOutLines = ReceiptLayout.layoutLines(receipt, charactersPerLine)
        val qrisBitmap = decodeQrisImage(receipt.qrisImagePath, widthPx)
        val qrisHeight = qrisBitmap?.let { it.height + QRIS_MARGIN_PX } ?: 0
        val contentHeight = (laidOutLines.size * lineHeight).toInt() + qrisHeight + VERTICAL_PADDING_PX
        val receiptBitmap = Bitmap.createBitmap(widthPx, contentHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(receiptBitmap)
        canvas.drawColor(Color.WHITE)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.MONOSPACE
            textSize = receiptTextSize
        }
        var baselineY = VERTICAL_PADDING_PX + receiptTextSize
        for (line in laidOutLines) {
            textPaint.isFakeBoldText = line.isBold
            canvas.drawText(line.text, 0f, baselineY, textPaint)
            baselineY += lineHeight
        }
        if (qrisBitmap != null) {
            val left = (widthPx - qrisBitmap.width) / 2f
            canvas.drawBitmap(qrisBitmap, left, baselineY - receiptTextSize + QRIS_MARGIN_PX, null)
            qrisBitmap.recycle()
        }
        return receiptBitmap
    }

    private fun decodeQrisImage(qrisImagePath: String?, widthPx: Int): Bitmap? {
        if (qrisImagePath.isNullOrBlank()) return null
        val decoded = try {
            BitmapFactory.decodeFile(qrisImagePath)
        } catch (exception: Throwable) {
            null
        } ?: return null
        val targetWidth = (widthPx * QRIS_WIDTH_RATIO).toInt()
        if (decoded.width <= 0 || targetWidth <= 0) {
            decoded.recycle()
            return null
        }
        val targetHeight = (decoded.height * (targetWidth.toFloat() / decoded.width)).toInt().coerceAtLeast(1)
        val scaled = try {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
        } catch (exception: Throwable) {
            null
        }
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private companion object {
        const val MONOSPACE_WIDTH_RATIO = 0.6f
        const val LINE_SPACING_RATIO = 1.4f
        const val QRIS_WIDTH_RATIO = 0.6f
        const val WIDE_BITMAP_THRESHOLD_PX = 500
        const val VERTICAL_PADDING_PX = 8
        const val QRIS_MARGIN_PX = 12
        const val PNG_QUALITY = 100
    }
}
