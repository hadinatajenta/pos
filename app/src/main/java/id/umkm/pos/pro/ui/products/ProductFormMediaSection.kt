package id.umkm.pos.pro.ui.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.components.ProductThumbnail
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosColors

private val PRODUCT_ICONS = listOf(
    "🍚",
    "🍜",
    "🍞",
    "🍰",
    "🍪",
    "☕",
    "🍎",
    "🍌",
    "🍗",
    "🐟",
    "🍺",
    "💊",
    "📦",
    "🍔",
)

@Composable
internal fun ProductMediaSection(
    form: ProductFormState,
    actions: ProductsActions,
    isSaving: Boolean,
) {
    ProductIconPicker(
        selectedIcon = form.icon,
        onSelect = actions.onFormIconChange,
        enabled = !isSaving,
    )
    ProductPhotoPicker(
        form = form,
        actions = actions,
        isSaving = isSaving,
    )
}

@Composable
private fun ProductIconPicker(
    selectedIcon: String?,
    onSelect: (String?) -> Unit,
    enabled: Boolean,
) {
    val strings = LocalPosStrings.current
    val icons = if (selectedIcon != null && selectedIcon.isNotBlank() && selectedIcon !in PRODUCT_ICONS) {
        listOf(selectedIcon) + PRODUCT_ICONS
    } else {
        PRODUCT_ICONS
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProductFieldLabel(text = strings.productsProductIcon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icons.forEach { emoji ->
                val isSelected = selectedIcon == emoji
                ProductIconChoice(
                    emoji = emoji,
                    selected = isSelected,
                    contentDescriptionText = "${strings.productsProductIcon} $emoji",
                    enabled = enabled,
                    onClick = { onSelect(if (isSelected) null else emoji) },
                )
            }
        }
    }
}

@Composable
private fun ProductIconChoice(
    emoji: String,
    selected: Boolean,
    contentDescriptionText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.border(2.dp, ProductPalette.Focus, CircleShape)
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(3.dp)
            .semantics { contentDescription = contentDescriptionText },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (selected) ProductPalette.Ink else ProductPalette.Background)
                .border(
                    width = 1.dp,
                    color = if (selected) ProductPalette.Ink else ProductPalette.Line,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun ProductPhotoPicker(
    form: ProductFormState,
    actions: ProductsActions,
    isSaving: Boolean,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { pickedUri ->
        pickedUri?.let { uri -> actions.onProductImagePicked(uri.toString()) }
    }
    val canPick = !form.isImageImporting && !isSaving
    val launchPicker: () -> Unit = {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProductFieldLabel(text = strings.productsPhoto)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductThumbnail(
                category = form.category,
                size = 56.dp,
                imagePath = form.imagePath,
                productName = form.name,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (form.isImageImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ProductPalette.Brand,
                        )
                    }
                    TextButton(
                        onClick = launchPicker,
                        enabled = canPick,
                        modifier = Modifier.heightIn(min = 44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cloud_upload),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (form.imagePath == null) {
                                strings.productsUploadPhoto
                            } else {
                                strings.productsPhotoChange
                            },
                            style = LocalAppTextStyles.current.buttonLabel,
                            maxLines = 1,
                        )
                    }
                }
                if (form.imagePath != null) {
                    TextButton(
                        onClick = actions.onProductImageRemove,
                        enabled = canPick,
                        modifier = Modifier.heightIn(min = 44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.dangerText),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trash),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.productsPhotoRemove,
                            style = LocalAppTextStyles.current.buttonLabel,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (form.isImageRemoved) {
            Text(
                text = strings.productsPhotoWillBeRemoved,
                style = LocalAppTextStyles.current.supportingLabel,
                color = PosColors.TextSecondary,
            )
        }
    }
}
