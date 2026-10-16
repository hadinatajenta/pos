package id.umkm.pos.pro.hardware.receipt

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ReceiptAlignment
import org.junit.Assert.assertThrows
import org.junit.Test

class EscPosCommandBuilderTest {

    @Test
    fun initializeEmitsResetCommand() {
        assertThat(EscPosCommandBuilder.buildInitialize()).isEqualTo(byteArrayOf(0x1B, 0x40))
    }

    @Test
    fun alignmentMapsEveryAlignment() {
        assertThat(EscPosCommandBuilder.buildAlignment(ReceiptAlignment.START)).isEqualTo(byteArrayOf(0x1B, 0x61, 0x00))
        assertThat(EscPosCommandBuilder.buildAlignment(ReceiptAlignment.CENTER)).isEqualTo(byteArrayOf(0x1B, 0x61, 0x01))
        assertThat(EscPosCommandBuilder.buildAlignment(ReceiptAlignment.END)).isEqualTo(byteArrayOf(0x1B, 0x61, 0x02))
    }

    @Test
    fun boldEmitsOnAndOffCommands() {
        assertThat(EscPosCommandBuilder.buildBold(true)).isEqualTo(byteArrayOf(0x1B, 0x45, 0x01))
        assertThat(EscPosCommandBuilder.buildBold(false)).isEqualTo(byteArrayOf(0x1B, 0x45, 0x00))
    }

    @Test
    fun textLineEncodesAsciiAndLineFeed() {
        assertThat(EscPosCommandBuilder.buildTextLine("AB")).isEqualTo(byteArrayOf(0x41, 0x42, 0x0A))
    }

    @Test
    fun feedEmitsRequestedLineCount() {
        assertThat(EscPosCommandBuilder.buildFeed(3)).isEqualTo(byteArrayOf(0x1B, 0x64, 0x03))
        assertThat(EscPosCommandBuilder.buildFeed(0)).isEqualTo(byteArrayOf(0x1B, 0x64, 0x00))
    }

    @Test
    fun feedRejectsOutOfRangeLineCount() {
        assertThrows(IllegalArgumentException::class.java) { EscPosCommandBuilder.buildFeed(-1) }
        assertThrows(IllegalArgumentException::class.java) { EscPosCommandBuilder.buildFeed(256) }
    }

    @Test
    fun cutEmitsFullAndPartialCommands() {
        assertThat(EscPosCommandBuilder.buildCut()).isEqualTo(byteArrayOf(0x1D, 0x56, 0x00))
        assertThat(EscPosCommandBuilder.buildCut(shouldCutPartial = true)).isEqualTo(byteArrayOf(0x1D, 0x56, 0x01))
    }

    @Test
    fun rasterImageWritesLittleEndianDimensions() {
        val raster = ByteArray(48 * 10) { it.toByte() }

        val command = EscPosCommandBuilder.buildRasterImage(raster, widthBytes = 48, height = 10)

        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, 0x30, 0x00, 0x0A, 0x00)
        assertThat(command.copyOfRange(0, header.size)).isEqualTo(header)
        assertThat(command.copyOfRange(header.size, command.size)).isEqualTo(raster)
    }

    @Test
    fun rasterImageRejectsMismatchedByteCount() {
        assertThrows(IllegalArgumentException::class.java) {
            EscPosCommandBuilder.buildRasterImage(ByteArray(5), widthBytes = 2, height = 4)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EscPosCommandBuilder.buildRasterImage(ByteArray(0), widthBytes = 0, height = 4)
        }
    }

    @Test
    fun documentFramesLinesWithInitFeedAndCut() {
        val lines = listOf(
            LayoutLine("TOKO", ReceiptAlignment.CENTER, isBold = true),
            LayoutLine("Total", ReceiptAlignment.END, isBold = false),
        )

        val document = EscPosCommandBuilder.buildDocument(lines)

        assertThat(document.copyOfRange(0, 2)).isEqualTo(EscPosCommandBuilder.buildInitialize())
        assertThat(document.copyOfRange(document.size - 3, document.size)).isEqualTo(EscPosCommandBuilder.buildCut())
        assertThat(containsSubsequence(document, EscPosCommandBuilder.buildBold(true))).isTrue()
        assertThat(containsSubsequence(document, EscPosCommandBuilder.buildBold(false))).isTrue()
        assertThat(containsSubsequence(document, EscPosCommandBuilder.buildAlignment(ReceiptAlignment.END))).isTrue()
    }

    @Test
    fun documentEmitsBoldOnceForConsecutiveBoldLines() {
        val lines = listOf(
            LayoutLine("A", ReceiptAlignment.CENTER, isBold = true),
            LayoutLine("B", ReceiptAlignment.CENTER, isBold = true),
        )

        val document = EscPosCommandBuilder.buildDocument(lines)

        assertThat(countOccurrences(document, EscPosCommandBuilder.buildBold(true))).isEqualTo(1)
    }

    @Test
    fun rasterDocumentFramesRasterWithInitFeedAndCut() {
        val raster = EscPosCommandBuilder.buildRasterImage(ByteArray(1), widthBytes = 1, height = 1)

        val document = EscPosCommandBuilder.buildRasterDocument(raster)

        assertThat(document.copyOfRange(0, 2)).isEqualTo(EscPosCommandBuilder.buildInitialize())
        assertThat(containsSubsequence(document, raster)).isTrue()
        assertThat(document.copyOfRange(document.size - 3, document.size)).isEqualTo(EscPosCommandBuilder.buildCut())
    }

    private fun containsSubsequence(source: ByteArray, target: ByteArray): Boolean =
        countOccurrences(source, target) > 0

    private fun countOccurrences(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || source.size < target.size) return 0
        var occurrences = 0
        for (startIndex in 0..source.size - target.size) {
            var isMatch = true
            for (offset in target.indices) {
                if (source[startIndex + offset] != target[offset]) {
                    isMatch = false
                    break
                }
            }
            if (isMatch) occurrences++
        }
        return occurrences
    }
}
