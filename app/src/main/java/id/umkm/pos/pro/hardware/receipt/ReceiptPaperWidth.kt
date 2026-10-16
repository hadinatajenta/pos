package id.umkm.pos.pro.hardware.receipt

enum class ReceiptPaperWidth(
    val characters: Int,
    val pixels: Int,
) {
    MILLIMETER_58(characters = 32, pixels = 384),
    MILLIMETER_80(characters = 48, pixels = 576),
}
