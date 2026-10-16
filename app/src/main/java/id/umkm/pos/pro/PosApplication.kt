package id.umkm.pos.pro

import android.app.Application
import id.umkm.pos.pro.app.DefaultAppContainer
import id.umkm.pos.pro.core.contract.AppContainer

class PosApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
