package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.PosSearchField
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing

private val SORT_BUTTON_MIN_HEIGHT = 56.dp

@Composable
internal fun ProductsSearchAndSortBar(
    state: ProductsUiState,
    actions: ProductsActions,
    isCompact: Boolean = true,
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isCompact) PosSpacing.md else PosSpacing.lg,
                vertical = PosSpacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosSearchField(
            value = state.searchQuery,
            onValueChange = actions.onSearchQueryChange,
            placeholder = strings.productsSearchPlaceholder,
            modifier = Modifier.weight(1f),
        )
        SortDropdownButton(
            sortOrder = state.sortOrder,
            onSortOrderChange = actions.onSortOrderChange,
        )
    }
}

@Composable
private fun SortDropdownButton(
    sortOrder: ProductSortOrder,
    onSortOrderChange: (ProductSortOrder) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val strings = LocalPosStrings.current
    Box {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .heightIn(min = SORT_BUTTON_MIN_HEIGHT)
                .clickable(onClickLabel = strings.productsSort, onClick = { isExpanded = true }),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_list_filter),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = sortOrder.label(strings),
                    style = LocalAppTextStyles.current.supportingLabel,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(PosSpacing.xxs))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PosSizes.iconSmall),
                )
            }
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            ProductSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label(strings)) },
                    trailingIcon = { SelectedOptionIcon(isSelected = order == sortOrder) },
                    onClick = {
                        isExpanded = false
                        onSortOrderChange(order)
                    },
                )
            }
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
    val strings = LocalPosStrings.current
    EmptyState(
        iconRes = R.drawable.ic_funnel,
        title = strings.productsFilterEmptyTitle,
        supportingText = strings.productsFilterEmptyHint,
        actionLabel = strings.productsFilterShowAll,
        onAction = {
            actions.onStockFilterChange(ProductStockFilter.ALL)
            actions.onCategoryFilterChange(null)
        },
    )
}
