package id.umkm.pos.pro.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings

@Composable
fun SecurityGateHost(gate: SecurityGateController) {
    val strings = LocalPosStrings.current
    if (gate.pendingAction == null) return

    var recoveryMode by remember(gate.pendingAction) { mutableStateOf(false) }
    var pin by remember(gate.pendingAction) { mutableStateOf("") }
    var code by remember(gate.pendingAction) { mutableStateOf("") }

    val pinMode = !recoveryMode
    val canSubmit = if (pinMode) pin.isNotEmpty() else code.isNotBlank()
    val feedback = gate.feedback

    AlertDialog(
        onDismissRequest = gate::cancel,
        title = {
            Text(if (pinMode) strings.securityGateTitle else strings.securityGateRecoveryTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (pinMode) {
                        strings.securityGateMessage(gate.pendingLabel)
                    } else {
                        strings.securityGateRecoveryMessage
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (pinMode) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value -> pin = value.filter(Char::isDigit).take(8) },
                        label = { Text(strings.securityGatePinLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { value -> code = value },
                        label = { Text(strings.securityGateRecoveryLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                feedback?.let { active ->
                    Text(
                        text = feedbackText(active, strings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(
                    onClick = {
                        recoveryMode = !recoveryMode
                        gate.clearFeedback()
                    },
                ) {
                    Text(
                        if (pinMode) strings.securityGateUseRecovery else strings.securityGateBackToPin,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pinMode) {
                        gate.submitPin(pin)
                    } else {
                        gate.submitRecoveryCode(code)
                    }
                },
                enabled = canSubmit,
            ) {
                Text(
                    if (pinMode) strings.securityGateUnlock else strings.securityGateRecoveryConfirm,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = gate::cancel) {
                Text(strings.actionCancel)
            }
        },
    )
}

private fun feedbackText(feedback: GateFeedback, strings: PosStrings): String = when (feedback) {
    is GateFeedback.WrongPin -> buildString {
        append(strings.securityGateWrongPin(feedback.remainingAttempts))
        if (feedback.retryDelaySeconds > 0) {
            append(' ')
            append(strings.securityGateRetryIn(feedback.retryDelaySeconds))
        }
    }

    is GateFeedback.RetryLater -> strings.securityGateRetryIn(feedback.seconds)
    is GateFeedback.Locked -> strings.securityGateLockedFor(feedback.minutes)
    GateFeedback.RecoveryInvalid -> strings.securityGateRecoveryInvalid
    GateFeedback.RecoveryUnavailable -> strings.securityGateRecoveryUnavailable
    GateFeedback.Unavailable -> strings.securityGateUnavailable
}
