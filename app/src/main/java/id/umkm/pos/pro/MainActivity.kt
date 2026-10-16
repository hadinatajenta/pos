package id.umkm.pos.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import id.umkm.pos.pro.app.DefaultAppContainer
import id.umkm.pos.pro.ui.AppRoot
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.more.LocalDemoDataSeeder
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PosApplication).container
        val demoDataSeeder = (container as? DefaultAppContainer)?.demoDataSeeder
        setContent {
            UmkmPosTheme {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalDemoDataSeeder provides demoDataSeeder,
                ) {
                    AppRoot()
                }
            }
        }
    }
}
