package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import java.util.TimeZone

private enum class FilterPickerTarget { START, END }

internal fun periodLabel(
    preset: ReportsRangePreset,
    range: ReportRange,
    timeZone: TimeZone,
    strings: PosStrings,
): String = when (preset) {
    ReportsRangePreset.TODAY -> strings.reportsRangeTodayLower
    ReportsRangePreset.LAST_7_DAYS -> strings.reportsRangeDaysSuffix(7)
    ReportsRangePreset.LAST_28_DAYS -> strings.reportsRangeDaysSuffix(28)
    ReportsRangePreset.CUSTOM ->
        "${ReportDates.formatLocalDateShort(range.startMillis, timeZone, strings)} - " +
            ReportDates.formatLocalDateShort(range.endMillis, timeZone, strings)
}

private val FILTER_PRESETS = listOf(
    ReportsRangePreset.TODAY,
    ReportsRangePreset.LAST_7_DAYS,
    ReportsRangePreset.LAST_28_DAYS,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ReportsFilterSheet(
    preset: ReportsRangePreset,
    range: ReportRange,
    timeZone: TimeZone,
    onDismiss: () -> Unit,
    onPresetSelected: (ReportsRangePreset) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
) {
    var isCustomEditing by remember { mutableStateOf(preset == ReportsRangePreset.CUSTOM) }
    var draftStartEpochDay by remember {
        mutableStateOf(ReportDates.epochDayForLocalMillis(range.startMillis, timeZone))
    }
    var draftEndEpochDay by remember {
        mutableStateOf(ReportDates.epochDayForLocalMillis(range.endMillis, timeZone))
    }
    var activePicker by remember { mutableStateOf<FilterPickerTarget?>(null) }
    val strings = LocalPosStrings.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PosSpacing.lg, vertical = PosSpacing.sm)
                .padding(bottom = PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            Text(
                text = strings.reportsFilterPeriod,
                style = LocalAppTextStyles.current.sectionHeading,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                FILTER_PRESETS.forEach { option ->
                    RangeChip(
                        label = option.pickerLabel(strings),
                        selected = option == preset,
                        onClick = {
                            onPresetSelected(option)
                            onDismiss()
                        },
                    )
                }
            }
            RangeChip(
                label = ReportsRangePreset.CUSTOM.pickerLabel(strings),
                selected = preset == ReportsRangePreset.CUSTOM,
                onClick = { isCustomEditing = true },
            )
            if (isCustomEditing) {
                FilterDateButton(
                    title = strings.reportsRangeStart,
                    value = ReportDates.formatEpochDayShort(draftStartEpochDay, strings),
                    onClick = { activePicker = FilterPickerTarget.START },
                )
                FilterDateButton(
                    title = strings.reportsRangeEnd,
                    value = ReportDates.formatEpochDayShort(draftEndEpochDay, strings),
                    onClick = { activePicker = FilterPickerTarget.END },
                )
                PrimaryActionButton(
                    text = strings.actionApply,
                    onClick = {
                        onCustomRangeSelected(draftStartEpochDay, draftEndEpochDay)
                        onDismiss()
                    },
                )
            }
        }
    }

    activePicker?.let { target ->
        val initialEpochDay = if (target == FilterPickerTarget.START) draftStartEpochDay else draftEndEpochDay
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochDay * ReportDates.MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            val selectedEpochDay = selectedMillis / ReportDates.MILLIS_PER_DAY
                            if (target == FilterPickerTarget.START) {
                                draftStartEpochDay = selectedEpochDay
                            } else {
                                draftEndEpochDay = selectedEpochDay
                            }
                        }
                        activePicker = null
                    },
                ) {
                    Text(text = strings.actionPick)
                }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) {
                    Text(text = strings.actionCancel)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FilterDateButton(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = LocalAppTextStyles.current.body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.heightIn(min = 38.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = LocalAppTextStyles.current.supportingLabel,
                modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
            )
        }
    }
}

@Composable
internal fun ExportDialog(
    onConfirm: (ReportExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(ReportExportFormat.CSV) }
    val strings = LocalPosStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = strings.reportsExportDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                Text(
                    text = strings.reportsExportAdHint(DownloadsReportSink.DESTINATION_LABEL),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.reportsExportFormat,
                    style = LocalAppTextStyles.current.tableHeader,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReportExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PosSizes.touchTarget)
                            .selectable(
                                selected = format == selectedFormat,
                                role = Role.RadioButton,
                                onClick = { selectedFormat = format },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = format == selectedFormat, onClick = null)
                        Spacer(modifier = Modifier.width(PosSpacing.sm))
                        Text(text = format.label, style = LocalAppTextStyles.current.body)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedFormat) },
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.reportsExportConfirm)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionCancel)
            }
        },
    )
}
