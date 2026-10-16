package id.umkm.pos.pro.hardware.receipt

import id.umkm.pos.pro.core.contract.ReceiptAlignment
import id.umkm.pos.pro.core.contract.ReceiptDocument

data class LayoutLine(
    val text: String,
    val alignment: ReceiptAlignment,
    val isBold: Boolean,
)

object ReceiptLayout {

    private const val SPACE = " "
    private const val LINE_BREAK = '\n'

    fun wrapText(text: String, widthChars: Int): List<String> {
        require(widthChars > 0) { "Receipt width must be positive" }
        if (text.isEmpty()) return listOf("")
        val wrappedLines = mutableListOf<String>()
        val currentLine = StringBuilder()
        for (word in text.split(' ')) {
            if (word.isEmpty()) continue
            if (currentLine.isEmpty()) {
                appendWord(word, widthChars, wrappedLines, currentLine)
            } else if (currentLine.length + 1 + word.length <= widthChars) {
                currentLine.append(' ').append(word)
            } else {
                wrappedLines.add(currentLine.toString())
                currentLine.setLength(0)
                appendWord(word, widthChars, wrappedLines, currentLine)
            }
        }
        if (currentLine.isNotEmpty()) wrappedLines.add(currentLine.toString())
        return if (wrappedLines.isEmpty()) listOf("") else wrappedLines
    }

    fun padToWidth(text: String, widthChars: Int, alignment: ReceiptAlignment): String {
        require(widthChars > 0) { "Receipt width must be positive" }
        if (text.length >= widthChars) return text
        val padding = widthChars - text.length
        return when (alignment) {
            ReceiptAlignment.START -> text + SPACE.repeat(padding)
            ReceiptAlignment.END -> SPACE.repeat(padding) + text
            ReceiptAlignment.CENTER -> {
                val leftPadding = padding / 2
                SPACE.repeat(leftPadding) + text + SPACE.repeat(padding - leftPadding)
            }
        }
    }

    fun layoutLines(receipt: ReceiptDocument, widthChars: Int): List<LayoutLine> {
        require(widthChars > 0) { "Receipt width must be positive" }
        val laidOutLines = mutableListOf<LayoutLine>()
        if (receipt.storeName.isNotBlank()) {
            appendLaidOutText(
                target = laidOutLines,
                text = receipt.storeName.trim(),
                widthChars = widthChars,
                alignment = ReceiptAlignment.CENTER,
                isBold = true,
            )
        }
        for (line in receipt.lines) {
            appendLaidOutText(
                target = laidOutLines,
                text = line.text,
                widthChars = widthChars,
                alignment = line.alignment,
                isBold = line.isBold,
            )
        }
        return laidOutLines
    }

    fun renderText(receipt: ReceiptDocument, widthChars: Int): String =
        layoutLines(receipt, widthChars).joinToString(LINE_BREAK.toString()) { it.text }

    private fun appendWord(
        word: String,
        widthChars: Int,
        wrappedLines: MutableList<String>,
        currentLine: StringBuilder,
    ) {
        var remainingWord = word
        while (remainingWord.length > widthChars) {
            wrappedLines.add(remainingWord.substring(0, widthChars))
            remainingWord = remainingWord.substring(widthChars)
        }
        currentLine.append(remainingWord)
    }

    private fun appendLaidOutText(
        target: MutableList<LayoutLine>,
        text: String,
        widthChars: Int,
        alignment: ReceiptAlignment,
        isBold: Boolean,
    ) {
        for (segment in text.split(LINE_BREAK)) {
            for (wrappedLine in wrapText(segment, widthChars)) {
                target.add(
                    LayoutLine(
                        text = padToWidth(wrappedLine, widthChars, alignment),
                        alignment = alignment,
                        isBold = isBold,
                    ),
                )
            }
        }
    }
}
