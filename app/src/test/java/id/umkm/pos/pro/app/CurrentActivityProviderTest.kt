package id.umkm.pos.pro.app

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CurrentActivityProviderTest {

    @Test
    fun tracksResumedActivityAndClearsOnPauseOrDestroy() {
        val provider = CurrentActivityProvider()
        assertThat(provider.currentActivity).isNull()

        val activity = Activity()
        provider.onActivityResumed(activity)
        assertThat(provider.currentActivity).isSameInstanceAs(activity)

        provider.onActivityPaused(activity)
        assertThat(provider.currentActivity).isNull()

        provider.onActivityResumed(activity)
        val anotherActivity = Activity()
        provider.onActivityDestroyed(anotherActivity)
        assertThat(provider.currentActivity).isSameInstanceAs(activity)

        provider.onActivityDestroyed(activity)
        assertThat(provider.currentActivity).isNull()
    }
}
