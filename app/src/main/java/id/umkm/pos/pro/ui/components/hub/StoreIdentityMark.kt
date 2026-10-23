package id.umkm.pos.pro.ui.components.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R

private val WHITESPACE_SEPARATOR = Regex("\\s+")
private const val MAX_STORE_INITIALS = 2

@Composable
fun StoreIdentityMark(
    storeName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val initials = remember(storeName) { deriveStoreInitials(storeName) }
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (initials == null) {
            Icon(
                painter = painterResource(R.drawable.ic_store),
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun deriveStoreInitials(storeName: String?): String? {
    val tokens = storeName
        ?.trim()
        ?.split(WHITESPACE_SEPARATOR)
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    if (tokens.isEmpty()) return null
    return tokens
        .take(MAX_STORE_INITIALS)
        .map { token -> token.first().uppercaseChar() }
        .joinToString(separator = "")
        .take(MAX_STORE_INITIALS)
}
