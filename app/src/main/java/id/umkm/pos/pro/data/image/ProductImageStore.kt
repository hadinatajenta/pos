package id.umkm.pos.pro.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
            val bitmap = decodePickedBitmap(context, Uri.parse(sourceUri), MAX_STORED_EDGE_PX)
                ?: return@withContext AppResult.Failure(
                    AppError.Validation("image source cannot be decoded"),
                )
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
}
