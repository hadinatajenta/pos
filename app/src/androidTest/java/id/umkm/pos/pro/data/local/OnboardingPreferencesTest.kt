package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPreferencesTest {

    @Test
    fun reportsCompletionOnlyAfterItIsMarked() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(ONBOARDING_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val store = OnboardingPreferences(context)

        assertFalse(store.hasCompletedOnboarding())

        store.markOnboardingCompleted()

        assertTrue(store.hasCompletedOnboarding())
        assertTrue(OnboardingPreferences(context).hasCompletedOnboarding())
    }
}
