package id.umkm.pos.pro.ui.cashier

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.domain.qris.QrisPayload
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.paymentMethodLabel
import id.umkm.pos.pro.ui.common.renderQrBitmap
import id.umkm.pos.pro.ui.components.AutoSizeNumberText
import id.umkm.pos.pro.ui.components.PosCard
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.products.ProductPalette
import id.umkm.pos.pro.ui.products.ProductRadii
import id.umkm.pos.pro.ui.products.RupiahInputVisualTransformation
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PAGE_HORIZONTAL_PADDING = 20.dp
private val PAGE_BOTTOM_PADDING = 24.dp
private val PAGE_GAP = 16.dp
private val TOTAL_CARD_CORNER = RoundedCornerShape(24.dp)
private val TOTAL_CARD_PADDING = 24.dp
private val TOTAL_CARD_ELEVATION = 4.dp
private val METHOD_CARD_CORNER = RoundedCornerShape(16.dp)
private val METHOD_CARD_MIN_HEIGHT = 60.dp
private val METHOD_CARD_HORIZONTAL_PADDING = 16.dp
private val METHOD_CARD_ELEVATION = 2.dp
private val METHOD_ICON_BOX_SIZE = 38.dp
private val QUICK_AMOUNT_MIN_HEIGHT = 44.dp
private val FOOTER_PADDING = 16.dp
private val FOOTER_ELEVATION = 8.dp
private val PAY_BUTTON_HEIGHT = 54.dp
private val QR_PAYMENT_SIZE = 220.dp
private const val QR_RENDER_SIZE_PX = 720
private val TOTAL_AMOUNT_TEXT_STYLE = TextStyle(
    fontSize = 32.sp,
    lineHeight = 38.sp,
    fontWeight = FontWeight.Black,
    fontFeatureSettings = "tnum",
    textAlign = TextAlign.Center,
)

@Composable
internal fun CashierPaymentPage(
    state: CashierUiState,
    actions: CashierActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    var showAllMethods by remember { mutableStateOf(false) }
    var isConfirmationVisible by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        CashierPageHeader(
            title = strings.cashierPaymentTitle,
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PAGE_HORIZONTAL_PADDING)
                .padding(bottom = PAGE_BOTTOM_PADDING),
            verticalArrangement = Arrangement.spacedBy(PAGE_GAP),
        ) {
            PaymentTotalCard(grandTotal = state.grandTotal)
            Text(
                text = strings.cashierPaymentMethodLabel,
                color = ProductPalette.Ink,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentMethodOption(
                    method = PaymentMethod.CASH,
                    selectedMethod = state.paymentDraft.method,
                    enabled = true,
                    iconRes = R.drawable.ic_banknote,
                    onSelect = actions.onPaymentMethodChange,
                )
                PaymentMethodOption(
                    method = PaymentMethod.QRIS,
                    selectedMethod = state.paymentDraft.method,
                    enabled = state.qrisPayload != null,
                    iconRes = R.drawable.ic_payment_qris,
                    onSelect = actions.onPaymentMethodChange,
                )
                if (state.qrisPayload == null) {
                    Text(
                        text = strings.cashierQrisNotConfigured,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = ProductPalette.MutedText,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                if (showAllMethods) {
                    PaymentMethodOption(
                        method = PaymentMethod.TRANSFER,
                        selectedMethod = state.paymentDraft.method,
                        enabled = true,
                        iconRes = R.drawable.ic_payment_transfer,
                        onSelect = actions.onPaymentMethodChange,
                    )
                    PaymentMethodOption(
                        method = PaymentMethod.DEBIT,
                        selectedMethod = state.paymentDraft.method,
                        enabled = true,
                        iconRes = R.drawable.ic_payment_debit,
                        onSelect = actions.onPaymentMethodChange,
                    )
                    PaymentMethodOption(
                        method = PaymentMethod.CREDIT,
                        selectedMethod = state.paymentDraft.method,
                        enabled = true,
                        iconRes = R.drawable.ic_payment_credit,
                        onSelect = actions.onPaymentMethodChange,
                    )
                }
                TextButton(
                    onClick = { showAllMethods = !showAllMethods },
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(
                        text = if (showAllMethods) {
                            strings.cashierPaymentHideOptions
                        } else {
                            strings.cashierPaymentMoreOptions
                        },
                        color = ProductPalette.Brand,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (state.paymentDraft.method == PaymentMethod.CASH) {
                CashPaymentSection(state = state, actions = actions)
            }
        }
        PaymentFooter(
            state = state,
            onPay = { isConfirmationVisible = true },
        )
    }
    if (isConfirmationVisible) {
        PaymentConfirmationSheet(
            state = state,
            onDismiss = { isConfirmationVisible = false },
            onConfirm = {
                isConfirmationVisible = false
                actions.onConfirmCharge()
            },
        )
    }
}

@Composable
private fun PaymentTotalCard(grandTotal: Long) {
    val strings = LocalPosStrings.current
    Surface(
        color = ProductPalette.Surface,
        contentColor = ProductPalette.Ink,
        shape = TOTAL_CARD_CORNER,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = TOTAL_CARD_ELEVATION,
                shape = TOTAL_CARD_CORNER,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TOTAL_CARD_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = strings.cashierTotalDue,
                style = LocalAppTextStyles.current.supportingLabel,
                color = ProductPalette.MutedText,
                textAlign = TextAlign.Center,
            )
            AutoSizeNumberText(
                text = Rupiah.format(grandTotal),
                style = TOTAL_AMOUNT_TEXT_STYLE,
                color = ProductPalette.Ink,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PaymentMethodOption(
    method: PaymentMethod,
    selectedMethod: PaymentMethod,
    enabled: Boolean,
    iconRes: Int,
    onSelect: (PaymentMethod) -> Unit,
) {
    val strings = LocalPosStrings.current
    val isSelected = method == selectedMethod
    Surface(
        color = if (isSelected) ProductPalette.SafeContainer else ProductPalette.Surface,
        contentColor = if (enabled) ProductPalette.Ink else ProductPalette.MutedText,
        shape = METHOD_CARD_CORNER,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = METHOD_CARD_ELEVATION,
                shape = METHOD_CARD_CORNER,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onSelect(method) },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = METHOD_CARD_MIN_HEIGHT)
                .padding(horizontal = METHOD_CARD_HORIZONTAL_PADDING, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = enabled,
            )
            Box(
                modifier = Modifier
                    .size(METHOD_ICON_BOX_SIZE)
                    .clip(CircleShape)
                    .background(ProductPalette.Background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (enabled) ProductPalette.Brand else ProductPalette.MutedText,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = paymentMethodLabel(method, strings),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CashPaymentSection(
    state: CashierUiState,
    actions: CashierActions,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }
    val paidAmount = state.paymentDraft.amountReceivedInput.toLongOrNull()
    val applyQuickAmount: (Long) -> Unit = { amount ->
        actions.onAmountReceivedChange(amount.toString())
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    val quickAmounts = remember(state.grandTotal) { quickCashSuggestions(state.grandTotal) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.paymentDraft.amountReceivedInput,
            onValueChange = { input -> actions.onAmountReceivedChange(input.filter(Char::isDigit)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester),
            label = { Text(text = strings.cashierAmountReceived) },
            singleLine = true,
            visualTransformation = RupiahInputVisualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                QuickAmountPill(
                    label = strings.cashierExactMoney,
                    onClick = { applyQuickAmount(state.grandTotal) },
                )
            }
            items(quickAmounts) { amount ->
                QuickAmountPill(
                    label = Rupiah.format(amount),
                    onClick = { applyQuickAmount(amount) },
                )
            }
            item {
                QuickAmountPill(
                    label = strings.cashierEnterAmountOption,
                    onClick = {
                        amountFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                )
            }
        }
        if (paidAmount != null) {
            val isEnough = paidAmount >= state.grandTotal
            val difference = if (isEnough) {
                paidAmount - state.grandTotal
            } else {
                state.grandTotal - paidAmount
            }
            Text(
                text = if (isEnough) {
                    "${strings.cashierChange}: ${Rupiah.format(difference)}"
                } else {
                    "${strings.cashierInsufficientCash}: ${Rupiah.format(difference)}"
                },
                style = LocalAppTextStyles.current.financialValue.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
                color = if (isEnough) palette.successText else palette.dangerText,
            )
        }
    }
}

@Composable
private fun QuickAmountPill(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ProductRadii.pill,
        color = ProductPalette.Surface,
        contentColor = ProductPalette.Ink,
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = QUICK_AMOUNT_MIN_HEIGHT)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PaymentFooter(
    state: CashierUiState,
    onPay: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val submitErrorMessage = state.submitErrorMessage?.asTextOrNull()
    Surface(
        color = ProductPalette.Surface,
        shadowElevation = FOOTER_ELEVATION,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FOOTER_PADDING),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (submitErrorMessage != null) {
                Text(
                    text = submitErrorMessage,
                    style = LocalAppTextStyles.current.supportingLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PrimaryActionButton(
                text = strings.cashierPayNow,
                onClick = onPay,
                enabled = canConfirmPayment(state),
                loading = state.isSubmitting,
                modifier = Modifier.height(PAY_BUTTON_HEIGHT),
            )
        }
    }
}

private fun canConfirmPayment(state: CashierUiState): Boolean {
    if (state.lineCount == 0 || state.isSubmitting) return false
    return when (state.paymentDraft.method) {
        PaymentMethod.CASH -> {
            val paidAmount = state.paymentDraft.amountReceivedInput.toLongOrNull()
            paidAmount != null && paidAmount >= state.grandTotal
        }

        PaymentMethod.QRIS -> state.qrisPayload != null
        PaymentMethod.TRANSFER, PaymentMethod.DEBIT, PaymentMethod.CREDIT -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentConfirmationSheet(
    state: CashierUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val paidAmount = state.paymentDraft.amountReceivedInput.toLongOrNull()
    val changeAmount = paidAmount?.let { paid -> (paid - state.grandTotal).coerceAtLeast(0L) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = ProductPalette.Surface,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1D5DB)),
                )
            }
            Text(
                text = strings.cashierConfirmSheetTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                color = ProductPalette.Ink,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            ConfirmationAmountRow(
                label = strings.cashierTotalDue,
                value = Rupiah.format(state.grandTotal),
            )
            if (state.paymentDraft.method == PaymentMethod.CASH && paidAmount != null) {
                ConfirmationAmountRow(
                    label = strings.cashierCashReceived,
                    value = Rupiah.format(paidAmount),
                )
            }
            CashierDashedDivider(modifier = Modifier.padding(vertical = 16.dp))
            if (state.paymentDraft.method == PaymentMethod.CASH) {
                Surface(
                    color = ProductPalette.SafeContainer,
                    contentColor = ProductPalette.Brand,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = strings.cashierChange,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = Rupiah.format(changeAmount ?: 0L),
                            fontSize = 22.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (state.paymentDraft.method == PaymentMethod.QRIS) {
                QrisPaymentSection(
                    payload = state.qrisPayload,
                    amountRupiah = state.grandTotal,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProductPalette.Brand,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = strings.cashierConfirmAndPay,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = strings.actionCancel,
                    color = ProductPalette.MutedText,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ConfirmationAmountRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = ProductPalette.MutedText,
        )
        Text(
            text = value,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ProductPalette.Ink,
        )
    }
}

internal fun quickCashSuggestions(grandTotal: Long): List<Long> {
    if (grandTotal <= 0L) return emptyList()
    val steps = listOf(10_000L, 50_000L, 100_000L, 200_000L)
    return steps
        .map { step -> ceilTo(grandTotal, step) }
        .filter { amount -> amount > grandTotal }
        .distinct()
        .sorted()
        .take(3)
}

private fun ceilTo(value: Long, step: Long): Long =
    if (value <= 0L) step else ((value + step - 1L) / step) * step

@Composable
internal fun QrisPaymentSection(
    payload: String?,
    amountRupiah: Long,
) {
    val palette = LocalPosSemanticPalette.current
    val strings = LocalPosStrings.current
    if (payload == null) {
        Text(
            text = strings.cashierQrisNotConfigured,
            style = LocalAppTextStyles.current.supportingLabel,
            color = palette.danger,
        )
        return
    }
    val qrImage by produceState<ImageBitmap?>(initialValue = null, payload, amountRupiah) {
        value = withContext(Dispatchers.Default) {
            QrisPayload.withAmount(payload, amountRupiah)
                ?.let { dynamicPayload -> renderQrBitmap(dynamicPayload, QR_RENDER_SIZE_PX)?.asImageBitmap() }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosCard(modifier = Modifier.size(QR_PAYMENT_SIZE)) {
            val image = qrImage
            if (image == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.cashierPreparingQris,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Image(
                    bitmap = image,
                    contentDescription = strings.cashierQrisDescription,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = Rupiah.format(amountRupiah),
            style = LocalAppTextStyles.current.financialValue,
        )
        Text(
            text = strings.cashierAmountAutoFilled,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
