package id.umkm.pos.pro

import android.app.Application
import id.umkm.pos.pro.app.AppContainer
import id.umkm.pos.pro.app.DefaultAppContainer

class PosApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
