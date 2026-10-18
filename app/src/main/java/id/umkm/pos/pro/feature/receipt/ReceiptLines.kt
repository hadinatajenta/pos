package id.umkm.pos.pro.feature.receipt

import id.umkm.pos.pro.hardware.receipt.ReceiptLayout

internal fun twoColumnLine(label: String, value: String, widthChars: Int): String {
    val labelWidth = ReceiptLayout.stringDisplayWidth(label)
    val valueWidth = ReceiptLayout.stringDisplayWidth(value)
    val available = widthChars - valueWidth
    if (available <= labelWidth) return "$label $value"
    return label + " ".repeat(available - labelWidth) + value
}

