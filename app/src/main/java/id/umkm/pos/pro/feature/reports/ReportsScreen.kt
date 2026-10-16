package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.core.model.DailySales
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.SalesSummary
import id.umkm.pos.pro.core.model.Transaction
import id.umkm.pos.pro.core.model.TransactionStatus
import java.util.TimeZone

private enum class DatePickerTarget { START, END }

@Composable
internal fun ReportsScreen(
    uiState: ReportsUiState,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onSummaryDrillDown: () -> Unit,
    onDayDrillDown: (Long) -> Unit,
    onCloseDrillDown: () -> Unit,
    onExportCsv: () -> Unit,
    onDismissExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZone = remember { TimeZone.getDefault() }
    Column(modifier = modifier.fillMaxSize()) {
        ReportsHeader(
            uiState = uiState,
            timeZone = timeZone,
            onPresetSelected = onPresetSelected,
            onCustomRangeSelected = onCustomRangeSelected,
            onExportCsv = onExportCsv,
            onDismissExport = onDismissExport,
        )
        HorizontalDivider()
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val error = uiState.error
            val drillDown = uiState.drillDown
            when {
                error != null -> MessageState(
                    message = error.message,
                    actionLabel = "Retry",
                    onAction = onRefresh,
                )

                drillDown != null -> DrillDownSection(
                    drillDown = drillDown,
                    timeZone = timeZone,
                    onCloseDrillDown = onCloseDrillDown,
                )

                else -> MainReportContent(
                    uiState = uiState,
                    onSummaryDrillDown = onSummaryDrillDown,
                    onDayDrillDown = onDayDrillDown,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsHeader(
    uiState: ReportsUiState,
    timeZone: TimeZone,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onExportCsv: () -> Unit,
    onDismissExport: () -> Unit,
) {
    var activePicker by remember { mutableStateOf<DatePickerTarget?>(null) }
    val startEpochDay = ReportDates.epochDayForLocalMillis(uiState.range.startMillis, timeZone)
    val endEpochDay = ReportDates.epochDayForLocalMillis(uiState.range.endMillis, timeZone)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onExportCsv) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Export CSV")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportsRangePreset.entries
                .filter { preset -> preset != ReportsRangePreset.CUSTOM }
                .forEach { preset ->
                    FilterChip(
                        selected = preset == uiState.preset,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(text = preset.label) },
                    )
                }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { activePicker = DatePickerTarget.START }) {
                Text(text = "From ${ReportDates.formatLocalDate(uiState.range.startMillis, timeZone)}")
            }
            OutlinedButton(onClick = { activePicker = DatePickerTarget.END }) {
                Text(text = "To ${ReportDates.formatLocalDate(uiState.range.endMillis, timeZone)}")
            }
        }
        ExportStatus(exportState = uiState.exportState, onDismissExport = onDismissExport)
    }

    activePicker?.let { target ->
        val initialEpochDay = if (target == DatePickerTarget.START) startEpochDay else endEpochDay
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochDay * ReportDates.MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedEpochDay = selectedMillis / ReportDates.MILLIS_PER_DAY
                            if (target == DatePickerTarget.START) {
                                onCustomRangeSelected(selectedEpochDay, endEpochDay)
                            } else {
                                onCustomRangeSelected(startEpochDay, selectedEpochDay)
                            }
                        }
                        activePicker = null
                    },
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) {
                    Text(text = "Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ExportStatus(
    exportState: ReportsExportState,
    onDismissExport: () -> Unit,
) {
    when (exportState) {
        ReportsExportState.Idle -> Unit
        ReportsExportState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Exporting CSV", style = MaterialTheme.typography.labelMedium)
        }

        is ReportsExportState.Succeeded -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Saved ${exportState.artifactPath.substringAfterLast('/')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismissExport) {
                Text(text = "Dismiss")
            }
        }

        is ReportsExportState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Export failed: ${exportState.error.message}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismissExport) {
                Text(text = "Dismiss")
            }
        }
    }
}

@Composable
private fun MainReportContent(
    uiState: ReportsUiState,
    onSummaryDrillDown: () -> Unit,
    onDayDrillDown: (Long) -> Unit,
) {
    val summary = uiState.summary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (summary == null) {
            item {
                Text(
                    text = "Loading report",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item { SummarySection(summary = summary, onSummaryDrillDown = onSummaryDrillDown) }
            item { TrendSection(days = uiState.dailySales) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Daily breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSummaryDrillDown) {
                        Text(text = "All transactions")
                    }
                }
            }
            if (uiState.dailySales.isEmpty()) {
                item {
                    Text(
                        text = "No sales recorded in this range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(items = uiState.dailySales, key = { day -> day.epochDay }) { day ->
                    DailyRow(day = day, onDayDrillDown = onDayDrillDown)
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    summary: SalesSummary,
    onSummaryDrillDown: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onSummaryDrillDown)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Summary", style = MaterialTheme.typography.titleMedium)
            SummaryLine(label = "Sales", value = Rupiah.format(summary.totalSales))
            SummaryLine(label = "Net profit", value = Rupiah.format(summary.netProfit), isEmphasized = true)
            SummaryLine(label = "HPP", value = Rupiah.format(summary.totalHpp))
            SummaryLine(label = "Transactions", value = summary.transactionCount.toString())
            Text(
                text = "Tap to review every transaction",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    isEmphasized: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (isEmphasized) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
        )
    }
}

@Composable
private fun TrendSection(days: List<DailySales>) {
    val maxSales = days.maxOfOrNull { day -> day.totalSales } ?: 0L
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Sales trend", style = MaterialTheme.typography.titleMedium)
        if (days.isEmpty() || maxSales <= 0L) {
            Text(
                text = "No sales to chart in this range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val sales = day.totalSales.coerceAtLeast(0L)
                    val barFraction = if (sales <= 0L) {
                        0f
                    } else {
                        (sales.toFloat() / maxSales.toFloat()).coerceIn(0.04f, 1f)
                    }
                    Box(modifier = Modifier.width(10.dp).fillMaxHeight()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(barFraction)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                                ),
                        )
                    }
                }
            }
            Text(
                text = "Each bar is one day of sales",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyRow(
    day: DailySales,
    onDayDrillDown: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDayDrillDown(day.epochDay) }
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = ReportDates.formatEpochDay(day.epochDay), style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${day.transactionCount} trx",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = Rupiah.format(day.totalSales), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = Rupiah.format(day.netProfit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DrillDownSection(
    drillDown: ReportsDrillDown,
    timeZone: TimeZone,
    onCloseDrillDown: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseDrillDown) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = when (drillDown) {
                    is ReportsDrillDown.Summary -> "All transactions"
                    is ReportsDrillDown.Day -> ReportDates.formatEpochDay(drillDown.epochDay)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${drillDown.transactions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        if (drillDown.transactions.isEmpty()) {
            MessageState(message = "No transactions in this range.", actionLabel = null, onAction = {})
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(items = drillDown.transactions, key = { transaction -> transaction.id }) { transaction ->
                    TransactionRow(transaction = transaction, timeZone = timeZone)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    timeZone: TimeZone,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = transaction.invoiceNo, style = MaterialTheme.typography.titleSmall)
            Text(text = Rupiah.format(transaction.grandTotal), style = MaterialTheme.typography.titleSmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = ReportDates.formatTimestamp(transaction.createdAt, timeZone),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${paymentMethodLabel(transaction.paymentMethod)} · ${statusLabel(transaction.status)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageState(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

private fun paymentMethodLabel(paymentMethod: PaymentMethod): String = when (paymentMethod) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.QRIS -> "QRIS"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.SPLIT -> "Split"
    PaymentMethod.KASBON -> "Kasbon"
}

private fun statusLabel(status: TransactionStatus): String = when (status) {
    TransactionStatus.COMPLETED -> "Paid"
    TransactionStatus.PARTIAL_DEBT -> "Partial"
    TransactionStatus.RETURNED -> "Returned"
}
