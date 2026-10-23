package id.umkm.pos.pro.data.seed

object DemoMarkers {

    const val NOTE: String = "SMP"
    const val LEGACY_NOTE: String = "DEMO"
    const val SKU_PREFIX: String = "SMP-"
    const val LEGACY_SKU_PREFIX: String = "DEMO-"

    fun isDemoSku(sku: String): Boolean = sku.startsWith(SKU_PREFIX)

    fun skuLikePattern(): String = "$SKU_PREFIX%"

    fun legacySkuLikePattern(): String = "$LEGACY_SKU_PREFIX%"

    fun categoryFor(baseCategory: String): String = baseCategory

    fun isDemoNote(note: String?): Boolean = note == NOTE || note == LEGACY_NOTE
}
