package id.umkm.pos.pro.hardware.scanner

import java.nio.ByteBuffer

object LuminanceFrame {

    fun extract(buffer: ByteBuffer, rowStride: Int, width: Int, height: Int): ByteArray? {
        if (width <= 0 || height <= 0) return null
        if (rowStride < width) return null
        val requiredBytes = rowStride * height
        val source = buffer.duplicate()
        if (source.remaining() < requiredBytes) return null
        val luminance = ByteArray(requiredBytes)
        source.get(luminance, 0, requiredBytes)
        return luminance
    }

    fun rotateQuarterTurn(
        luminance: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int,
        clockwise: Boolean,
    ): RotatedFrame? {
        if (width <= 0 || height <= 0 || rowStride < width) return null
        if (luminance.size < rowStride * height) return null
        val rotatedStride = height
        val rotated = ByteArray(rotatedStride * width)
        for (column in 0 until width) {
            for (row in 0 until height) {
                val sourceIndex = row * rowStride + column
                val targetIndex = if (clockwise) {
                    column * rotatedStride + (height - 1 - row)
                } else {
                    (width - 1 - column) * rotatedStride + row
                }
                rotated[targetIndex] = luminance[sourceIndex]
            }
        }
        return RotatedFrame(
            luminance = rotated,
            rowStride = rotatedStride,
            width = height,
            height = width,
        )
    }
}

data class RotatedFrame(
    val luminance: ByteArray,
    val rowStride: Int,
    val width: Int,
    val height: Int,
)
