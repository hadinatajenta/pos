package id.umkm.pos.pro.data.file

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class DownloadsFileSink(private val context: Context) : BinarySink, CsvSink {

    override suspend fun write(targetPath: String, content: ByteArray): Long =
        writeToDownloads(targetPath, content)

    override suspend fun write(targetPath: String, content: String): Long =
        writeToDownloads(targetPath, content.toByteArray(Charsets.UTF_8))

    private suspend fun writeToDownloads(requestedName: String, content: ByteArray): Long =
        withContext(Dispatchers.IO) {
            val fileName = FileNames.normalized(requestedName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(fileName, content)
            } else {
                writeToPublicDownloads(fileName, content)
            }
            content.size.toLong()
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeViaMediaStore(fileName: String, content: ByteArray) {
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pendingValues(fileName))
            ?: throw IOException("failed to create export file in $DESTINATION_LABEL")
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content)
            } ?: throw IOException("failed to open the export file stream")
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
        resolver.update(uri, publishedValues(), null, null)
    }

    @Suppress("DEPRECATION")
    private fun writeToPublicDownloads(fileName: String, content: ByteArray) {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists() && !downloads.mkdirs()) {
            throw IOException("failed to create the $DESTINATION_LABEL directory")
        }
        File(downloads, fileName).writeBytes(content)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pendingValues(fileName: String): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, FileNames.mimeTypeFor(fileName))
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, STILL_PENDING)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishedValues(): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.IS_PENDING, NO_LONGER_PENDING)
    }

    companion object {
        const val DESTINATION_LABEL: String = "Download"

        private const val STILL_PENDING = 1
        private const val NO_LONGER_PENDING = 0
    }
}
