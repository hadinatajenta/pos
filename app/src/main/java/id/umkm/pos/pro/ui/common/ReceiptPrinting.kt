package id.umkm.pos.pro.ui.common

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.FileOutputStream
import java.io.IOException

data class ReceiptPrintRequest(
    val documentName: String,
    val content: String,
)

@Composable
fun rememberReceiptPrintLauncher(): (ReceiptPrintRequest) -> Unit {
    val context: Context = LocalContext.current
    return remember(context) {
        val launchSystemPrint: (ReceiptPrintRequest) -> Unit = { request ->
            val printManager = context.getSystemService(PrintManager::class.java)
            if (printManager != null) {
                printManager.print(
                    request.documentName,
                    ReceiptPrintAdapter(request.content),
                    PrintAttributes.Builder().build(),
                )
            }
        }
        launchSystemPrint
    }
}

private class ReceiptPrintAdapter(private val content: String) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val documentInfo = PrintDocumentInfo.Builder("$DOCUMENT_FILE_NAME.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(documentInfo, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        try {
            val document = PdfDocument()
            writePages(document)
            FileOutputStream(destination.fileDescriptor).use { output ->
                document.writeTo(output)
            }
            document.close()
            callback.onWriteFinished(pages ?: arrayOf(PageRange.ALL_PAGES))
        } catch (error: IOException) {
            callback.onWriteFailed(error.message ?: "struk gagal dirender")
        }
    }

    private fun writePages(document: PdfDocument) {
        val textLines = content.split("\n")
        val lineCountPerPage = LINES_PER_PAGE.coerceAtLeast(1)
        val pageChunks = textLines.chunked(lineCountPerPage).ifEmpty { listOf(emptyList()) }
        pageChunks.forEachIndexed { pageIndex, chunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val textPaint = Paint().apply {
                typeface = Typeface.MONOSPACE
                textSize = TEXT_SIZE_PT
                color = TEXT_COLOR
            }
            var baselineY = MARGIN_PT + TEXT_SIZE_PT
            chunk.forEach { line ->
                page.canvas.drawText(line, MARGIN_PT, baselineY, textPaint)
                baselineY += LINE_HEIGHT_PT
            }
            document.finishPage(page)
        }
    }

    private companion object {
        const val DOCUMENT_FILE_NAME = "struk"
        const val PAGE_WIDTH_PT = 595
        const val PAGE_HEIGHT_PT = 842
        const val MARGIN_PT = 36f
        const val TEXT_SIZE_PT = 9.5f
        const val LINE_HEIGHT_PT = 13f
        const val TEXT_COLOR = 0xFF111111.toInt()
        const val LINES_PER_PAGE = 56
    }
}
