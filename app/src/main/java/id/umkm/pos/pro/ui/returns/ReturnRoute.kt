package id.umkm.pos.pro.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.domain.usecase.ProcessReturnUseCase
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PreviewAppContainerProvider
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asText
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R
import androidx.compose.runtime.getValue

private val RETURN_CONTENT_MAX_WIDTH = 640.dp
private val RETURN_QUANTITY_FIELD_WIDTH = 120.dp

@Composable
fun ReturnRoute(
    transactionId: Long,
    onReturnProcessed: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val container = LocalAppContainer.current
    val returnViewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                ReturnViewModel(
                    transactionRepository = container.transactionRepository,
                    productRepository = container.productRepository,
                    processReturnUseCase = ProcessReturnUseCase(
                        transactionRepository = container.transactionRepository,
                        returnRepository = container.returnRepository,
                        productRepository = container.productRepository,
                        clock = container.clock,
                    ),
                )
            }
        }
    }
    val viewModel: ReturnViewModel = viewModel(factory = returnViewModelFactory)
    LaunchedEffect(transactionId) {
        viewModel.onScreenShown(transactionId)
    }
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) { ReturnActions.from(viewModel) }
    ReturnScreen(
        state = state,
        actions = actions,
        onReturnProcessed = {
            viewModel.onProcessed()
            onReturnProcessed()
        },
        modifier = modifier,
        onBack = onBack,
    )
}

@Composable
internal fun ReturnScreen(
    state: ReturnUiState,
    actions: ReturnActions,
    onReturnProcessed: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val strings = LocalPosStrings.current
    val loadError = state.loadError
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.returnTitle,
            supportingText = state.invoiceNo
                ?.let { invoiceNo -> strings.returnInvoiceLabel(invoiceNo) }
                ?: strings.returnItemsTitle,
            onBack = onBack,
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = RETURN_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .imePadding(),
            ) {
                when {
                    state.isLoading -> LoadingState(modifier = Modifier.weight(1f), label = strings.returnLoading)
                    loadError != null -> ErrorState(
                        error = loadError,
                        onRetry = actions.onRetry,
                        modifier = Modifier.weight(1f),
                    )

                    state.isProcessed -> ReturnSuccessState(
                        onReturnProcessed = onReturnProcessed,
                        modifier = Modifier.weight(1f),
                    )

                    else -> ReturnForm(state = state, actions = actions, modifier = Modifier.weight(1f))
                }
            }
        }
    }

}

@Composable
private fun ReturnForm(
    state: ReturnUiState,
    actions: ReturnActions,
    modifier: Modifier,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        Text(text = strings.returnItemsTitle, style = LocalAppTextStyles.current.sectionHeading)
        Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
            state.lines.forEach { line ->
                ReturnLineRow(
                    line = line,
                    errorMessage = state.lineErrors[line.transactionItemId],
                    isEnabled = !state.isSubmitting,
                    actions = actions,
                )
            }
        }
        val formError = state.formError
        if (formError != null) {
            InlineError(message = formError)
        }
        OutlinedTextField(
            value = state.note,
            onValueChange = actions.onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            label = { Text(strings.returnNoteLabel) },
            minLines = 2,
        )
        val submitError = state.submitError
        if (submitError != null) {
            InlineError(message = submitError)
        }
        Button(
            onClick = actions.onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PosSizes.touchTarget),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PosSizes.iconSmall),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(text = strings.returnSubmittingButton, style = LocalAppTextStyles.current.buttonLabel)
            } else {
                Text(text = strings.returnSubmitButton, style = LocalAppTextStyles.current.buttonLabel)
            }
        }
        Spacer(modifier = Modifier.height(PosSizes.listBottomPadding))
    }

}

@Composable
private fun ReturnLineRow(
    line: ReturnLineInput,
    errorMessage: UiText?,
    isEnabled: Boolean,
    actions: ReturnActions,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(PosSpacing.md),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.xxs),
                ) {
                    Text(
                        text = line.itemName.asText(),
                        style = LocalAppTextStyles.current.body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!line.variantName.isNullOrBlank()) {
                        Text(
                            text = line.variantName,
                            style = LocalAppTextStyles.current.supportingLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = strings.returnPurchasedQty(formatQuantity(line.purchasedQty)),
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = line.returnQty,
                    onValueChange = { value -> actions.onReturnQtyChange(line.transactionItemId, value) },
                    modifier = Modifier.width(RETURN_QUANTITY_FIELD_WIDTH),
                    enabled = isEnabled,
                    singleLine = true,
                    isError = errorMessage != null,
                    label = { Text(strings.returnQtyLabel) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage.asText(),
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = PosSpacing.md),
            )
        }
    }
}

@Composable
private fun InlineError(message: UiText) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_circle_alert),
            contentDescription = null,
            modifier = Modifier.size(PosSizes.iconSmall),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = message.asText(),
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ReturnSuccessState(
    onReturnProcessed: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(PosSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_circle_check),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = strings.returnSuccessTitle,
            style = LocalAppTextStyles.current.sectionHeading,
            textAlign = TextAlign.Center,
        )
        Text(
            text = strings.returnSuccessMsg,
            style = LocalAppTextStyles.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(PosSpacing.xs))
        Button(
            onClick = onReturnProcessed,
            modifier = Modifier.heightIn(min = PosSizes.touchTarget),
        ) {
            Text(text = strings.actionDone, style = LocalAppTextStyles.current.buttonLabel)
        }
    }
}


private fun previewLine(
    transactionItemId: Long,
    itemName: String,
    variantName: String?,
    purchasedQty: Double,
    returnQty: String,
): ReturnLineInput = ReturnLineInput(
    transactionItemId = transactionItemId,
    itemName = UiText.literal(itemName),
    variantName = variantName,
    purchasedQty = purchasedQty,
    returnQty = returnQty,
)

private fun previewActions(): ReturnActions = ReturnActions(
    onReturnQtyChange = { _, _ -> },
    onNoteChange = {},
    onRetry = {},
    onSubmit = {},
)

private fun previewReturnState(): ReturnUiState = ReturnUiState(
    invoiceNo = "INV-20261016-0001",
    isLoading = false,
    lines = listOf(
        previewLine(1L, "Kopi Susu Gula Aren", "Dus", 40.0, "40"),
        previewLine(2L, "Air Mineral 600ml", null, 3.0, "1"),
    ),
)

@Preview(name = "Return form", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReturnFormPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ReturnScreen(
                state = previewReturnState(),
                actions = previewActions(),
                onReturnProcessed = {},
            )
        }
    }
}

@Preview(name = "Return progress", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReturnProgressPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ReturnScreen(
                state = previewReturnState().copy(isSubmitting = true),
                actions = previewActions(),
                onReturnProcessed = {},
            )
        }
    }
}

@Preview(name = "Return success", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReturnSuccessPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ReturnScreen(
                state = previewReturnState().copy(isProcessed = true),
                actions = previewActions(),
                onReturnProcessed = {},
            )
        }
    }
}

@Preview(name = "Return error", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReturnErrorPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            ReturnScreen(
                state = ReturnUiState(
                    isLoading = false,
                    loadError = AppError.NotFound("This sale could not be found."),
                ),
                actions = previewActions(),
                onReturnProcessed = {},
            )
        }
    }
}
