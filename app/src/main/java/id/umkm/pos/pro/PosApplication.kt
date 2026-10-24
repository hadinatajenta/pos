package id.umkm.pos.pro

import android.app.Application
import com.google.android.gms.ads.MobileAds
import id.umkm.pos.pro.app.CurrentActivityProvider
import id.umkm.pos.pro.app.DefaultAppContainer
import id.umkm.pos.pro.core.contract.AppContainer

class PosApplication : Application() {
    private val activityProvider = CurrentActivityProvider()

    val container: AppContainer by lazy {
        DefaultAppContainer(
            context = this,
            activityProvider = activityProvider,
        )
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityProvider)
        MobileAds.initialize(this)
    }
}
