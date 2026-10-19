package id.umkm.pos.pro

import android.view.KeyEvent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import id.umkm.pos.pro.app.DefaultAppContainer
import id.umkm.pos.pro.hardware.scanner.HardwareScannerInput
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

    private val hidScanner: HardwareScannerInput?
        get() = ((application as? PosApplication)?.container as? DefaultAppContainer)?.hidScanner

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        hidScanner?.handleKeyEvent(event) == true || super.dispatchKeyEvent(event)
}
