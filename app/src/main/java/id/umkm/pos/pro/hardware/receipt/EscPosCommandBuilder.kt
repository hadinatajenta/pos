package id.umkm.pos.pro.hardware.receipt

import id.umkm.pos.pro.core.contract.ReceiptAlignment
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object EscPosCommandBuilder {

    private const val ESC = 0x1B
    private const val GS = 0x1D
    private const val LINE_FEED = 0x0A
    private const val ALIGNMENT_COMMAND = 0x61
    private const val BOLD_COMMAND = 0x45
    private const val FEED_COMMAND = 0x64
    private const val CUT_COMMAND = 0x56
    private const val RASTER_COMMAND = 0x76
    private const val RASTER_MODE = 0x30
    private const val RASTER_NORMAL_DENSITY = 0x00
    private const val CUT_FULL = 0x00
    private const val CUT_PARTIAL = 0x01
    private const val MAX_FEED_LINES = 255

    val defaultCharset: Charset = Charsets.ISO_8859_1

    fun buildInitialize(): ByteArray = byteArrayOf(ESC.toByte(), 0x40.toByte())

    fun buildAlignment(alignment: ReceiptAlignment): ByteArray =
        byteArrayOf(ESC.toByte(), ALIGNMENT_COMMAND.toByte(), alignmentCode(alignment))

    fun buildBold(isBold: Boolean): ByteArray =
        byteArrayOf(ESC.toByte(), BOLD_COMMAND.toByte(), if (isBold) 1.toByte() else 0.toByte())

    fun buildText(text: String, charset: Charset = defaultCharset): ByteArray =
        text.toByteArray(charset)

    fun buildTextLine(text: String, charset: Charset = defaultCharset): ByteArray =
        text.toByteArray(charset) + byteArrayOf(LINE_FEED.toByte())

    fun buildFeed(lineCount: Int): ByteArray {
        require(lineCount in 0..MAX_FEED_LINES) { "Feed line count must be between 0 and $MAX_FEED_LINES" }
        return byteArrayOf(ESC.toByte(), FEED_COMMAND.toByte(), lineCount.toByte())
    }

    fun buildCut(shouldCutPartial: Boolean = false): ByteArray =
        byteArrayOf(
            GS.toByte(),
            CUT_COMMAND.toByte(),
            if (shouldCutPartial) CUT_PARTIAL.toByte() else CUT_FULL.toByte(),
        )

    fun buildRasterImage(rasterBytes: ByteArray, widthBytes: Int, height: Int): ByteArray {
        require(widthBytes > 0) { "Raster width must be positive" }
        require(height > 0) { "Raster height must be positive" }
        require(rasterBytes.size == widthBytes * height) {
            "Raster byte count does not match width $widthBytes and height $height"
        }
        return byteArrayOf(
            GS.toByte(),
            RASTER_COMMAND.toByte(),
            RASTER_MODE.toByte(),
            RASTER_NORMAL_DENSITY.toByte(),
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            ((height shr 8) and 0xFF).toByte(),
        ) + rasterBytes
    }

    fun buildDocument(
        lines: List<LayoutLine>,
        leadingFeedLines: Int = 0,
        trailingFeedLines: Int = 3,
        shouldCut: Boolean = true,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.write(buildInitialize())
        if (leadingFeedLines > 0) buffer.write(buildFeed(leadingFeedLines))
        var isBoldActive = false
        for (line in lines) {
            buffer.write(buildAlignment(line.alignment))
            if (line.isBold != isBoldActive) {
                buffer.write(buildBold(line.isBold))
                isBoldActive = line.isBold
            }
            buffer.write(buildTextLine(line.text))
        }
        if (isBoldActive) buffer.write(buildBold(false))
        if (trailingFeedLines > 0) buffer.write(buildFeed(trailingFeedLines))
        if (shouldCut) buffer.write(buildCut())
        return buffer.toByteArray()
    }

    fun buildRasterDocument(
        rasterCommand: ByteArray,
        leadingFeedLines: Int = 0,
        trailingFeedLines: Int = 3,
        shouldCut: Boolean = true,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.write(buildInitialize())
        if (leadingFeedLines > 0) buffer.write(buildFeed(leadingFeedLines))
        buffer.write(rasterCommand)
        if (trailingFeedLines > 0) buffer.write(buildFeed(trailingFeedLines))
        if (shouldCut) buffer.write(buildCut())
        return buffer.toByteArray()
    }

    private fun alignmentCode(alignment: ReceiptAlignment): Byte = when (alignment) {
        ReceiptAlignment.START -> 0
        ReceiptAlignment.CENTER -> 1
        ReceiptAlignment.END -> 2
    }
}
