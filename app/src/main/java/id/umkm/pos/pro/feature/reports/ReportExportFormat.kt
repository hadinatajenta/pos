package id.umkm.pos.pro.feature.reports

enum class ReportExportFormat(
    val label: String,
    val fileExtension: String,
) {
    CSV(label = "CSV", fileExtension = "csv"),
    XLSX(label = "Excel (.xlsx)", fileExtension = "xlsx"),
}
