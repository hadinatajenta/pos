package id.umkm.pos.pro.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.CriticalAction
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.security.rememberSecurityGate
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import kotlinx.coroutines.delay

private val FORM_CONTAINER_MAX_WIDTH = 560.dp
private val FORM_HEADER_CLOSE_SIZE = 40.dp
private val FORM_FOOTER_ACTION_HEIGHT = 54.dp
private val FORM_FOOTER_DELETE_SIZE = 54.dp
private const val DELETE_ARM_CONFIRM_MILLIS = 2_500L

private val FORM_FIELD_ORDER = listOf(
    ProductFormField.NAME,
    ProductFormField.CATEGORY,
    ProductFormField.BASE_UNIT,
    ProductFormField.BASE_PRICE,
    ProductFormField.BASE_HPP,
    ProductFormField.VARIANT_NAME,
    ProductFormField.CONVERSION_FACTOR,
    ProductFormField.VARIANT_PRICE,
    ProductFormField.WHOLESALE_MIN_QTY,
    ProductFormField.WHOLESALE_PRICE,
    ProductFormField.STOCK_QUANTITY,
    ProductFormField.MIN_STOCK_ALERT,
    ProductFormField.SKU,
    ProductFormField.BARCODE,
)

internal class FormFieldFocus {

    private val focusRequesters: Map<ProductFormField, FocusRequester> =
        ProductFormField.entries.associateWith { FocusRequester() }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    private val bringIntoViewRequesters: Map<ProductFormField, BringIntoViewRequester> =
        ProductFormField.entries.associateWith { BringIntoViewRequester() }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    fun modifierFor(field: ProductFormField): Modifier = Modifier
        .focusRequester(focusRequesters.getValue(field))
        .bringIntoViewRequester(bringIntoViewRequesters.getValue(field))

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    suspend fun focusAndReveal(field: ProductFormField) {
        focusRequesters.getValue(field).requestFocus()
        bringIntoViewRequesters.getValue(field).bringIntoView()
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    suspend fun focusFirstError(errors: Map<ProductFormField, UiText>) {
        val target = FORM_FIELD_ORDER.firstOrNull { field -> field in errors } ?: return
        focusRequesters.getValue(target).requestFocus()
        bringIntoViewRequesters.getValue(target).bringIntoView()
    }
}

@Composable
internal fun ProductFormPanel(
    form: ProductFormState,
    isSaving: Boolean,
    categorySuggestions: List<String>,
    actions: ProductsActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    val gate = rememberSecurityGate()
    val fieldFocus = remember { FormFieldFocus() }
    var isCostRevealed by remember(form.productId) { mutableStateOf(false) }
    var isDeleteArmed by remember(form.productId) { mutableStateOf(false) }
    val showsCostValue = form.persistedPricing == null ||
        isCostRevealed ||
        gate.isUnlocked(CriticalAction.EDIT_PRODUCT_COST)
    val pricingChange = productPricingChange(form)
    val pricingPinAction = when {
        pricingChange.requiresCostPin && !isCostRevealed -> CriticalAction.EDIT_PRODUCT_COST
        pricingChange.requiresPricePin -> CriticalAction.EDIT_PRODUCT_PRICE
        else -> null
    }
    val onSave: () -> Unit = {
        if (pricingPinAction == null || validateProductForm(form).isNotEmpty()) {
            actions.onSubmitForm()
        } else {
            gate.request(pricingPinAction, pricingPinLabel(pricingPinAction, strings)) {
                actions.onSubmitForm()
            }
        }
    }
    val onDeleteClick: () -> Unit = {
        val productId = form.productId
        if (productId != null) {
            if (isDeleteArmed) {
                isDeleteArmed = false
                gate.request(CriticalAction.DELETE_PRODUCT, strings.actionDelete) {
                    actions.onDeleteProduct(productId)
                }
            } else {
                isDeleteArmed = true
            }
        }
    }
    LaunchedEffect(form.validationFailures) {
        if (form.validationFailures > 0L) {
            fieldFocus.focusFirstError(form.errors)
        }
    }
    LaunchedEffect(isDeleteArmed) {
        if (!isDeleteArmed) return@LaunchedEffect
        delay(DELETE_ARM_CONFIRM_MILLIS)
        isDeleteArmed = false
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProductPalette.Surface),
    ) {
        ProductFormHeader(
            form = form,
            isSaving = isSaving,
            onClose = actions.onRequestDismissForm,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = FORM_CONTAINER_MAX_WIDTH)
                        .fillMaxWidth()
                        .padding(
                            start = FORM_PAGE_PADDING,
                            end = FORM_PAGE_PADDING,
                            top = 4.dp,
                            bottom = 24.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProductSectionLabel(text = strings.productsSectionBasics)
                    ProductBasicsSection(
                        form = form,
                        categorySuggestions = categorySuggestions,
                        fieldFocus = fieldFocus,
                        actions = actions,
                        isSaving = isSaving,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProductSectionLabel(text = strings.productsPricingAndUnits)
                    ProductPricingSection(
                        form = form,
                        fieldFocus = fieldFocus,
                        actions = actions,
                        showsCostValue = showsCostValue,
                        onRequestCostUnlock = {
                            gate.request(CriticalAction.EDIT_PRODUCT_COST, strings.productsBaseHpp) {
                                isCostRevealed = true
                            }
                        },
                        isSaving = isSaving,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProductSectionLabel(text = strings.productsSectionStock)
                    ProductStockSection(
                        form = form,
                        fieldFocus = fieldFocus,
                        actions = actions,
                        isSaving = isSaving,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProductSectionLabel(text = strings.productsOtherOptions)
                    ProductOtherOptionsSection(
                        form = form,
                        fieldFocus = fieldFocus,
                        actions = actions,
                        isSaving = isSaving,
                    )
                }
            }
        }
        ProductFormBottomBar(
            form = form,
            isSaving = isSaving,
            isDeleteArmed = isDeleteArmed,
            onSave = onSave,
            onDeleteClick = onDeleteClick,
        )
    }
}

@Composable
private fun ProductFormHeader(
    form: ProductFormState,
    isSaving: Boolean,
    onClose: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val title = if (form.isEditing) strings.productsEditProduct else strings.productsNewProduct
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = FORM_PAGE_PADDING,
                end = FORM_PAGE_PADDING,
                top = 18.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = ProductPalette.Ink,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(FORM_HEADER_CLOSE_SIZE)
                .clip(CircleShape)
                .background(ProductPalette.Background)
                .clickable(
                    enabled = !isSaving,
                    role = Role.Button,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = strings.productsBack,
                tint = if (isSaving) ProductPalette.Ink.copy(alpha = 0.5f) else ProductPalette.Ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ProductFormBottomBar(
    form: ProductFormState,
    isSaving: Boolean,
    isDeleteArmed: Boolean,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Surface(
        color = ProductPalette.Surface,
        contentColor = ProductPalette.Ink,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(color = ProductPalette.Line)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FORM_PAGE_PADDING, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = FORM_CONTAINER_MAX_WIDTH)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (form.isEditing) {
                        Surface(
                            onClick = onDeleteClick,
                            enabled = !isSaving,
                            color = if (isDeleteArmed) palette.dangerText else ProductPalette.OutContainer,
                            contentColor = if (isDeleteArmed) {
                                androidx.compose.ui.graphics.Color.White
                            } else {
                                ProductPalette.OutText
                            },
                            shape = ProductRadii.button,
                            border = if (isDeleteArmed) {
                                BorderStroke(1.dp, palette.dangerText)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .height(FORM_FOOTER_ACTION_HEIGHT)
                                .then(
                                    if (isDeleteArmed) {
                                        Modifier
                                    } else {
                                        Modifier.width(FORM_FOOTER_DELETE_SIZE)
                                    },
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = if (isDeleteArmed) 12.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_trash),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (isDeleteArmed) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.productsDeleteArmed,
                                        style = LocalAppTextStyles.current.buttonLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    SaveProductButton(
                        label = if (form.isEditing) {
                            strings.productsSaveChanges
                        } else {
                            strings.productsSaveNew
                        },
                        enabled = form.canSubmit && !isSaving,
                        isSaving = isSaving,
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveProductButton(
    label: String,
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(FORM_FOOTER_ACTION_HEIGHT),
        shape = ProductRadii.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = ProductPalette.Brand,
            contentColor = androidx.compose.ui.graphics.Color.White,
            disabledContainerColor = ProductPalette.Brand.copy(alpha = 0.40f),
            disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f),
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = LocalAppTextStyles.current.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
