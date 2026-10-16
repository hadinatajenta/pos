package id.umkm.pos.pro.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.core.contract.AdPlacement
import id.umkm.pos.pro.core.contract.UiState
import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.domain.usecase.RecordDebtPaymentUseCase
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.monetization.admob.BannerAdSlot
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.ErrorState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.OfflineNotice
import id.umkm.pos.pro.ui.common.PreviewAppContainerProvider
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.common.StatusChip
import id.umkm.pos.pro.ui.common.UiStateContent
import id.umkm.pos.pro.ui.common.formatDate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

@Composable
fun CustomersRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val customersViewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                CustomersViewModel(
                    customerRepository = container.customerRepository,
                    transactionRepository = container.transactionRepository,
                    recordDebtPaymentUseCase = RecordDebtPaymentUseCase(
                        transactionRepository = container.transactionRepository,
                        clock = container.clock,
                    ),
                    clock = container.clock,
                )
            }
        }
    }
    val viewModel: CustomersViewModel = viewModel(factory = customersViewModelFactory)
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) { CustomersActions.from(viewModel) }
    CustomersScreen(state = state, actions = actions, modifier = modifier)
}

@Composable
internal fun CustomersScreen(
    state: CustomersUiState,
    actions: CustomersActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        actions.onMessageConsumed()
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Customers",
                supportingText = "Debt, due dates, and payments",
                actions = {
                    Button(
                        onClick = actions.onOpenCreateCustomer,
                        modifier = Modifier.heightIn(min = PosSizes.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(PosSizes.iconSmall),
                        )
                        Spacer(modifier = Modifier.width(PosSpacing.xs))
                        Text(text = "New", style = LocalAppTextStyles.current.buttonLabel)
                    }
                },
            )
            OfflineNotice()
            CustomersSearchField(state = state, actions = actions)
            UiStateContent(
                state = state.customers,
                onLoading = { LoadingState(modifier = Modifier.weight(1f), label = "Loading customers") },
                onEmpty = {
                    CustomersEmptyState(state = state, actions = actions, modifier = Modifier.weight(1f))
                },
                onError = { error ->
                    ErrorState(error = error, onRetry = actions.onRetry, modifier = Modifier.weight(1f))
                },
                onSuccess = { items ->
                    CustomersList(items = items, actions = actions, modifier = Modifier.weight(1f))
                },
            )
            BannerAdSlot(placement = AdPlacement.DEBT_LIST)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(PosSpacing.md),
        )
        state.recordPayment?.let { draft ->
            RecordPaymentSheet(draft = draft, isSubmitting = state.isSubmitting, actions = actions)
        }
        state.customerForm?.let { form ->
            CustomerFormSheet(form = form, isSubmitting = state.isSubmitting, actions = actions)
        }
    }
}

@Composable
private fun CustomersSearchField(
    state: CustomersUiState,
    actions: CustomersActions,
) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = actions.onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        singleLine = true,
        label = { Text("Search customers") },
        placeholder = { Text("Name") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun CustomersEmptyState(
    state: CustomersUiState,
    actions: CustomersActions,
    modifier: Modifier,
) {
    val isSearching = state.searchQuery.isNotBlank()
    EmptyState(
        icon = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.Groups,
        title = if (isSearching) "No customers match" else "No customers yet",
        supportingText = if (isSearching) {
            "Try another name."
        } else {
            "Add a customer to track kasbon debt and payments."
        },
        modifier = modifier,
        actionLabel = if (isSearching) "Clear search" else "Add customer",
        onAction = if (isSearching) {
            { actions.onSearchQueryChange("") }
        } else {
            actions.onOpenCreateCustomer
        },
    )
}

@Composable
private fun CustomersList(
    items: List<CustomerDebtItem>,
    actions: CustomersActions,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PosSpacing.md,
            end = PosSpacing.md,
            top = PosSpacing.xs,
            bottom = PosSizes.listBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        items(items = items, key = { item -> item.customer.id }) { item ->
            CustomerDebtCard(item = item, actions = actions)
        }
    }
}

@Composable
private fun CustomerDebtCard(
    item: CustomerDebtItem,
    actions: CustomersActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            Text(
                text = item.customer.name,
                style = LocalAppTextStyles.current.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Outstanding ${Rupiah.format(item.outstandingDebt)}",
                style = LocalAppTextStyles.current.financialValue,
            )
            Text(
                text = item.nextDueDateMillis?.let { millis -> "Next due ${formatDate(millis)}" } ?: "No due date set",
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DebtStatusChip(status = item.status)
            if (item.outstandingDebt > 0L) {
                OutlinedButton(
                    onClick = { actions.onOpenRecordPayment(item.customer.id) },
                    modifier = Modifier.align(Alignment.End).heightIn(min = PosSizes.touchTarget),
                ) {
                    Text(text = "Record payment")
                }
            }
        }
    }
}

@Composable
private fun DebtStatusChip(status: DebtStatus) {
    val colorScheme = MaterialTheme.colorScheme
    val icon = when (status) {
        DebtStatus.PAID -> Icons.Filled.CheckCircle
        DebtStatus.UNPAID -> Icons.Filled.Timelapse
        DebtStatus.PARTIALLY_PAID -> Icons.Filled.Timelapse
        DebtStatus.OVERDUE -> Icons.Filled.ErrorOutline
    }
    val containerColor = when (status) {
        DebtStatus.PAID -> colorScheme.secondaryContainer
        DebtStatus.UNPAID -> colorScheme.surfaceVariant
        DebtStatus.PARTIALLY_PAID -> colorScheme.tertiaryContainer
        DebtStatus.OVERDUE -> colorScheme.errorContainer
    }
    val contentColor = when (status) {
        DebtStatus.PAID -> colorScheme.onSecondaryContainer
        DebtStatus.UNPAID -> colorScheme.onSurfaceVariant
        DebtStatus.PARTIALLY_PAID -> colorScheme.onTertiaryContainer
        DebtStatus.OVERDUE -> colorScheme.onErrorContainer
    }
    StatusChip(
        icon = icon,
        label = debtStatusLabel(status),
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

private fun previewCustomer(id: Long, name: String, debt: Long): Customer = Customer(
    id = id,
    name = name,
    whatsappNumber = null,
    totalDebt = debt,
    notes = null,
)

private fun previewCustomersState(): CustomersUiState = CustomersUiState(
    searchQuery = "",
    customers = UiState.Success(
        listOf(
            CustomerDebtItem(
                customer = previewCustomer(id = 1L, name = "Warung Bu Sari", debt = 250_000L),
                outstandingDebt = 250_000L,
                nextDueDateMillis = 1_792_800_000_000L,
                status = DebtStatus.OVERDUE,
            ),
            CustomerDebtItem(
                customer = previewCustomer(id = 2L, name = "Pak Budi", debt = 75_000L),
                outstandingDebt = 75_000L,
                nextDueDateMillis = null,
                status = DebtStatus.PARTIALLY_PAID,
            ),
            CustomerDebtItem(
                customer = previewCustomer(id = 3L, name = "Ibu Ani", debt = 0L),
                outstandingDebt = 0L,
                nextDueDateMillis = null,
                status = DebtStatus.PAID,
            ),
        ),
    ),
    recordPayment = null,
    customerForm = null,
)

private fun previewCustomersActions(): CustomersActions = CustomersActions(
    onSearchQueryChange = {},
    onRetry = {},
    onOpenRecordPayment = {},
    onCloseRecordPayment = {},
    onSelectTransaction = {},
    onAmountChange = {},
    onPaymentMethodChange = {},
    onNoteChange = {},
    onConfirmPayment = {},
    onOpenCreateCustomer = {},
    onCloseCustomerForm = {},
    onCustomerNameChange = {},
    onCustomerWhatsappChange = {},
    onCustomerNotesChange = {},
    onSubmitCustomerForm = {},
    onMessageConsumed = {},
)

@Preview(name = "Customers compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CustomersCompactPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            CustomersScreen(state = previewCustomersState(), actions = previewCustomersActions())
        }
    }
}

@Preview(name = "Customers payment sheet", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CustomersPaymentPreview() {
    UmkmPosTheme {
        PreviewAppContainerProvider {
            CustomersScreen(
                state = previewCustomersState().copy(
                    recordPayment = RecordPaymentState(
                        customerId = 1L,
                        customerName = "Warung Bu Sari",
                        outstandingDebt = 250_000L,
                        transactions = listOf(
                            OutstandingTransactionItem(
                                transactionId = 10L,
                                invoiceNo = "INV-1001",
                                remainingDebt = 150_000L,
                                dueDateMillis = 1_792_800_000_000L,
                                isOverdue = true,
                            ),
                            OutstandingTransactionItem(
                                transactionId = 11L,
                                invoiceNo = "INV-1004",
                                remainingDebt = 100_000L,
                                dueDateMillis = null,
                                isOverdue = false,
                            ),
                        ),
                        selectedTransactionId = 10L,
                        amountInput = "50",
                        validationMessage = null,
                    ),
                ),
                actions = previewCustomersActions(),
            )
        }
    }
}
