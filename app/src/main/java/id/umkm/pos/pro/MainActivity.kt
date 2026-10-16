package id.umkm.pos.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.umkm.pos.pro.app.PosApp
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UmkmPosTheme {
                PosApp()
            }
        }
    }
}
