package id.umkm.pos.pro.data.local

import android.content.Context
import id.umkm.pos.pro.core.contract.OnboardingStore

internal const val ONBOARDING_PREFERENCES = "umkm_pos_onboarding"
internal const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

class OnboardingPreferences(context: Context) : OnboardingStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(ONBOARDING_PREFERENCES, Context.MODE_PRIVATE)

    override fun hasCompletedOnboarding(): Boolean =
        preferences.getBoolean(ONBOARDING_COMPLETED_KEY, false)

    override fun markOnboardingCompleted() {
        preferences.edit().putBoolean(ONBOARDING_COMPLETED_KEY, true).apply()
    }
}
