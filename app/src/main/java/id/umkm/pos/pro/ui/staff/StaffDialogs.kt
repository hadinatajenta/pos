package id.umkm.pos.pro.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.components.SecondaryTextAction
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val STAFF_EDITOR_MAX_WIDTH = 440.dp

@Composable
internal fun StaffEditorDialog(
    editor: StaffEditorState,
    isSubmitting: Boolean,
    actions: StaffActions,
) {
    val strings = LocalPosStrings.current
    val errorText = editor.error?.resolve(strings)

    Dialog(
        onDismissRequest = actions.onDismissEditor,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(PosSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = STAFF_EDITOR_MAX_WIDTH)
                    .fillMaxWidth()
                    .testTag(STAFF_EDITOR_DIALOG_TEST_TAG),
            ) {
                Column(
                    modifier = Modifier
                        .padding(PosSpacing.lg)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
                ) {
                    Text(
                        text = if (editor.isEditing) strings.staffEditAction else strings.staffAddAction,
                        style = LocalAppTextStyles.current.sectionHeading,
                    )
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = actions.onNameChange,
                        label = { Text(text = strings.staffNameLabel) },
                        placeholder = { Text(text = strings.staffNamePlaceholder) },
                        singleLine = true,
                        isError = errorText != null,
                        supportingText = if (errorText != null) {
                            { Text(text = errorText) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(STAFF_NAME_FIELD_TEST_TAG),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                    ) {
                        SecondaryTextAction(
                            text = strings.actionCancel,
                            onClick = actions.onDismissEditor,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryActionButton(
                            text = strings.actionSave,
                            onClick = actions.onSaveEditor,
                            enabled = !isSubmitting,
                            loading = isSubmitting,
                            iconRes = R.drawable.ic_check,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(STAFF_EDITOR_SAVE_TEST_TAG),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StaffDeactivateDialog(
    staff: Staff,
    actions: StaffActions,
) {
    val strings = LocalPosStrings.current
    AlertDialog(
        onDismissRequest = actions.onDismissDeactivate,
        title = { Text(text = strings.staffDeactivateConfirmTitle) },
        text = { Text(text = strings.staffDeactivateConfirmMessage(staff.name)) },
        confirmButton = {
            TextButton(
                onClick = actions.onConfirmDeactivate,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.staffDeactivateAction)
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onDismissDeactivate,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionCancel)
            }
        },
    )
}
