package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.successDataOrNull
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import androidx.compose.ui.res.painterResource
import id.umkm.pos.pro.R

private const val ALL_CATEGORIES_LABEL = "Semua Kategori"
private const val ALL_CATEGORY_CHIP_LABEL = "Semua"

@Composable
internal fun ProductsSearchField(
    state: ProductsUiState,
    actions: ProductsActions,
) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = actions.onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
        singleLine = true,
        label = { Text("Cari produk") },
        placeholder = { Text("Cari nama produk, kategori...") },
        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (state.searchQuery.isNotEmpty()) {
                IconButton(onClick = { actions.onSearchQueryChange("") }) {
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Hapus pencarian")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
internal fun ProductsFilterRow(
    state: ProductsUiState,
    actions: ProductsActions,
) {
    val categories = state.catalog.successDataOrNull()?.categories.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        ProductsDropdown(
            label = state.categoryFilter ?: ALL_CATEGORIES_LABEL,
            modifier = Modifier.weight(1f),
        ) { dismiss ->
            DropdownMenuItem(
                text = { Text(ALL_CATEGORIES_LABEL) },
                trailingIcon = { SelectedOptionIcon(isSelected = state.categoryFilter == null) },
                onClick = {
                    dismiss()
                    actions.onCategoryFilterChange(null)
                },
            )
            categories.forEach { category ->
                val isSelected = category.equals(state.categoryFilter, ignoreCase = true)
                DropdownMenuItem(
                    text = { Text(category) },
                    trailingIcon = { SelectedOptionIcon(isSelected = isSelected) },
                    onClick = {
                        dismiss()
                        actions.onCategoryFilterChange(category)
                    },
                )
            }
        }
        ProductsDropdown(
            label = "Urutkan: ${state.sortOrder.label}",
            modifier = Modifier.weight(1f),
        ) { dismiss ->
            ProductSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    trailingIcon = { SelectedOptionIcon(isSelected = order == state.sortOrder) },
                    onClick = {
                        dismiss()
                        actions.onSortOrderChange(order)
                    },
                )
            }
        }
    }
}

@Composable
internal fun ProductsCategoryChips(
    categories: List<String>,
    selectedCategoryFilter: String?,
    onCategorySelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        CategoryChip(
            label = ALL_CATEGORY_CHIP_LABEL,
            isSelected = selectedCategoryFilter == null,
            onClick = { onCategorySelect(null) },
        )
        categories.forEach { category ->
            CategoryChip(
                label = category,
                isSelected = category.equals(selectedCategoryFilter, ignoreCase = true),
                onClick = { onCategorySelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .heightIn(min = PosSizes.touchTarget)
            .padding(horizontal = PosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = LocalAppTextStyles.current.supportingLabel,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProductsDropdown(
    label: String,
    modifier: Modifier = Modifier,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.small
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
                .clickable(role = Role.DropdownList, onClick = { isExpanded = true })
                .heightIn(min = PosSizes.touchTarget)
                .padding(start = PosSpacing.sm, end = PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = LocalAppTextStyles.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PosSizes.iconMedium),
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            menuContent { isExpanded = false }
        }
    }
}

@Composable
private fun SelectedOptionIcon(isSelected: Boolean) {
    if (!isSelected) return
    Icon(
        painter = painterResource(R.drawable.ic_check),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(PosSizes.iconSmall),
    )
}

@Composable
internal fun ProductsFilterEmptyState(actions: ProductsActions) {
    EmptyState(
        iconRes = R.drawable.ic_funnel,
        title = "Tidak ada produk pada filter ini",
        supportingText = "Pilih kategori lain atau tampilkan semua produk.",
        actionLabel = "Tampilkan semua",
        onAction = { actions.onCategoryFilterChange(null) },
    )
}
