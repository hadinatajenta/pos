package id.umkm.pos.pro.ui.components

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import id.umkm.pos.pro.data.image.decodeProductImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val IMAGE_CACHE_ENTRIES = 16
private const val THUMBNAIL_EDGE_PX = 288

private val fileImageCache = LruCache<String, ImageBitmap>(IMAGE_CACHE_ENTRIES)

@Composable
fun rememberFileImageBitmap(imagePath: String?): ImageBitmap? {
    val cacheKey = imagePath?.takeIf { path -> path.isNotBlank() } ?: return null
    return produceState(initialValue = fileImageCache.get(cacheKey), key1 = cacheKey) {
        if (value == null) {
            val decoded = withContext(Dispatchers.IO) {
                decodeProductImage(cacheKey, THUMBNAIL_EDGE_PX)?.asImageBitmap()
            }
            if (decoded != null) {
                fileImageCache.put(cacheKey, decoded)
            }
            value = decoded
        }
    }.value
}
