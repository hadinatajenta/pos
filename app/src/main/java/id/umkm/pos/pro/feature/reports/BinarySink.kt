package id.umkm.pos.pro.feature.reports

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun interface BinarySink {
    suspend fun write(targetPath: String, content: ByteArray): Long
}

class FileBinarySink : BinarySink {

    override suspend fun write(targetPath: String, content: ByteArray): Long =
        withContext(Dispatchers.IO) {
            val target = File(targetPath)
            target.parentFile?.mkdirs()
            target.writeBytes(content)
            content.size.toLong()
        }
}
