package id.umkm.pos.pro.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import id.umkm.pos.pro.R
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.formatDateTime
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun ShiftHistorySheet(
    state: ShiftUiState,
    actions: ShiftActions,
) {
    val strings = LocalPosStrings.current
    ShiftSheetContainer(
        title = strings.shiftHistoryTitle,
        supportingText = strings.shiftHistorySubtitle,
        onDismiss = actions.onDismissHistory,
        footer = {
            SecondaryTextAction(
                text = strings.actionClose,
                onClick = actions.onDismissHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        ShiftHistoryContent(
            entries = state.history,
            isLoading = state.isHistoryLoading,
            errorMessage = state.historyErrorMessage?.asTextOrNull(),
            onRetry = actions.onRetryHistory,
        )
    }
}

@Composable
private fun ShiftHistoryContent(
    entries: List<ShiftHistoryEntry>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    val strings = LocalPosStrings.current
    when {
        isLoading && entries.isEmpty() -> LoadingState(
            modifier = Modifier.fillMaxSize(),
            label = strings.shiftHistoryLoading,
        )

        errorMessage != null && entries.isEmpty() -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = errorMessage,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.error,
            )
            SecondaryTextAction(text = strings.actionRetry, onClick = onRetry)
        }

        entries.isEmpty() -> EmptyState(
            iconRes = R.drawable.ic_clock,
            title = strings.shiftHistoryEmptyTitle,
            supportingText = strings.shiftHistoryEmptyHint,
            modifier = Modifier.fillMaxSize(),
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PosSpacing.md, vertical = PosSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            items(items = entries, key = { entry -> entry.shift.id }) { entry ->
                ShiftHistoryCard(entry = entry)
            }
        }
    }
}

@Composable
private fun ShiftHistoryCard(entry: ShiftHistoryEntry) {
    val shift = entry.shift
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = shift.cashierName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.shiftHistoryOpened(formatDateTime(shift.openedAt, strings)),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shift.closedAt?.let { closedAt ->
                    Text(
                        text = strings.shiftHistoryClosed(formatDateTime(closedAt, strings)),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entry.isActive) {
                StatusChip(
                    iconRes = R.drawable.ic_clock,
                    label = strings.shiftHistoryStatusActive,
                    containerColor = palette.successContainer,
                    contentColor = palette.successText,
                )
            } else {
                StatusChip(
                    iconRes = R.drawable.ic_circle_check,
                    label = strings.shiftHistoryStatusClosed,
                    containerColor = palette.neutralContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        ShiftMoneyRow(
            label = strings.shiftHistoryStartingCash,
            value = Rupiah.format(shift.startingCash),
        )
        ShiftMoneyRow(
            label = strings.shiftHistoryExpectedCash,
            value = entry.expectedEndingCash?.let(Rupiah::format) ?: strings.shiftValueEmpty,
            isEmphasized = entry.isActive,
        )
        if (entry.isActive) {
            Text(
                text = strings.shiftHistoryLiveNote,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ShiftMoneyRow(
                label = strings.shiftHistoryActualCash,
                value = shift.actualEndingCash?.let(Rupiah::format) ?: strings.shiftValueEmpty,
            )
            entry.variance?.let { variance ->
                ShiftVarianceRow(
                    variance = variance,
                    label = strings.shiftHistoryVariance,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
