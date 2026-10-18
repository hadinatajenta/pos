package id.umkm.pos.pro.data.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun interface CsvSink {
    suspend fun write(targetPath: String, content: String): Long
}

class FileCsvSink : CsvSink {

    override suspend fun write(targetPath: String, content: String): Long =
        withContext(Dispatchers.IO) {
            val target = File(targetPath)
            target.parentFile?.mkdirs()
            val bytes = content.toByteArray(Charsets.UTF_8)
            target.writeBytes(bytes)
            bytes.size.toLong()
        }
}
