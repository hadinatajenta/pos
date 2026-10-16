package id.umkm.pos.pro.hardware.receipt

object MonochromeRasterizer {

    const val DEFAULT_THRESHOLD = 128
    private const val ALPHA_OPAQUE_THRESHOLD = 128

    fun toRasterBytes(
        argbPixels: IntArray,
        width: Int,
        height: Int,
        threshold: Int = DEFAULT_THRESHOLD,
    ): ByteArray {
        require(width > 0) { "Raster width must be positive" }
        require(height > 0) { "Raster height must be positive" }
        require(argbPixels.size >= width * height) { "Pixel buffer is smaller than the raster size" }
        val widthBytes = (width + 7) / 8
        val raster = ByteArray(widthBytes * height)
        for (row in 0 until height) {
            val pixelRowOffset = row * width
            val byteRowOffset = row * widthBytes
            for (column in 0 until width) {
                if (isDark(argbPixels[pixelRowOffset + column], threshold)) {
                    val byteIndex = byteRowOffset + (column shr 3)
                    val bitIndex = 7 - (column and 7)
                    raster[byteIndex] = (raster[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        return raster
    }

    private fun isDark(argb: Int, threshold: Int): Boolean {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < ALPHA_OPAQUE_THRESHOLD) return false
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        val luminance = (red * 299 + green * 587 + blue * 114) / 1000
        return luminance < threshold
    }
}
