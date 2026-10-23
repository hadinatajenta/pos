package id.umkm.pos.pro.ui.products

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.UiText
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.common.formatQuantity
import id.umkm.pos.pro.ui.common.sanitizeQuantityInput
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosColors

internal val FORM_CONTENT_MAX_WIDTH = 560.dp
internal val FORM_PAGE_PADDING = 22.dp

private val FIELD_MIN_HEIGHT = 52.dp
private val FOCUS_RING_WIDTH = 4.dp
private const val REQUIRED_FIELD_MARKER = " *"

@Composable
internal fun ProductSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
        ),
        color = ProductPalette.Muted,
    )
}

@Composable
internal fun ProductFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = PosColors.TextSecondary,
    )
}

@Composable
internal fun ProductTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: UiText?,
    isRequired: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    prefix: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    numeric: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    placeholder: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val palette = LocalPosSemanticPalette.current
    val errorText = error?.asTextOrNull()?.takeIf { text -> text.isNotBlank() }
    val helperText = errorText ?: supportingText?.takeIf { text -> text.isNotBlank() }
    val helperColor = if (errorText != null) palette.dangerText else PosColors.TextSecondary
    val ringColor = when {
        errorText != null -> palette.dangerText.copy(alpha = 0.12f)
        isFocused -> ProductPalette.FocusRing
        else -> Color.Transparent
    }
    val textStyle = LocalAppTextStyles.current.body.let { style ->
        if (numeric) style.copy(fontFeatureSettings = "tnum") else style
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ringColor, ProductRadii.input)
            .padding(FOCUS_RING_WIDTH),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = FIELD_MIN_HEIGHT),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = {
                Text(
                    text = if (isRequired) "$label$REQUIRED_FIELD_MARKER" else label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            },
            placeholder = placeholder?.let { text ->
                {
                    Text(
                        text = text,
                        style = LocalAppTextStyles.current.body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            prefix = prefix?.let { text ->
                {
                    Text(
                        text = text,
                        style = LocalAppTextStyles.current.body,
                        color = PosColors.TextSecondary,
                    )
                }
            },
            trailingIcon = trailingIcon,
            supportingText = helperText?.let { text ->
                {
                    Text(
                        text = text,
                        style = LocalAppTextStyles.current.supportingLabel,
                        color = helperColor,
                    )
                }
            },
            isError = errorText != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            shape = ProductRadii.input,
            colors = productTextFieldColors(danger = palette.dangerText),
        )
    }
}

@Composable
internal fun ProductDropdownField(
    label: String,
    value: String,
    options: List<String>,
    error: UiText?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        ProductTextField(
            label = label,
            value = value,
            onValueChange = {},
            error = error,
            isRequired = false,
            enabled = enabled,
            readOnly = true,
            supportingText = supportingText,
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = PosColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = modifier,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = label,
                    onClick = { isExpanded = true },
                ),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = ProductPalette.Surface,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontWeight = if (option.equals(value, ignoreCase = true)) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onSelect(option)
                    },
                )
            }
            if (extraActionLabel != null && onExtraAction != null) {
                HorizontalDivider(color = ProductPalette.Line)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = extraActionLabel,
                            color = ProductPalette.Brand,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onExtraAction()
                    },
                )
            }
        }
    }
}

@Composable
private fun productTextFieldColors(danger: Color): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = ProductPalette.Ink,
        unfocusedTextColor = ProductPalette.Ink,
        disabledTextColor = PosColors.TextMuted,
        errorTextColor = ProductPalette.Ink,
        cursorColor = ProductPalette.Focus,
        errorCursorColor = danger,
        focusedContainerColor = ProductPalette.Surface,
        unfocusedContainerColor = ProductPalette.Background,
        disabledContainerColor = ProductPalette.Background,
        errorContainerColor = ProductPalette.Surface,
        focusedBorderColor = ProductPalette.Focus,
        unfocusedBorderColor = ProductPalette.Line,
        disabledBorderColor = ProductPalette.Line,
        errorBorderColor = danger,
        focusedLabelColor = ProductPalette.Brand,
        unfocusedLabelColor = PosColors.TextSecondary,
        disabledLabelColor = PosColors.TextMuted,
        errorLabelColor = danger,
        focusedSupportingTextColor = PosColors.TextSecondary,
        unfocusedSupportingTextColor = PosColors.TextSecondary,
        disabledSupportingTextColor = PosColors.TextMuted,
        errorSupportingTextColor = danger,
        focusedPrefixColor = PosColors.TextSecondary,
        unfocusedPrefixColor = PosColors.TextSecondary,
        errorPrefixColor = danger,
        focusedTrailingIconColor = ProductPalette.Brand,
        unfocusedTrailingIconColor = PosColors.TextSecondary,
        errorTrailingIconColor = danger,
    )

@Composable
internal fun ProductSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    compact: Boolean = false,
    enabled: Boolean = true,
    action: Boolean = false,
    dashedBorder: Boolean = false,
) {
    val containerColor = when {
        selected -> ProductPalette.Brand
        accent -> Color.Transparent
        else -> ProductPalette.Background
    }
    val borderColor = when {
        selected -> ProductPalette.Brand
        accent -> ProductPalette.Brand
        else -> ProductPalette.Line
    }
    val contentColor = when {
        selected -> Color.White
        accent -> ProductPalette.Brand
        else -> ProductPalette.Ink
    }
    val selectionModifier = if (action) {
        Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )
    }
    val borderModifier = if (dashedBorder) {
        Modifier.drawBehind {
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(size.height / 2f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                    ),
                ),
            )
        }
    } else {
        Modifier.border(1.dp, borderColor, CircleShape)
    }
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(containerColor, CircleShape)
            .then(borderModifier)
            .then(selectionModifier)
            .padding(horizontal = if (compact) 12.dp else 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = LocalAppTextStyles.current.body.copy(
                fontSize = if (compact) 13.sp else 14.sp,
                lineHeight = if (compact) 18.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ProductInlineNote(
    text: String,
    @DrawableRes iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, ProductRadii.input)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = LocalAppTextStyles.current.supportingLabel.copy(fontFeatureSettings = "tnum"),
            color = contentColor,
        )
    }
}

@Composable
internal fun ProductInlineWarning(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPosSemanticPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_triangle_alert),
            contentDescription = null,
            tint = palette.warningText,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = LocalAppTextStyles.current.supportingLabel,
            color = palette.warningText,
        )
    }
}

@Composable
internal fun QuantityStepper(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: UiText?,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPosSemanticPalette.current
    val errorText = error?.asTextOrNull()?.takeIf { text -> text.isNotBlank() }
    val parsedValue = value.trim().toDoubleOrNull() ?: 0.0
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        errorText != null -> palette.dangerText
        isFocused -> ProductPalette.Focus
        else -> ProductPalette.Line
    }
    val ringColor = when {
        errorText != null -> palette.dangerText.copy(alpha = 0.12f)
        isFocused -> ProductPalette.FocusRing
        else -> Color.Transparent
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ProductFieldLabel(text = label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuantityStepButton(
                iconRes = R.drawable.ic_minus,
                description = decreaseDescription,
                enabled = enabled && parsedValue > 0.0,
                onClick = { onValueChange(steppedQuantity(value, -1)) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(ringColor, ProductRadii.stepper)
                    .padding(FOCUS_RING_WIDTH),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { input -> onValueChange(sanitizeQuantityInput(input)) },
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .clip(ProductRadii.stepper)
                        .background(ProductPalette.Background, ProductRadii.stepper)
                        .border(1.dp, borderColor, ProductRadii.stepper)
                        .semantics { contentDescription = label },
                    enabled = enabled,
                    textStyle = LocalAppTextStyles.current.body.copy(
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        textAlign = TextAlign.Center,
                        color = if (enabled) ProductPalette.Ink else PosColors.TextMuted,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(ProductPalette.Focus),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            innerTextField()
                        }
                    },
                )
            }
            QuantityStepButton(
                iconRes = R.drawable.ic_plus,
                description = increaseDescription,
                enabled = enabled,
                onClick = { onValueChange(steppedQuantity(value, 1)) },
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = LocalAppTextStyles.current.supportingLabel,
                color = palette.dangerText,
            )
        }
    }
}

@Composable
private fun QuantityStepButton(
    @DrawableRes iconRes: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(ProductRadii.stepper)
            .background(ProductPalette.Surface, ProductRadii.stepper)
            .border(1.dp, ProductPalette.Line, ProductRadii.stepper)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (enabled) ProductPalette.Ink else PosColors.TextMuted.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun steppedQuantity(current: String, delta: Int): String {
    val parsed = current.trim().toDoubleOrNull() ?: 0.0
    return formatQuantity((parsed + delta).coerceAtLeast(0.0))
}
