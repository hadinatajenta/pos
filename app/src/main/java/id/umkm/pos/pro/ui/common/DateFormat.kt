package id.umkm.pos.pro.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DATE_PATTERN = "d MMM yyyy"

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(epochMillis))
