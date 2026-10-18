package id.umkm.pos.pro.data.backup

import com.google.common.truth.Truth.assertThat
import java.util.TimeZone
import org.junit.Test

class BackupFileNamesTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun buildsATimestampedJsonName() {
        val epochMillis = 1_700_000_000_000L

        val fileName = BackupFileNames.forTimestamp(epochMillis, utc)

        assertThat(fileName).isEqualTo("umkm-pos-backup-20231114-221320.json")
    }

    @Test
    fun differentTimestampsProduceDifferentNames() {
        val first = BackupFileNames.forTimestamp(1_700_000_000_000L, utc)
        val second = BackupFileNames.forTimestamp(1_700_000_060_000L, utc)

        assertThat(first).isNotEqualTo(second)
    }
}
