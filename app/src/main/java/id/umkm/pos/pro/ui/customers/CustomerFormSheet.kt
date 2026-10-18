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
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PosSectionHeader
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerFormSheet(
    form: CustomerFormState,
    isSubmitting: Boolean,
    actions: CustomersActions,
) {
    val context = LocalContext.current
    val strings = LocalPosStrings.current
    var contactMessage by remember { mutableStateOf<UiText?>(null) }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
        if (contactUri == null) return@rememberLauncherForActivityResult
        val contact = readPickedContact(context, contactUri)
        if (contact == null) {
            contactMessage = UiText.from { strings -> strings.customersContactUnreadable }
        } else {
            contactMessage = null
            actions.onCustomerNameChange(contact.name)
            contact.whatsappNumber?.let { number -> actions.onCustomerWhatsappChange(number) }
        }
    }
    val permissionRequester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            contactPicker.launch(null)
        } else {
            contactMessage = UiText.from { strings -> strings.customersContactPermissionDenied }
        }
    }
    val onPickContact: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            contactPicker.launch(null)
        } else {
            permissionRequester.launch(Manifest.permission.READ_CONTACTS)
        }
    }
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
            PosSectionHeader(title = strings.customersNewCustomer)
            PosCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onPickContact,
                    modifier = Modifier.fillMaxWidth().heightIn(min = PosSizes.touchTarget),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_users),
                        contentDescription = null,
                        modifier = Modifier.size(PosSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(PosSpacing.xs))
                    Text(text = strings.customersPickFromContacts)
                }
                contactMessage?.let { message ->
                    Text(
                        text = message.asText(),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = LocalPosSemanticPalette.current.danger,
                    )
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = actions.onCustomerNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.customersName) },
                    isError = form.validationMessage != null,
                    supportingText = form.validationMessage?.let { message -> { Text(message.asText()) } },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = form.whatsappNumber,
                    onValueChange = actions.onCustomerWhatsappChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.customersPhoneWhatsApp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                OutlinedTextField(
                    value = form.notes,
                    onValueChange = actions.onCustomerNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.customersNote) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                OutlinedButton(
                    onClick = actions.onCloseCustomerForm,
                    modifier = Modifier.weight(1f).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = strings.actionCancel)
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
                        Text(text = strings.actionSave, style = LocalAppTextStyles.current.buttonLabel)
                    }
                }
            }
        }
    }
}
