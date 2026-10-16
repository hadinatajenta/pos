package id.umkm.pos.pro.feature.reports

import kotlin.math.abs

object Rupiah {

    fun format(amount: Long): String {
        val groupedDigits = abs(amount)
            .toString()
            .reversed()
            .chunked(3)
            .joinToString(separator = ".")
            .reversed()
        return if (amount < 0L) "-Rp $groupedDigits" else "Rp $groupedDigits"
    }
}
