package id.umkm.pos.pro.ui.common

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberReceiptSharer(): (String) -> Unit {
    val context: Context = LocalContext.current
    return remember(context) {
        val shareReceipt: (String) -> Unit = { receiptText ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share receipt"))
        }
        shareReceipt
    }
}
