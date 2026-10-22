package id.umkm.pos.pro.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun StaffList(
    state: StaffUiState,
    actions: StaffActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val activeStaff = state.activeStaffList
    val inactiveStaff = state.inactiveStaffList

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = PosSpacing.md,
            end = PosSpacing.md,
            top = PosSpacing.xs,
            bottom = PosSizes.listBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        state.errorMessage?.let { error ->
            item(key = "staff-error") {
                StaffErrorBanner(
                    message = error.resolve(strings),
                    onDismiss = actions.onMessageConsumed,
                )
            }
        }
        if (activeStaff.isNotEmpty()) {
            item(key = "staff-active-header") {
                PosSectionHeader(
                    title = strings.staffActiveLabel,
                    supportingText = strings.staffActiveCount(activeStaff.size),
                    modifier = Modifier.padding(top = PosSpacing.xs),
                )
            }
            items(activeStaff, key = { staff -> "active-${staff.id}" }) { staff ->
                StaffRow(
                    staff = staff,
                    isOnDuty = staff.id == state.activeStaffId,
                    actions = actions,
                )
            }
        }
        if (inactiveStaff.isNotEmpty()) {
            if (activeStaff.isNotEmpty()) {
                item(key = "staff-section-gap") {
                    Spacer(modifier = Modifier.height(PosSpacing.xs))
                }
            }
            item(key = "staff-inactive-header") {
                PosSectionHeader(
                    title = strings.staffInactiveLabel,
                    supportingText = strings.staffInactiveCount(inactiveStaff.size),
                    modifier = Modifier.padding(top = PosSpacing.xs),
                )
            }
            items(inactiveStaff, key = { staff -> "inactive-${staff.id}" }) { staff ->
                StaffRow(
                    staff = staff,
                    isOnDuty = false,
                    actions = actions,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffRow(
    staff: Staff,
    isOnDuty: Boolean,
    actions: StaffActions,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val statusLabel = if (staff.isActive) strings.staffActiveLabel else strings.staffInactiveLabel

    PosCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(STAFF_ROW_TEST_TAG_PREFIX + staff.id),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                Text(
                    text = staff.name,
                    style = LocalAppTextStyles.current.sectionHeading,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
                ) {
                    StatusChip(
                        iconRes = if (staff.isActive) {
                            R.drawable.ic_circle_check
                        } else {
                            R.drawable.ic_circle_x
                        },
                        label = statusLabel,
                        containerColor = if (staff.isActive) {
                            palette.successContainer
                        } else {
                            palette.neutralContainer
                        },
                        contentColor = if (staff.isActive) {
                            palette.success
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (isOnDuty) {
                        StatusChip(
                            iconRes = R.drawable.ic_clock,
                            label = strings.staffOnDutyLabel,
                            containerColor = palette.goldContainer,
                            contentColor = palette.onGoldContainer,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                SecondaryTextAction(
                    text = strings.actionEdit,
                    onClick = { actions.onOpenRenameEditor(staff) },
                )
                if (staff.isActive) {
                    SecondaryTextAction(
                        text = strings.staffDeactivateAction,
                        onClick = { actions.onRequestDeactivate(staff) },
                    )
                } else {
                    SecondaryTextAction(
                        text = strings.staffReactivateAction,
                        onClick = { actions.onActivateStaff(staff) },
                    )
                }
            }
        }
    }
}
