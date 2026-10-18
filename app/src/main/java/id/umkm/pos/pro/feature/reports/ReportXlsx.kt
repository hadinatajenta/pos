package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import java.io.ByteArrayOutputStream
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportXlsx {

    private const val CONTENT_TYPES_ENTRY = "[Content_Types].xml"
    private const val ROOT_RELS_ENTRY = "_rels/.rels"
    private const val WORKBOOK_ENTRY = "xl/workbook.xml"
    private const val WORKBOOK_RELS_ENTRY = "xl/_rels/workbook.xml.rels"
    private const val TRANSACTION_SHEET_ENTRY = "xl/worksheets/sheet1.xml"
    private const val PRODUCT_SHEET_ENTRY = "xl/worksheets/sheet2.xml"

    private const val SPREADSHEET_NAMESPACE =
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val RELATIONSHIP_NAMESPACE =
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACKAGE_RELATIONSHIP_NAMESPACE =
        "http://schemas.openxmlformats.org/package/2006/relationships"

    private const val XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    fun buildWorkbook(
        content: ReportContent,
        strings: PosStrings,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): ByteArray {
        val transactionRows = buildList<List<XlsxCell>> {
            add(ReportCsv.reportTransactionColumns(strings).map { column -> XlsxCell.Text(column) })
            content.transactions.forEach { transaction ->
                add(
                    listOf(
                        XlsxCell.Text(ReportDates.formatTimestamp(transaction.createdAt, timeZone)),
                        XlsxCell.Text(transaction.invoiceNo),
                        XlsxCell.Number(transaction.grandTotal.toString()),
                        XlsxCell.Text(paymentMethodLabel(transaction.paymentMethod, strings)),
                        XlsxCell.Number(transaction.totalHpp.toString()),
                        XlsxCell.Number(transaction.netProfit.toString()),
                    ),
                )
            }
        }
        val productRows = buildList<List<XlsxCell>> {
            add(ReportCsv.productSummaryColumns(strings).map { column -> XlsxCell.Text(column) })
            content.productSummaries.forEach { summary ->
                add(
                    listOf(
                        XlsxCell.Text(summary.name),
                        XlsxCell.Number(formatQuantity(summary.quantitySold)),
                        XlsxCell.Number(summary.salesValue.toString()),
                    ),
                )
            }
        }

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            writeEntry(zip, CONTENT_TYPES_ENTRY, contentTypesXml())
            writeEntry(zip, ROOT_RELS_ENTRY, rootRelationshipsXml())
            writeEntry(zip, WORKBOOK_ENTRY, workbookXml(strings))
            writeEntry(zip, WORKBOOK_RELS_ENTRY, workbookRelationshipsXml())
            writeEntry(zip, TRANSACTION_SHEET_ENTRY, worksheetXml(transactionRows))
            writeEntry(zip, PRODUCT_SHEET_ENTRY, worksheetXml(productRows))
        }
        return output.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml(): String = buildString {
        append(XML_DECLARATION)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        append("<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        append("</Types>")
    }

    private fun rootRelationshipsXml(): String = buildString {
        append(XML_DECLARATION)
        append("<Relationships xmlns=\"$PACKAGE_RELATIONSHIP_NAMESPACE\">")
        append("<Relationship Id=\"rId1\" Type=\"$RELATIONSHIP_NAMESPACE/officeDocument\" Target=\"xl/workbook.xml\"/>")
        append("</Relationships>")
    }

    private fun workbookXml(strings: PosStrings): String = buildString {
        append(XML_DECLARATION)
        append("<workbook xmlns=\"$SPREADSHEET_NAMESPACE\" xmlns:r=\"$RELATIONSHIP_NAMESPACE\">")
        append("<sheets>")
        append("<sheet name=\"${strings.exportTransactionSheetName}\" sheetId=\"1\" r:id=\"rId1\"/>")
        append("<sheet name=\"${strings.exportProductSheetName}\" sheetId=\"2\" r:id=\"rId2\"/>")
        append("</sheets>")
        append("</workbook>")
    }

    private fun workbookRelationshipsXml(): String = buildString {
        append(XML_DECLARATION)
        append("<Relationships xmlns=\"$PACKAGE_RELATIONSHIP_NAMESPACE\">")
        append("<Relationship Id=\"rId1\" Type=\"$RELATIONSHIP_NAMESPACE/worksheet\" Target=\"worksheets/sheet1.xml\"/>")
        append("<Relationship Id=\"rId2\" Type=\"$RELATIONSHIP_NAMESPACE/worksheet\" Target=\"worksheets/sheet2.xml\"/>")
        append("</Relationships>")
    }

    private fun worksheetXml(rows: List<List<XlsxCell>>): String = buildString {
        append(XML_DECLARATION)
        append("<worksheet xmlns=\"$SPREADSHEET_NAMESPACE\">")
        append("<sheetData>")
        rows.forEachIndexed { rowIndex, cells ->
            val rowNumber = rowIndex + 1
            append("<row r=\"$rowNumber\">")
            cells.forEachIndexed { columnIndex, cell ->
                append(cellXml(columnName(columnIndex) + rowNumber, cell))
            }
            append("</row>")
        }
        append("</sheetData>")
        append("</worksheet>")
    }

    private fun cellXml(reference: String, cell: XlsxCell): String = when (cell) {
        is XlsxCell.Text ->
            "<c r=\"$reference\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXml(cell.value)}</t></is></c>"

        is XlsxCell.Number ->
            "<c r=\"$reference\"><v>${cell.value}</v></c>"
    }

    private fun columnName(index: Int): String {
        var remaining = index
        val name = StringBuilder()
        do {
            name.append('A' + remaining % 26)
            remaining = remaining / 26 - 1
        } while (remaining >= 0)
        return name.reverse().toString()
    }

    private fun escapeXml(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(character)
            }
        }
    }
}

sealed interface XlsxCell {
    data class Text(val value: String) : XlsxCell

    data class Number(val value: String) : XlsxCell
}
