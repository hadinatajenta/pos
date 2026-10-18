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

    fun charWidth(ch: Char): Int =
        if (ch.code in 0x1100..0x115F ||
            ch.code in 0x2E80..0x9FFF ||
            ch.code in 0xAC00..0xD7A3 ||
            ch.code in 0xF900..0xFAFF ||
            ch.code in 0xFE10..0xFE19 ||
            ch.code in 0xFE30..0xFE6F ||
            ch.code in 0xFF00..0xFF60 ||
            ch.code in 0xFFE0..0xFFE6
        ) 2 else 1

    fun stringDisplayWidth(str: String): Int = str.sumOf { charWidth(it) }

    fun wrapText(text: String, widthChars: Int): List<String> {
        require(widthChars > 0) { "Receipt width must be positive" }
        if (text.isEmpty()) return listOf("")
        val wrappedLines = mutableListOf<String>()
        val currentLine = StringBuilder()
        var currentLineWidth = 0
        for (word in text.split(' ')) {
            if (word.isEmpty()) continue
            val wordWidth = stringDisplayWidth(word)
            if (currentLine.isEmpty()) {
                appendWord(word, widthChars, wrappedLines, currentLine)
                currentLineWidth = stringDisplayWidth(currentLine.toString())
            } else if (currentLineWidth + 1 + wordWidth <= widthChars) {
                currentLine.append(' ').append(word)
                currentLineWidth += 1 + wordWidth
            } else {
                wrappedLines.add(currentLine.toString())
                currentLine.setLength(0)
                appendWord(word, widthChars, wrappedLines, currentLine)
                currentLineWidth = stringDisplayWidth(currentLine.toString())
            }
        }
        if (currentLine.isNotEmpty()) wrappedLines.add(currentLine.toString())
        return if (wrappedLines.isEmpty()) listOf("") else wrappedLines
    }

    fun padToWidth(text: String, widthChars: Int, alignment: ReceiptAlignment): String {
        require(widthChars > 0) { "Receipt width must be positive" }
        val displayWidth = stringDisplayWidth(text)
        if (displayWidth >= widthChars) return text
        val padding = widthChars - displayWidth
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
        while (stringDisplayWidth(remainingWord) > widthChars) {
            var takeCount = 0
            var accumWidth = 0
            for (ch in remainingWord) {
                val w = charWidth(ch)
                if (accumWidth + w > widthChars) break
                accumWidth += w
                takeCount++
            }
            if (takeCount == 0) takeCount = 1
            wrappedLines.add(remainingWord.substring(0, takeCount))
            remainingWord = remainingWord.substring(takeCount)
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
