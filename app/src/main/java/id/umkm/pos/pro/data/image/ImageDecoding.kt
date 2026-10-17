package id.umkm.pos.pro.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build

internal fun decodePickedBitmap(context: Context, source: Uri, maxEdgePx: Int): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeWithImageDecoder(context, source, maxEdgePx)
    } else {
        decodeWithBitmapFactory(context, source, maxEdgePx)
    }

internal fun sampleSizeFor(width: Int, height: Int, targetEdgePx: Int): Int {
    var sampleSize = 1
    var longestEdge = maxOf(width, height)
    while (longestEdge / 2 >= targetEdgePx) {
        longestEdge /= 2
        sampleSize *= 2
    }
    return sampleSize
}

private fun decodeWithImageDecoder(context: Context, source: Uri, maxEdgePx: Int): Bitmap {
    val imageSource = ImageDecoder.createSource(context.contentResolver, source)
    return ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
        val longestEdge = maxOf(info.size.width, info.size.height)
        if (longestEdge > maxEdgePx) {
            val scale = maxEdgePx.toFloat() / longestEdge.toFloat()
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    }
}

private fun decodeWithBitmapFactory(context: Context, source: Uri, maxEdgePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = context.contentResolver.openInputStream(source) ?: return null
    boundsStream.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
    }
    val imageStream = context.contentResolver.openInputStream(source) ?: return null
    return imageStream.use { stream -> BitmapFactory.decodeStream(stream, null, options) }
}
