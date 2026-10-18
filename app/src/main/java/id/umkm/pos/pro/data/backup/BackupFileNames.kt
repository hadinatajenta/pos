package id.umkm.pos.pro.data.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BackupFileNames {

    const val PREFIX: String = "umkm-pos-backup"
    const val EXTENSION: String = "json"

    private const val TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss"

    fun forTimestamp(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val timestamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
            .apply { this.timeZone = timeZone }
            .format(Date(epochMillis))
        return "$PREFIX-$timestamp.$EXTENSION"
    }
}
