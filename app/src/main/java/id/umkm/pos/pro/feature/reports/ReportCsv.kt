package id.umkm.pos.pro.feature.reports

import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.Transaction
import java.util.TimeZone

object ReportCsv {

    private const val RECORD_SEPARATOR = "\r\n"

    val TRANSACTION_COLUMNS: List<String> = listOf(
        "invoice_no",
        "created_at",
        "payment_method",
        "status",
        "subtotal",
        "discount_total",
        "invoice_discount",
        "tax_total",
        "rounding",
        "grand_total",
        "total_hpp",
        "net_profit",
        "paid_amount",
        "change_amount",
        "remaining_debt",
    )

    val DAILY_COLUMNS: List<String> = listOf(
        "date",
        "total_sales",
        "net_profit",
        "transaction_count",
    )

    fun buildTransactionsCsv(
        transactions: List<Transaction>,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = buildString {
        appendRecord(TRANSACTION_COLUMNS)
        transactions.forEach { transaction ->
            appendRecord(
                listOf(
                    transaction.invoiceNo,
                    ReportDates.formatTimestamp(transaction.createdAt, timeZone),
                    transaction.paymentMethod.name,
                    transaction.status.name,
                    transaction.subtotal.toString(),
                    transaction.discountTotal.toString(),
                    transaction.invoiceDiscount.toString(),
                    transaction.taxTotal.toString(),
                    transaction.rounding.toString(),
                    transaction.grandTotal.toString(),
                    transaction.totalHpp.toString(),
                    transaction.netProfit.toString(),
                    transaction.paidAmount.toString(),
                    transaction.changeAmount.toString(),
                    transaction.remainingDebt.toString(),
                ),
            )
        }
    }

    fun buildDailySalesCsv(days: List<DailySales>): String = buildString {
        appendRecord(DAILY_COLUMNS)
        days.forEach { day ->
            appendRecord(
                listOf(
                    ReportDates.formatEpochDay(day.epochDay),
                    day.totalSales.toString(),
                    day.netProfit.toString(),
                    day.transactionCount.toString(),
                ),
            )
        }
    }

    fun escapeField(value: String): String {
        val requiresQuoting = value != value.trim() ||
            value.any { character ->
                character == ',' || character == '"' || character == '\n' || character == '\r'
            }
        if (!requiresQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun StringBuilder.appendRecord(fields: List<String>) {
        append(fields.joinToString(separator = ",") { field -> escapeField(field) })
        append(RECORD_SEPARATOR)
    }
}
