package id.umkm.pos.pro.ui.settings

fun requiresTaxOrRoundingGate(
    storedTaxRatePercent: Int,
    storedRoundingIncrement: Long,
    taxRatePercent: Int,
    roundingIncrement: Long,
): Boolean = taxRatePercent != storedTaxRatePercent || roundingIncrement != storedRoundingIncrement
