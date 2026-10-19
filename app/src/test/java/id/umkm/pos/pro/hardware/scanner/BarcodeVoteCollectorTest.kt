package id.umkm.pos.pro.hardware.scanner

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BarcodeVoteCollectorTest {

    @Test
    fun asingleReadIsNotEnough() {
        val collector = BarcodeVoteCollector()

        val confirmed = collector.submit("8990000000001")

        assertThat(confirmed).isNull()
    }

    @Test
    fun twoMatchingReadsConfirmTheCode() {
        val collector = BarcodeVoteCollector()

        collector.submit("8990000000001")
        val confirmed = collector.submit("8990000000001")

        assertThat(confirmed).isEqualTo("8990000000001")
    }

    @Test
    fun aDifferentCodeRestartsTheCount() {
        val collector = BarcodeVoteCollector()

        collector.submit("8990000000001")
        val afterChange = collector.submit("8990000000002")

        assertThat(afterChange).isNull()
        assertThat(collector.submit("8990000000002")).isEqualTo("8990000000002")
    }

    @Test
    fun unreadableFramesDoNotResetProgress() {
        val collector = BarcodeVoteCollector()

        collector.submit("8990000000001")
        collector.submit(null)
        collector.submit("")
        val confirmed = collector.submit("8990000000001")

        assertThat(confirmed).isEqualTo("8990000000001")
    }

    @Test
    fun resetDropsTheCandidate() {
        val collector = BarcodeVoteCollector()
        collector.submit("8990000000001")

        collector.reset()

        assertThat(collector.submit("8990000000001")).isNull()
    }
}
