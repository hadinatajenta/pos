package id.umkm.pos.pro.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.SectionHeading
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerFormSheet(
    form: CustomerFormState,
    isSubmitting: Boolean,
    actions: CustomersActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onCloseCustomerForm,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = PosSpacing.md)
                .padding(bottom = PosSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
        ) {
            SectionHeading(text = "New customer")
            OutlinedTextField(
                value = form.name,
                onValueChange = actions.onCustomerNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                isError = form.validationMessage != null,
                supportingText = form.validationMessage?.let { message -> { Text(message) } },
                singleLine = true,
            )
            OutlinedTextField(
                value = form.whatsappNumber,
                onValueChange = actions.onCustomerWhatsappChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("WhatsApp number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = actions.onCustomerNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                OutlinedButton(
                    onClick = actions.onCloseCustomerForm,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = "Cancel")
                }
                Button(
                    onClick = actions.onSubmitCustomerForm,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(PosSizes.iconSmall),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = "Save customer", style = LocalAppTextStyles.current.buttonLabel)
                    }
                }
            }
        }
    }
}
