package id.umkm.pos.pro.ui.products

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosColors
import kotlin.math.roundToInt

private const val RUPIAH_PREFIX = "Rp"
private const val ADVANCED_CHEVRON_MILLIS = 200

@Composable
internal fun ProductPricingSection(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    showsCostValue: Boolean,
    onRequestCostUnlock: () -> Unit,
    isSaving: Boolean,
) {
    ProductPricingFields(
        form = form,
        fieldFocus = fieldFocus,
        actions = actions,
        showsCostValue = showsCostValue,
        onRequestCostUnlock = onRequestCostUnlock,
        isSaving = isSaving,
    )
}

@Composable
private fun ProductPricingFields(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    showsCostValue: Boolean,
    onRequestCostUnlock: () -> Unit,
    isSaving: Boolean,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val price = form.basePrice.trim().toLongOrNull()
    val cost = form.baseHpp.trim().toLongOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ProductTextField(
                label = strings.productsBasePrice,
                value = form.basePrice,
                onValueChange = { value ->
                    actions.onFormFieldChange(ProductFormField.BASE_PRICE, value.filter(Char::isDigit))
                },
                error = form.errorFor(ProductFormField.BASE_PRICE),
                isRequired = true,
                enabled = !isSaving,
                keyboardType = KeyboardType.Number,
                prefix = RUPIAH_PREFIX,
                numeric = true,
                visualTransformation = RupiahInputVisualTransformation,
                modifier = fieldFocus.modifierFor(ProductFormField.BASE_PRICE).weight(1f),
            )
            if (showsCostValue) {
                ProductTextField(
                    label = strings.productsBaseHpp,
                    value = form.baseHpp,
                    onValueChange = { value ->
                        actions.onFormFieldChange(ProductFormField.BASE_HPP, value.filter(Char::isDigit))
                    },
                    error = form.errorFor(ProductFormField.BASE_HPP),
                    isRequired = false,
                    enabled = !isSaving,
                    keyboardType = KeyboardType.Number,
                    prefix = RUPIAH_PREFIX,
                    numeric = true,
                    visualTransformation = RupiahInputVisualTransformation,
                    modifier = fieldFocus.modifierFor(ProductFormField.BASE_HPP).weight(1f),
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    ProductTextField(
                        label = strings.productsBaseHpp,
                        value = strings.securityMaskedValue,
                        onValueChange = {},
                        error = form.errorFor(ProductFormField.BASE_HPP),
                        isRequired = false,
                        enabled = !isSaving,
                        readOnly = true,
                        keyboardType = KeyboardType.Number,
                        prefix = RUPIAH_PREFIX,
                        numeric = true,
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock),
                                contentDescription = null,
                                tint = PosColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        modifier = fieldFocus.modifierFor(ProductFormField.BASE_HPP),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                enabled = !isSaving,
                                role = Role.Button,
                                onClickLabel = strings.productsBaseHpp,
                                onClick = onRequestCostUnlock,
                            ),
                    )
                }
            }
        }
        if (showsCostValue && price != null && price > 0L && cost != null) {
            if (cost > price) {
                ProductInlineNote(
                    text = strings.productsLossInfo(Rupiah.format(cost - price)),
                    iconRes = R.drawable.ic_triangle_alert,
                    containerColor = palette.warningContainer,
                    contentColor = palette.warningText,
                )
            } else {
                val profit = price - cost
                val margin = ((profit.toDouble() / price.toDouble()) * 100.0).roundToInt()
                ProductInlineNote(
                    text = strings.productsMarginInfo(Rupiah.format(profit), margin),
                    iconRes = R.drawable.ic_trending_up,
                    containerColor = palette.successContainer,
                    contentColor = palette.successText,
                )
            }
        }
        HorizontalDivider(color = ProductPalette.Line)
        AdvancedUnitsToggle(
            expanded = form.isAdvancedExpanded,
            enabled = !isSaving,
            onClick = actions.onFormAdvancedToggle,
        )
        if (form.isAdvancedExpanded) {
            AdvancedUnitFields(
                form = form,
                fieldFocus = fieldFocus,
                actions = actions,
                isSaving = isSaving,
            )
        }
    }
}

@Composable
private fun AdvancedUnitsToggle(
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = ADVANCED_CHEVRON_MILLIS),
        label = "advancedUnitChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = PosColors.TextSecondary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation },
        )
        Text(
            text = strings.productsUnitPricingToggle,
            style = LocalAppTextStyles.current.buttonLabel,
            color = ProductPalette.Brand,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AdvancedUnitFields(
    form: ProductFormState,
    fieldFocus: FormFieldFocus,
    actions: ProductsActions,
    isSaving: Boolean,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductTextField(
            label = strings.productsUnitName,
            value = form.variantName,
            onValueChange = { value -> actions.onFormFieldChange(ProductFormField.VARIANT_NAME, value) },
            error = form.errorFor(ProductFormField.VARIANT_NAME),
            isRequired = false,
            enabled = !isSaving,
            supportingText = strings.productsUnitNameHint,
            modifier = fieldFocus.modifierFor(ProductFormField.VARIANT_NAME),
        )
        ProductTextField(
            label = strings.productsConversionFactor,
            value = form.conversionFactor,
            onValueChange = { value -> actions.onFormFieldChange(ProductFormField.CONVERSION_FACTOR, value) },
            error = form.errorFor(ProductFormField.CONVERSION_FACTOR),
            isRequired = false,
            enabled = !isSaving,
            keyboardType = KeyboardType.Decimal,
            supportingText = strings.productsConversionFactorHint,
            numeric = true,
            modifier = fieldFocus.modifierFor(ProductFormField.CONVERSION_FACTOR),
        )
        ProductTextField(
            label = strings.productsUnitPrice,
            value = form.variantPrice,
            onValueChange = { value ->
                actions.onFormFieldChange(ProductFormField.VARIANT_PRICE, value.filter(Char::isDigit))
            },
            error = form.errorFor(ProductFormField.VARIANT_PRICE),
            isRequired = false,
            enabled = !isSaving,
            keyboardType = KeyboardType.Number,
            prefix = RUPIAH_PREFIX,
            numeric = true,
            visualTransformation = RupiahInputVisualTransformation,
            modifier = fieldFocus.modifierFor(ProductFormField.VARIANT_PRICE),
        )
        ProductTextField(
            label = strings.productsWholesaleMinQty,
            value = form.wholesaleMinQty,
            onValueChange = { value ->
                actions.onFormFieldChange(ProductFormField.WHOLESALE_MIN_QTY, value)
            },
            error = form.errorFor(ProductFormField.WHOLESALE_MIN_QTY),
            isRequired = false,
            enabled = !isSaving,
            keyboardType = KeyboardType.Number,
            numeric = true,
            modifier = fieldFocus.modifierFor(ProductFormField.WHOLESALE_MIN_QTY),
        )
        ProductTextField(
            label = strings.productsWholesalePrice,
            value = form.wholesalePrice,
            onValueChange = { value ->
                actions.onFormFieldChange(ProductFormField.WHOLESALE_PRICE, value.filter(Char::isDigit))
            },
            error = form.errorFor(ProductFormField.WHOLESALE_PRICE),
            isRequired = false,
            enabled = !isSaving,
            keyboardType = KeyboardType.Number,
            prefix = RUPIAH_PREFIX,
            numeric = true,
            visualTransformation = RupiahInputVisualTransformation,
            modifier = fieldFocus.modifierFor(ProductFormField.WHOLESALE_PRICE),
        )
    }
}
