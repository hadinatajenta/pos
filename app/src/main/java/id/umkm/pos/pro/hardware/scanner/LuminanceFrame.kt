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
}
