package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.PosFontFamily

internal val PRODUCTS_SEARCH_BAR_HEIGHT = 56.dp

private val SEARCH_BAR_SHAPE = RoundedCornerShape(22.dp)
private val SEARCH_ACTION_TOUCH_SIZE = 44.dp
private val SEARCH_ICON_SIZE = 18.dp
private val SORT_LABEL_MAX_WIDTH = 84.dp
private val SORT_BUTTON_SHAPE = RoundedCornerShape(16.dp)
private val SORT_ICON_SIZE = 16.dp
private val DIVIDER_HEIGHT = 24.dp

@Composable
internal fun ProductsSearchBar(
    state: ProductsUiState,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusInteractionSource = remember { MutableInteractionSource() }
    val isFocused by focusInteractionSource.collectIsFocusedAsState()
    Surface(
        color = Color.White,
        contentColor = ProductPalette.Ink,
        shape = SEARCH_BAR_SHAPE,
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) ProductPalette.Focus else ProductPalette.Line,
        ),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(PRODUCTS_SEARCH_BAR_HEIGHT),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = ProductPalette.Muted,
                modifier = Modifier.size(SEARCH_ICON_SIZE),
            )
            BasicTextField(
                value = state.searchQuery,
                onValueChange = actions.onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp)
                    .then(
                        if (state.searchQuery.isEmpty()) {
                            Modifier.semantics {
                                contentDescription = strings.productsSearchPlaceholder
                            }
                        } else {
                            Modifier
                        },
                    ),
                textStyle = TextStyle(
                    fontFamily = PosFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = ProductPalette.Ink,
                ),
                singleLine = true,
                cursorBrush = SolidColor(ProductPalette.Brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() },
                ),
                interactionSource = focusInteractionSource,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.searchQuery.isEmpty()) {
                            Text(
                                text = strings.productsSearchPlaceholder,
                                color = ProductPalette.Muted,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (state.searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { actions.onSearchQueryChange("") },
                    modifier = Modifier.size(SEARCH_ACTION_TOUCH_SIZE),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = strings.actionClearSearch,
                        tint = ProductPalette.Muted,
                        modifier = Modifier.size(SEARCH_ICON_SIZE),
                    )
                }
            }
            VerticalDivider(
                color = ProductPalette.Line,
                modifier = Modifier.height(DIVIDER_HEIGHT),
            )
            ProductsSortMenu(
                sortOrder = state.sortOrder,
                onSortOrderChange = actions.onSortOrderChange,
            )
        }
    }
}

@Composable
private fun ProductsSortMenu(
    sortOrder: ProductSortOrder,
    onSortOrderChange: (ProductSortOrder) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val strings = LocalPosStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = SEARCH_ACTION_TOUCH_SIZE)
                .background(
                    color = if (isFocused) ProductPalette.FocusRing else Color.Transparent,
                    shape = SORT_BUTTON_SHAPE,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = strings.productsSort,
                    onClick = { isExpanded = true },
                )
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sortOrder.label(strings),
                color = ProductPalette.Ink,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = SORT_LABEL_MAX_WIDTH),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = ProductPalette.Muted,
                modifier = Modifier.size(SORT_ICON_SIZE),
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = Color.White,
        ) {
            ProductSortOrder.entries.forEach { order ->
                val isSelected = order == sortOrder
                DropdownMenuItem(
                    text = {
                        Text(
                            text = order.label(strings),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = ProductPalette.Brand,
                                modifier = Modifier.size(SEARCH_ICON_SIZE),
                            )
                        }
                    },
                    modifier = Modifier.semantics { selected = isSelected },
                    onClick = {
                        isExpanded = false
                        onSortOrderChange(order)
                    },
                )
            }
        }
    }
}
