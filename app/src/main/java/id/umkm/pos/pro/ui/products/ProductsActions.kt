package id.umkm.pos.pro.ui.products

class ProductsActions(
    val onSearchQueryChange: (String) -> Unit,
    val onCategoryFilterChange: (String?) -> Unit,
    val onSortOrderChange: (ProductSortOrder) -> Unit,
    val onRetry: () -> Unit,
    val onOpenCreateForm: () -> Unit,
    val onOpenEditForm: (Long) -> Unit,
    val onDismissForm: () -> Unit,
    val onFormFieldChange: (ProductFormField, String) -> Unit,
    val onFormActiveChange: (Boolean) -> Unit,
    val onFormAdvancedToggle: () -> Unit,
    val onProductImagePicked: (String) -> Unit,
    val onProductImageRemove: () -> Unit,
    val onSubmitForm: () -> Unit,
    val onRequestDelete: (Long) -> Unit,
    val onCancelDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
    val onMessageConsumed: () -> Unit,
) {
    companion object {
        fun from(viewModel: ProductsViewModel): ProductsActions = ProductsActions(
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onCategoryFilterChange = viewModel::onCategoryFilterChange,
            onSortOrderChange = viewModel::onSortOrderChange,
            onRetry = viewModel::onRetry,
            onOpenCreateForm = viewModel::onOpenCreateForm,
            onOpenEditForm = viewModel::onOpenEditForm,
            onDismissForm = viewModel::onDismissForm,
            onFormFieldChange = viewModel::onFormFieldChange,
            onFormActiveChange = viewModel::onFormActiveChange,
            onFormAdvancedToggle = viewModel::onFormAdvancedToggle,
            onProductImagePicked = viewModel::onProductImagePicked,
            onProductImageRemove = viewModel::onProductImageRemove,
            onSubmitForm = viewModel::onSubmitForm,
            onRequestDelete = viewModel::onRequestDelete,
            onCancelDelete = viewModel::onCancelDelete,
            onConfirmDelete = viewModel::onConfirmDelete,
            onMessageConsumed = viewModel::onMessageConsumed,
        )
    }
}
