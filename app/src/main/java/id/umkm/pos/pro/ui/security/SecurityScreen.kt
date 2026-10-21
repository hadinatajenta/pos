package id.umkm.pos.pro.ui.security

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.core.model.SecurityAuditEntry
import id.umkm.pos.pro.core.model.SecurityAuditResult
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.common.formatDateTime
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
internal fun SecurityScreen(
    state: SecurityUiState,
    actions: SecurityActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val headerBack: (() -> Unit)? = if (state.step == SecurityStep.OVERVIEW) {
        onBack
    } else {
        actions.onCloseStep
    }
    if (state.step != SecurityStep.OVERVIEW) {
        BackHandler(onBack = actions.onCloseStep)
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.settingsSecurityTitle,
            onBack = headerBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PosSpacing.md,
                    end = PosSpacing.md,
                    top = PosSpacing.sm,
                    bottom = PosSizes.listBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            SecurityFeedback(state = state, onDismiss = actions.onDismissFeedback)
            when (state.step) {
                SecurityStep.OVERVIEW -> {
                    SecurityStatusCard(state = state, actions = actions)
                    SecurityActionsCard(state = state, actions = actions)
                    SecurityAuditCard(state = state, actions = actions)
                }

                SecurityStep.ENABLE_PIN -> SecurityEnablePanel(state = state, actions = actions)
                SecurityStep.CHANGE_PIN -> SecurityChangePanel(state = state, actions = actions)
                SecurityStep.DISABLE_PIN -> SecurityDisablePanel(state = state, actions = actions)
                SecurityStep.REGENERATE_CODE -> SecurityRegeneratePanel(state = state, actions = actions)
                SecurityStep.RECOVERY_CODE -> SecurityRecoveryCodePanel(state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun SecurityFeedback(
    state: SecurityUiState,
    onDismiss: () -> Unit,
) {
    state.warningMessage?.let { warning ->
        SecurityFeedbackRow(
            iconRes = R.drawable.ic_triangle_alert,
            text = warning,
            containerColor = LocalPosSemanticPalette.current.warningContainer,
            contentColor = LocalPosSemanticPalette.current.warning,
            onDismiss = onDismiss,
        )
    }
    state.errorMessage?.let { error ->
        SecurityFeedbackRow(
            iconRes = R.drawable.ic_circle_alert,
            text = error,
            containerColor = LocalPosSemanticPalette.current.dangerContainer,
            contentColor = LocalPosSemanticPalette.current.danger,
            onDismiss = onDismiss,
        )
    }
    state.message?.let { message ->
        SecurityFeedbackRow(
            iconRes = R.drawable.ic_circle_check,
            text = message,
            containerColor = LocalPosSemanticPalette.current.successContainer,
            contentColor = LocalPosSemanticPalette.current.success,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SecurityFeedbackRow(
    @DrawableRes iconRes: Int,
    text: UiText,
    containerColor: Color,
    contentColor: Color,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = PosSpacing.md, top = PosSpacing.xs, bottom = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Text(
                text = text.asText(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = strings.actionClose,
                    modifier = Modifier.size(PosSizes.iconSmall),
                    tint = contentColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecurityStatusCard(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconMedium),
                tint = if (state.isPinEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.securityStatusTitle,
                    style = LocalAppTextStyles.current.sectionHeading,
                )
                val pinUpdatedAt = state.pinUpdatedAtMillis
                if (state.isPinEnabled && pinUpdatedAt != null) {
                    Text(
                        text = strings.securityStatusUpdatedAt(formatDate(pinUpdatedAt, strings)),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            StatusChip(
                iconRes = if (state.isPinEnabled) R.drawable.ic_circle_check else R.drawable.ic_circle_alert,
                label = if (state.isPinEnabled) {
                    strings.securityStatusEnabled
                } else {
                    strings.securityStatusDisabled
                },
                containerColor = if (state.isPinEnabled) palette.successContainer else palette.neutralContainer,
                contentColor = if (state.isPinEnabled) {
                    palette.successText
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (state.isPinEnabled) {
                val hasRecoveryCode = state.isRecoveryCodeConfigured
                StatusChip(
                    iconRes = if (hasRecoveryCode) R.drawable.ic_check else R.drawable.ic_triangle_alert,
                    label = if (hasRecoveryCode) {
                        strings.securityStatusRecoveryReady
                    } else {
                        strings.securityStatusRecoveryMissing
                    },
                    containerColor = if (hasRecoveryCode) palette.neutralContainer else palette.warningContainer,
                    contentColor = if (hasRecoveryCode) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        palette.warning
                    },
                )
            }
        }

        if (state.isSessionElevated) {
            Text(
                text = strings.securitySessionOpen,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = actions.onLockSession,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = strings.securitySessionLock,
                    style = LocalAppTextStyles.current.buttonLabel,
                )
            }
        }
    }
}

@Composable
private fun SecurityActionsCard(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        if (state.isPinEnabled) {
            PrimaryActionButton(
                text = strings.securityChangeAction,
                onClick = actions.onStartChangePin,
            )
            SecurityOutlinedButton(
                text = strings.securityRegenerateAction,
                iconRes = R.drawable.ic_refresh,
                onClick = actions.onStartRegenerateCode,
            )
            SecurityOutlinedButton(
                text = strings.securityDisableAction,
                iconRes = R.drawable.ic_circle_alert,
                onClick = actions.onStartDisablePin,
                contentColor = palette.warning,
            )
        } else {
            PrimaryActionButton(
                text = strings.securityEnableAction,
                iconRes = R.drawable.ic_lock,
                onClick = actions.onStartEnablePin,
            )
        }
    }
}

@Composable
private fun SecurityOutlinedButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PosSizes.touchTarget),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(PosSpacing.xs))
        Text(
            text = text,
            style = LocalAppTextStyles.current.buttonLabel,
            maxLines = 2,
        )
    }
}

@Composable
private fun SecurityEnablePanel(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.securityEnableIntro,
            style = LocalAppTextStyles.current.body,
        )
        Text(
            text = strings.securityEnableRecoveryNote,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecurityPinField(
            value = state.newPinInput,
            label = strings.securityNewPinLabel,
            error = state.newPinError,
            supportingHint = strings.securityPinLengthHint(state.minPinLength, state.maxPinLength),
            onValueChange = actions.onNewPinChange,
        )
        SecurityPinField(
            value = state.confirmPinInput,
            label = strings.securityConfirmPinLabel,
            error = state.confirmPinError,
            supportingHint = null,
            onValueChange = actions.onConfirmPinChange,
        )
        PrimaryActionButton(
            text = strings.securityEnableSubmit,
            iconRes = R.drawable.ic_lock,
            loading = state.isBusy,
            onClick = actions.onSubmitEnablePin,
        )
    }
}

@Composable
private fun SecurityChangePanel(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.securityChangeIntro,
            style = LocalAppTextStyles.current.body,
        )
        SecurityPinField(
            value = state.currentPinInput,
            label = strings.securityCurrentPinLabel,
            error = state.currentPinError,
            supportingHint = null,
            onValueChange = actions.onCurrentPinChange,
        )
        SecurityPinField(
            value = state.newPinInput,
            label = strings.securityNewPinLabel,
            error = state.newPinError,
            supportingHint = strings.securityPinLengthHint(state.minPinLength, state.maxPinLength),
            onValueChange = actions.onNewPinChange,
        )
        SecurityPinField(
            value = state.confirmPinInput,
            label = strings.securityConfirmPinLabel,
            error = state.confirmPinError,
            supportingHint = null,
            onValueChange = actions.onConfirmPinChange,
        )
        PrimaryActionButton(
            text = strings.securityChangeSubmit,
            loading = state.isBusy,
            onClick = actions.onSubmitChangePin,
        )
    }
}

@Composable
private fun SecurityDisablePanel(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.securityDisableIntro,
            style = LocalAppTextStyles.current.body,
        )
        SecurityPinField(
            value = state.currentPinInput,
            label = strings.securityCurrentPinLabel,
            error = state.currentPinError,
            supportingHint = null,
            onValueChange = actions.onCurrentPinChange,
        )
        PrimaryActionButton(
            text = strings.securityDisableSubmit,
            loading = state.isBusy,
            onClick = actions.onSubmitDisablePin,
        )
    }
}

@Composable
private fun SecurityRegeneratePanel(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.securityRegenerateIntro,
            style = LocalAppTextStyles.current.body,
        )
        SecurityPinField(
            value = state.currentPinInput,
            label = strings.securityCurrentPinLabel,
            error = state.currentPinError,
            supportingHint = null,
            onValueChange = actions.onCurrentPinChange,
        )
        PrimaryActionButton(
            text = strings.securityRegenerateSubmit,
            iconRes = R.drawable.ic_refresh,
            loading = state.isBusy,
            onClick = actions.onSubmitRegenerateCode,
        )
    }
}

@Composable
private fun SecurityRecoveryCodePanel(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val launchPrint = rememberRecoveryCodePrintLauncher()
    val recoveryCode = state.recoveryCode

    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconMedium),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = strings.securityRecoveryCodeTitle,
                style = LocalAppTextStyles.current.sectionHeading,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = strings.securityRecoveryCodeOnceWarning,
            style = LocalAppTextStyles.current.body,
            color = palette.warningText,
        )

        if (recoveryCode != null) {
            Surface(
                color = palette.neutralContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = recoveryCode,
                    style = LocalAppTextStyles.current.financialValue.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PosSpacing.md),
                )
            }

            OutlinedButton(
                onClick = {
                    launchPrint(
                        RecoveryCodePrintRequest(
                            documentName = strings.securityRecoveryCodePrintJobName,
                            title = strings.securityRecoveryCodeTitle,
                            codeLabel = strings.securityPrintCodeLabel,
                            code = recoveryCode,
                            instructions = listOf(
                                strings.securityPrintInstructionOne,
                                strings.securityPrintInstructionTwo,
                                strings.securityPrintInstructionThree,
                            ),
                            footer = strings.securityPrintFooter(
                                formatDate(System.currentTimeMillis(), strings),
                            ),
                            renderFailureMessage = strings.receiptRenderFailed,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PosSizes.touchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_printer),
                    contentDescription = null,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = strings.securityRecoveryCodePrint,
                    style = LocalAppTextStyles.current.buttonLabel,
                    maxLines = 2,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PosSizes.touchTarget)
                .clickable {
                    actions.onRecoveryCodeAcknowledgedChange(!state.isRecoveryCodeAcknowledged)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.isRecoveryCodeAcknowledged,
                onCheckedChange = actions.onRecoveryCodeAcknowledgedChange,
            )
            Text(
                text = strings.securityRecoveryCodeAcknowledge,
                style = LocalAppTextStyles.current.body,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = PosSpacing.sm),
            )
        }

        PrimaryActionButton(
            text = strings.securityRecoveryCodeDone,
            enabled = state.isRecoveryCodeAcknowledged,
            onClick = actions.onCloseStep,
        )
    }
}

@Composable
private fun SecurityAuditCard(
    state: SecurityUiState,
    actions: SecurityActions,
) {
    val strings = LocalPosStrings.current
    PosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconMedium),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.securityAuditTitle,
                    style = LocalAppTextStyles.current.sectionHeading,
                )
                Text(
                    text = strings.securityAuditLatest(state.auditEntries.size),
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = actions.onRefreshAudit) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = strings.actionRetry,
                    modifier = Modifier.size(PosSizes.iconMedium),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (state.auditEntries.isEmpty()) {
            Text(
                text = strings.securityAuditEmpty,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.auditEntries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    )
                }
                SecurityAuditRow(entry = entry)
            }
        }
    }
}

@Composable
private fun SecurityAuditRow(entry: SecurityAuditEntry) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val descriptor = auditVisuals(entry.result, palette)
    val action = CriticalAction.fromId(entry.actionId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PosSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Icon(
            painter = painterResource(descriptor.iconRes),
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
            tint = descriptor.tint,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
        ) {
            Text(
                text = auditResultLabel(entry.result, strings),
                style = LocalAppTextStyles.current.body,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = action?.let { strings.securityAuditActionLabel(it) } ?: entry.actionId,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDateTime(entry.occurredAtMillis, strings),
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecurityPinField(
    value: String,
    label: String,
    error: UiText?,
    supportingHint: String?,
    onValueChange: (String) -> Unit,
) {
    val supportingText: (@Composable () -> Unit)? = when {
        error != null -> {
            { Text(text = error.asText()) }
        }

        supportingHint != null -> {
            { Text(text = supportingHint) }
        }

        else -> null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        isError = error != null,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        supportingText = supportingText,
        modifier = Modifier.fillMaxWidth(),
    )
}

private data class AuditVisuals(@DrawableRes val iconRes: Int, val tint: Color)

private fun auditResultLabel(result: SecurityAuditResult, strings: PosStrings): String = when (result) {
    SecurityAuditResult.GRANTED -> strings.securityAuditResultGranted
    SecurityAuditResult.DENIED -> strings.securityAuditResultDenied
    SecurityAuditResult.LOCKED -> strings.securityAuditResultLocked
    SecurityAuditResult.RECOVERY_USED -> strings.securityAuditResultRecoveryUsed
    SecurityAuditResult.PIN_ENABLED -> strings.securityAuditResultPinEnabled
    SecurityAuditResult.PIN_CHANGED -> strings.securityAuditResultPinChanged
    SecurityAuditResult.PIN_DISABLED -> strings.securityAuditResultPinDisabled
    SecurityAuditResult.RECOVERY_CODE_REGENERATED -> strings.securityAuditResultRecoveryRegenerated
    SecurityAuditResult.RESTORE_RESET_PIN -> strings.securityAuditResultRestoreReset
}

private fun auditVisuals(
    result: SecurityAuditResult,
    palette: PosSemanticPalette,
): AuditVisuals = when (result) {
    SecurityAuditResult.GRANTED -> AuditVisuals(R.drawable.ic_circle_check, palette.success)
    SecurityAuditResult.DENIED -> AuditVisuals(R.drawable.ic_circle_x, palette.danger)
    SecurityAuditResult.LOCKED -> AuditVisuals(R.drawable.ic_lock, palette.warning)
    SecurityAuditResult.RECOVERY_USED -> AuditVisuals(R.drawable.ic_check, palette.warning)
    SecurityAuditResult.PIN_ENABLED -> AuditVisuals(R.drawable.ic_circle_check, palette.success)
    SecurityAuditResult.PIN_CHANGED -> AuditVisuals(R.drawable.ic_check, palette.success)
    SecurityAuditResult.PIN_DISABLED -> AuditVisuals(R.drawable.ic_circle_alert, palette.warning)
    SecurityAuditResult.RECOVERY_CODE_REGENERATED -> AuditVisuals(R.drawable.ic_refresh, palette.success)
    SecurityAuditResult.RESTORE_RESET_PIN -> AuditVisuals(R.drawable.ic_refresh, palette.warning)
}
