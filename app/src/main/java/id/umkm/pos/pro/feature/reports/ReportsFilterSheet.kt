package id.umkm.pos.pro.feature.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.data.file.DownloadsFileSink
import id.umkm.pos.pro.core.model.ReportRange
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.theme.PosSizes
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ReportsPalette.CardSurface,
        contentColor = ReportsPalette.TextMain,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = strings.reportsFilterPeriod,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ReportsPalette.TextMain,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
            }
            if (isCustomEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        color = ReportsPalette.CardSurface,
        contentColor = ReportsPalette.TextMain,
        shape = ReportsShapes.smallCard,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PosSizes.touchTarget)
            .shadow(
                elevation = 4.dp,
                shape = ReportsShapes.smallCard,
                clip = false,
                ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ReportsPalette.TextMuted,
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReportsPalette.TextMain,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = ReportsPalette.Primary,
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
    ReportsFilterPill(
        label = label,
        selected = selected,
        onClick = onClick,
    )
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
        shape = ReportsShapes.card,
        containerColor = ReportsPalette.CardSurface,
        titleContentColor = ReportsPalette.TextMain,
        textContentColor = ReportsPalette.TextMain,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = strings.reportsExportDialogTitle,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ReportsPalette.TextMain,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = strings.reportsExportAdHint(DownloadsFileSink.DESTINATION_LABEL),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = ReportsPalette.TextMuted,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.reportsExportFormat,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                        color = ReportsPalette.TextMuted,
                    )
                    ReportExportFormat.entries.forEach { format ->
                        ExportFormatOption(
                            format = format,
                            selected = format == selectedFormat,
                            onSelect = { selectedFormat = format },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedFormat) },
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(
                    text = strings.reportsExportConfirm,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReportsPalette.Primary,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(
                    text = strings.actionCancel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ReportsPalette.TextMuted,
                )
            }
        },
    )
}

@Composable
private fun ExportFormatOption(
    format: ReportExportFormat,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PosSizes.touchTarget)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = ReportsPalette.Primary,
                unselectedColor = ReportsPalette.TextMuted,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = format.label,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ReportsPalette.TextMain,
        )
    }
}
