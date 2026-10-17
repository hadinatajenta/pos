package id.umkm.pos.pro.feature.receipt

internal fun twoColumnLine(label: String, value: String, widthChars: Int): String {
    val available = widthChars - value.length
    if (available <= label.length) return "$label $value"
    return label + " ".repeat(available - label.length) + value
}
