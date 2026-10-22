package id.umkm.pos.pro.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSpacing

const val STAFF_MORE_ACTION_TEST_TAG_PREFIX = "staff-more-"
const val STAFF_AVATAR_TEST_TAG_PREFIX = "staff-avatar-"

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
            bottom = 96.dp,
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
                StaffSectionTitle(text = strings.staffActiveSection(activeStaff.size))
            }
            items(activeStaff, key = { staff -> "active-${staff.id}" }) { staff ->
                StaffCard(
                    staff = staff,
                    isOnDuty = staff.id == state.activeStaffId,
                    actions = actions,
                )
            }
        }
        if (inactiveStaff.isNotEmpty()) {
            item(key = "staff-inactive-header") {
                StaffSectionTitle(text = strings.staffInactiveSection(inactiveStaff.size))
            }
            items(inactiveStaff, key = { staff -> "inactive-${staff.id}" }) { staff ->
                StaffCard(
                    staff = staff,
                    isOnDuty = false,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun StaffSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = PosSpacing.sm, bottom = PosSpacing.xxs),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffCard(
    staff: Staff,
    isOnDuty: Boolean,
    actions: StaffActions,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(STAFF_ROW_TEST_TAG_PREFIX + staff.id),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            StaffAvatar(
                modifier = Modifier.testTag(STAFF_AVATAR_TEST_TAG_PREFIX + staff.id),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
            ) {
                Text(
                    text = staff.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
                ) {
                    StaffBadge(
                        label = if (staff.isActive) strings.staffActiveLabel else strings.staffInactiveLabel,
                        containerColor = if (staff.isActive) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            palette.neutralContainer
                        },
                        contentColor = if (staff.isActive) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (isOnDuty) {
                        StaffBadge(
                            label = strings.staffOnDutyLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            StaffMoreMenu(staff = staff, actions = actions)
        }
    }
}

@Composable
private fun StaffBadge(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun StaffMoreMenu(
    staff: Staff,
    actions: StaffActions,
) {
    val strings = LocalPosStrings.current
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(STAFF_MORE_ACTION_TEST_TAG_PREFIX + staff.id),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ellipsis_vertical),
                contentDescription = strings.staffMoreOptions,
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(strings.actionEdit) },
                onClick = {
                    isExpanded = false
                    actions.onOpenRenameEditor(staff)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (staff.isActive) {
                            strings.staffDeactivateAction
                        } else {
                            strings.staffReactivateAction
                        },
                    )
                },
                onClick = {
                    isExpanded = false
                    if (staff.isActive) {
                        actions.onRequestDeactivate(staff)
                    } else {
                        actions.onActivateStaff(staff)
                    }
                },
            )
        }
    }
}
