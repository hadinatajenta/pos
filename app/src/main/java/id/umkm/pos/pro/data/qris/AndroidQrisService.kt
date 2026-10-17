package id.umkm.pos.pro.data.qris

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.QrisImport
import id.umkm.pos.pro.core.contract.QrisService
import id.umkm.pos.pro.data.image.decodePickedBitmap
import id.umkm.pos.pro.domain.qris.QrisPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val QRIS_DIRECTORY_NAME = "qris"
private const val MAX_SOURCE_EDGE_PX = 1600
private const val JPEG_QUALITY = 92

class AndroidQrisService(private val context: Context) : QrisService {

    override suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport> =
        withContext(Dispatchers.IO) {
            val bitmap = decodePickedBitmap(context, Uri.parse(sourceUri), MAX_SOURCE_EDGE_PX)
                ?: return@withContext AppResult.Failure(
                    AppError.Validation("qris image cannot be decoded"),
                )
            try {
                val payload = decodePayload(bitmap)
                if (payload == null || !QrisPayload.isValid(payload)) {
                    return@withContext AppResult.Failure(
                        AppError.Validation("qris payload cannot be read"),
                    )
                }
                val target = File(imageDirectory(), "${UUID.randomUUID()}.jpg")
                target.parentFile?.mkdirs()
                target.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                }
                AppResult.Success(QrisImport(imagePath = target.absolutePath, payload = payload))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppResult.Failure(AppError.Storage(error.message ?: "failed to store qris image"))
            } finally {
                bitmap.recycle()
            }
        }

    override suspend fun deleteQrisImage(imagePath: String?) = withContext(Dispatchers.IO) {
        val path = imagePath?.takeIf { candidate -> candidate.isNotBlank() } ?: return@withContext
        val file = File(path)
        if (file.parentFile?.absolutePath != imageDirectory().absolutePath) return@withContext
        if (file.isFile) {
            file.delete()
        }
    }

    private fun decodePayload(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
        )
        return try {
            MultiFormatReader().apply { setHints(hints) }.decode(binary).text
        } catch (error: Exception) {
            null
        }
    }

    private fun imageDirectory(): File = File(context.filesDir, QRIS_DIRECTORY_NAME)
}
