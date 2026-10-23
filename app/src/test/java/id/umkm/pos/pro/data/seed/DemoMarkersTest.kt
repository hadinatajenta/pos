package id.umkm.pos.pro.data.seed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DemoMarkersTest {

    @Test
    fun identifiesDemoSkuByPrefix() {
        assertThat(DemoMarkers.isDemoSku("SMP-001")).isTrue()
        assertThat(DemoMarkers.isDemoSku("SKU-001")).isFalse()
        assertThat(DemoMarkers.isDemoSku("demo-001")).isFalse()
    }

    @Test
    fun skuLikePatternTargetsTheSamplePrefix() {
        assertThat(DemoMarkers.skuLikePattern()).isEqualTo("SMP-%")
    }

    @Test
    fun legacySkuPatternStillTargetsOldDemoProducts() {
        assertThat(DemoMarkers.legacySkuLikePattern()).isEqualTo("DEMO-%")
    }

    @Test
    fun categoryForKeepsThePlainCategory() {
        assertThat(DemoMarkers.categoryFor("Minuman")).isEqualTo("Minuman")
    }

    @Test
    fun demoNoteMatchesTheCurrentAndLegacyMarkers() {
        assertThat(DemoMarkers.isDemoNote("SMP")).isTrue()
        assertThat(DemoMarkers.isDemoNote("DEMO")).isTrue()
        assertThat(DemoMarkers.isDemoNote("demo")).isFalse()
        assertThat(DemoMarkers.isDemoNote(null)).isFalse()
    }
}
