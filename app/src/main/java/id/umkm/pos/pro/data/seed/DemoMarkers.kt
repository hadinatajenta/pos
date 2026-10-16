package id.umkm.pos.pro.data.seed

object DemoMarkers {

    const val NOTE: String = "DEMO"
    const val SKU_PREFIX: String = "DEMO-"
    const val CATEGORY_PREFIX: String = "Demo - "

    fun isDemoSku(sku: String): Boolean = sku.startsWith(SKU_PREFIX)

    fun skuLikePattern(): String = "$SKU_PREFIX%"

    fun categoryFor(baseCategory: String): String = "$CATEGORY_PREFIX$baseCategory"

    fun isDemoCategory(category: String?): Boolean =
        category != null && category.startsWith(CATEGORY_PREFIX)

    fun isDemoNote(note: String?): Boolean = note == NOTE
}
