package id.umkm.pos.pro.ui.customers

import id.umkm.pos.pro.core.model.PaymentMethod

class CustomersActions(
    val onSearchQueryChange: (String) -> Unit,
    val onRetry: () -> Unit,
    val onOpenRecordPayment: (Long) -> Unit,
    val onCloseRecordPayment: () -> Unit,
    val onSelectTransaction: (Long) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onPaymentMethodChange: (PaymentMethod) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onConfirmPayment: () -> Unit,
    val onOpenCreateCustomer: () -> Unit,
    val onCloseCustomerForm: () -> Unit,
    val onCustomerNameChange: (String) -> Unit,
    val onCustomerWhatsappChange: (String) -> Unit,
    val onCustomerNotesChange: (String) -> Unit,
    val onSubmitCustomerForm: () -> Unit,
    val onMessageConsumed: () -> Unit,
) {
    companion object {
        fun from(viewModel: CustomersViewModel): CustomersActions = CustomersActions(
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onRetry = viewModel::onRetry,
            onOpenRecordPayment = viewModel::onOpenRecordPayment,
            onCloseRecordPayment = viewModel::onCloseRecordPayment,
            onSelectTransaction = viewModel::onSelectTransaction,
            onAmountChange = viewModel::onAmountChange,
            onPaymentMethodChange = viewModel::onPaymentMethodChange,
            onNoteChange = viewModel::onNoteChange,
            onConfirmPayment = viewModel::onConfirmPayment,
            onOpenCreateCustomer = viewModel::onOpenCreateCustomer,
            onCloseCustomerForm = viewModel::onCloseCustomerForm,
            onCustomerNameChange = viewModel::onCustomerNameChange,
            onCustomerWhatsappChange = viewModel::onCustomerWhatsappChange,
            onCustomerNotesChange = viewModel::onCustomerNotesChange,
            onSubmitCustomerForm = viewModel::onSubmitCustomerForm,
            onMessageConsumed = viewModel::onMessageConsumed,
        )
    }
}
