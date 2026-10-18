package id.umkm.pos.pro.data.file

import java.io.File
import java.util.Locale

object FileNames {

    private const val CSV_MIME_TYPE = "text/csv"
    private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val JSON_MIME_TYPE = "application/json"
    private const val DEFAULT_MIME_TYPE = "application/octet-stream"

    fun normalized(requestedName: String): String {
        val fileName = File(requestedName.trim()).name
        require(fileName.isNotBlank()) { "file name must not be blank" }
        return fileName
    }

    fun mimeTypeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "csv" -> CSV_MIME_TYPE
            "xlsx" -> XLSX_MIME_TYPE
            "json" -> JSON_MIME_TYPE
            else -> DEFAULT_MIME_TYPE
        }
}
