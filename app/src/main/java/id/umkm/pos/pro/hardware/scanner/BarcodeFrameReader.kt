package id.umkm.pos.pro.hardware.scanner

interface BarcodeFrameReader {
    fun read(luminance: ByteArray, rowStride: Int, width: Int, height: Int): String?
}
