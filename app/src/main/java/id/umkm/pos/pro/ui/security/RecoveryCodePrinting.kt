package id.umkm.pos.pro.ui.security

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

data class RecoveryCodePrintRequest(
    val documentName: String,
    val title: String,
    val codeLabel: String,
    val code: String,
    val instructions: List<String>,
    val footer: String,
    val renderFailureMessage: String,
)

@Composable
fun rememberRecoveryCodePrintLauncher(): (RecoveryCodePrintRequest) -> Unit {
    val context: Context = LocalContext.current
    return remember(context) {
        val launchSystemPrint: (RecoveryCodePrintRequest) -> Unit = { request ->
            val printManager = context.getSystemService(PrintManager::class.java)
            if (printManager != null) {
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(
                    request.documentName,
                    RecoveryCodePrintAdapter(request),
                    attributes,
                )
            }
        }
        launchSystemPrint
    }
}

internal fun wrapRecoveryCodeLine(text: String, maxCharacters: Int): List<String> {
    if (text.isEmpty()) return emptyList()
    if (text.length <= maxCharacters) return listOf(text)
    val lines = mutableListOf<String>()
    val current = StringBuilder()
    text.split(' ').forEach { word ->
        when {
            current.isEmpty() -> current.append(word)
            current.length + 1 + word.length <= maxCharacters -> current.append(' ').append(word)
            else -> {
                lines += current.toString()
                current.clear()
                current.append(word)
            }
        }
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}

private class RecoveryCodePrintAdapter(
    private val request: RecoveryCodePrintRequest,
) : PrintDocumentAdapter() {

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
        val documentInfo = PrintDocumentInfo.Builder(DOCUMENT_FILE_NAME)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PAGE_COUNT)
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
        val document = PdfDocument()
        try {
            writePage(document)
            FileOutputStream(destination.fileDescriptor).use { output ->
                document.writeTo(output)
            }
            callback.onWriteFinished(pages ?: arrayOf(PageRange.ALL_PAGES))
        } catch (error: IOException) {
            callback.onWriteFailed(error.message ?: request.renderFailureMessage)
        } finally {
            document.close()
        }
    }

    private fun writePage(document: PdfDocument) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var baselineY = MARGIN_PT + TITLE_TEXT_SIZE_PT

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = TITLE_TEXT_SIZE_PT
            color = TEXT_COLOR
            isAntiAlias = true
        }
        canvas.drawText(request.title, MARGIN_PT, baselineY, titlePaint)

        baselineY += LABEL_GAP_PT + LABEL_TEXT_SIZE_PT
        val labelPaint = Paint().apply {
            typeface = Typeface.SANS_SERIF
            textSize = LABEL_TEXT_SIZE_PT
            color = MUTED_TEXT_COLOR
            isAntiAlias = true
        }
        canvas.drawText(request.codeLabel, MARGIN_PT, baselineY, labelPaint)

        baselineY += CODE_GAP_PT + CODE_TEXT_SIZE_PT
        val codePaint = Paint().apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = CODE_TEXT_SIZE_PT
            color = TEXT_COLOR
            isAntiAlias = true
        }
        canvas.drawText(request.code, MARGIN_PT, baselineY, codePaint)

        baselineY += INSTRUCTION_GAP_PT
        val instructionPaint = Paint().apply {
            typeface = Typeface.SANS_SERIF
            textSize = INSTRUCTION_TEXT_SIZE_PT
            color = TEXT_COLOR
            isAntiAlias = true
        }
        request.instructions.forEach { instruction ->
            wrapRecoveryCodeLine(instruction, MAX_CHARACTERS_PER_LINE).forEach { line ->
                baselineY += INSTRUCTION_LINE_HEIGHT_PT
                canvas.drawText(line, MARGIN_PT, baselineY, instructionPaint)
            }
            baselineY += INSTRUCTION_PARAGRAPH_GAP_PT
        }

        val footerPaint = Paint().apply {
            typeface = Typeface.SANS_SERIF
            textSize = FOOTER_TEXT_SIZE_PT
            color = MUTED_TEXT_COLOR
            isAntiAlias = true
        }
        canvas.drawText(request.footer, MARGIN_PT, PAGE_HEIGHT_PT - MARGIN_PT, footerPaint)

        document.finishPage(page)
    }

    private companion object {
        const val DOCUMENT_FILE_NAME = "kode-pemulihan.pdf"
        const val PAGE_COUNT = 1
        const val PAGE_WIDTH_PT = 595
        const val PAGE_HEIGHT_PT = 842
        const val MARGIN_PT = 56f
        const val TITLE_TEXT_SIZE_PT = 20f
        const val LABEL_TEXT_SIZE_PT = 11f
        const val CODE_TEXT_SIZE_PT = 26f
        const val INSTRUCTION_TEXT_SIZE_PT = 11.5f
        const val FOOTER_TEXT_SIZE_PT = 10f
        const val LINE_HEIGHT_PT = 15f
        const val LABEL_GAP_PT = 40f
        const val CODE_GAP_PT = 34f
        const val INSTRUCTION_GAP_PT = 40f
        const val INSTRUCTION_LINE_HEIGHT_PT = 16f
        const val INSTRUCTION_PARAGRAPH_GAP_PT = 14f
        const val MAX_CHARACTERS_PER_LINE = 62
        const val TEXT_COLOR = 0xFF111111.toInt()
        const val MUTED_TEXT_COLOR = 0xFF52605B.toInt()
    }
}
