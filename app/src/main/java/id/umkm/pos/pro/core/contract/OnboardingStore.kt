package id.umkm.pos.pro.core.contract

interface OnboardingStore {
    fun hasCompletedOnboarding(): Boolean
    fun markOnboardingCompleted()
}
