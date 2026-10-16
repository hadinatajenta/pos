package id.umkm.pos.pro.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ProductImageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val IMAGE_DIRECTORY_NAME = "product-images"
private const val MAX_STORED_EDGE_PX = 1024
private const val JPEG_QUALITY = 88

fun decodeProductImage(imagePath: String, targetEdgePx: Int): Bitmap? {
    if (imagePath.isBlank()) return null
    if (!File(imagePath).isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(imagePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, targetEdgePx)
    }
    return BitmapFactory.decodeFile(imagePath, options)
}

class ProductImageStore(private val context: Context) : ProductImageRepository {

    override suspend fun importImage(sourceUri: String): AppResult<String> = withContext(Dispatchers.IO) {
        val target = File(imageDirectory(), "${UUID.randomUUID()}.jpg")
        target.parentFile?.mkdirs()
        try {
            val bitmap = decodeSourceImage(Uri.parse(sourceUri))
                ?: return@withContext AppResult.Failure(AppError.Validation("image source cannot be decoded"))
            target.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()
            AppResult.Success(target.absolutePath)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            target.delete()
            AppResult.Failure(AppError.Storage(error.message ?: "failed to store product image"))
        }
    }

    override suspend fun deleteImage(imagePath: String?) = withContext(Dispatchers.IO) {
        val path = imagePath?.takeIf { candidate -> candidate.isNotBlank() } ?: return@withContext
        val file = File(path)
        if (file.parentFile?.absolutePath != imageDirectory().absolutePath) return@withContext
        if (file.isFile) {
            file.delete()
        }
    }

    private fun imageDirectory(): File = File(context.filesDir, IMAGE_DIRECTORY_NAME)

    private fun decodeSourceImage(source: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(source)
        } else {
            decodeWithBitmapFactory(source)
        }

    private fun decodeWithImageDecoder(source: Uri): Bitmap {
        val imageSource = ImageDecoder.createSource(context.contentResolver, source)
        return ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val longestEdge = maxOf(info.size.width, info.size.height)
            if (longestEdge > MAX_STORED_EDGE_PX) {
                val scale = MAX_STORED_EDGE_PX.toFloat() / longestEdge.toFloat()
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    }

    private fun decodeWithBitmapFactory(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(source) ?: return null
        boundsStream.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_STORED_EDGE_PX)
        }
        val imageStream = context.contentResolver.openInputStream(source) ?: return null
        return imageStream.use { stream -> BitmapFactory.decodeStream(stream, null, options) }
    }
}

private fun sampleSizeFor(width: Int, height: Int, targetEdgePx: Int): Int {
    var sampleSize = 1
    var longestEdge = maxOf(width, height)
    while (longestEdge / 2 >= targetEdgePx) {
        longestEdge /= 2
        sampleSize *= 2
    }
    return sampleSize
}
