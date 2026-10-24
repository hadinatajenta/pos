package id.umkm.pos.pro.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.PosShapes

@Composable
fun PosSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Search,
    containerColor: Color = Color.Unspecified,
    focusedContainerColor: Color = Color.Unspecified,
    showFocusBorder: Boolean = true,
) {
    val strings = LocalPosStrings.current
    val resolvedContainerColor = if (containerColor == Color.Unspecified) {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
    } else {
        containerColor
    }
    val resolvedFocusedContainerColor = if (focusedContainerColor == Color.Unspecified) {
        androidx.compose.material3.MaterialTheme.colorScheme.surface
    } else {
        focusedContainerColor
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        shape = PosShapes.large,
        placeholder = {
            Text(
                text = placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = LocalAppTextStyles.current.body,
            )
        },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = strings.actionClearSearch)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = resolvedFocusedContainerColor,
            unfocusedContainerColor = resolvedContainerColor,
            disabledContainerColor = resolvedContainerColor,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = if (showFocusBorder) {
                androidx.compose.material3.MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
    )
}
