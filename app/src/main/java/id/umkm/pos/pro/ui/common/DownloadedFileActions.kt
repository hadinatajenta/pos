package id.umkm.pos.pro.ui.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import id.umkm.pos.pro.data.file.FileNames
import java.io.File

fun openDownloadedFile(context: Context, fileName: String): Boolean {
    val uri = resolveDownloadedFileUri(context, fileName) ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, FileNames.mimeTypeFor(fileName))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return context.startActivitySafely(intent)
}

fun shareDownloadedFile(context: Context, fileName: String): Boolean {
    val uri = resolveDownloadedFileUri(context, fileName) ?: return false
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = FileNames.mimeTypeFor(fileName)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return context.startActivitySafely(Intent.createChooser(sendIntent, null))
}

fun resolveDownloadedFileUri(context: Context, fileName: String): Uri? {
    val normalizedName = try {
        FileNames.normalized(fileName)
    } catch (error: IllegalArgumentException) {
        return null
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolveViaMediaStore(context, normalizedName)?.let { uri -> return uri }
    }
    return resolveViaFileProvider(context, normalizedName)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun resolveViaMediaStore(context: Context, fileName: String): Uri? {
    val projection = arrayOf(MediaStore.MediaColumns._ID)
    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
    context.contentResolver
        .query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(fileName), null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0))
            }
        }
    return null
}

private fun resolveViaFileProvider(context: Context, fileName: String): Uri? {
    @Suppress("DEPRECATION")
    val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDirectory, fileName)
    if (!file.exists()) return null
    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (error: IllegalArgumentException) {
        null
    }
}

private fun Context.startActivitySafely(intent: Intent): Boolean {
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        startActivity(intent)
        true
    } catch (error: ActivityNotFoundException) {
        false
    }
}
