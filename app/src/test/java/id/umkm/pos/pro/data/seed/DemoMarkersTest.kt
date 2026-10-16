package id.umkm.pos.pro.data.seed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DemoMarkersTest {

    @Test
    fun identifiesDemoSkuByPrefix() {
        assertThat(DemoMarkers.isDemoSku("DEMO-001")).isTrue()
        assertThat(DemoMarkers.isDemoSku("SKU-001")).isFalse()
        assertThat(DemoMarkers.isDemoSku("demo-001")).isFalse()
    }

    @Test
    fun skuLikePatternTargetsTheDemoPrefix() {
        assertThat(DemoMarkers.skuLikePattern()).isEqualTo("DEMO-%")
    }

    @Test
    fun categoryForAddsMarkerPrefix() {
        val category = DemoMarkers.categoryFor("Minuman")

        assertThat(category).isEqualTo("Demo - Minuman")
        assertThat(DemoMarkers.isDemoCategory(category)).isTrue()
    }

    @Test
    fun demoCategoryRejectsNullAndPlainCategories() {
        assertThat(DemoMarkers.isDemoCategory(null)).isFalse()
        assertThat(DemoMarkers.isDemoCategory("Minuman")).isFalse()
    }

    @Test
    fun demoNoteMatchesOnlyTheMarkerNote() {
        assertThat(DemoMarkers.isDemoNote("DEMO")).isTrue()
        assertThat(DemoMarkers.isDemoNote("demo")).isFalse()
        assertThat(DemoMarkers.isDemoNote(null)).isFalse()
    }
}
