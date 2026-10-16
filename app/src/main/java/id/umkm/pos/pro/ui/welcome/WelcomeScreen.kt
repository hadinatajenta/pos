package id.umkm.pos.pro.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

const val WELCOME_SCREEN_TEST_TAG = "welcome-screen"

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosColors.GoldContainer)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = PosSpacing.lg)
            .testTag(WELCOME_SCREEN_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(PosSpacing.xl))
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Kelola bisnis jadi lebih mudah",
                style = MaterialTheme.typography.headlineMedium,
                color = PosColors.TextInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(PosSpacing.sm))
            Text(
                text = "Catat transaksi, pantau stok, dan lihat laporan penjualan dalam satu aplikasi. Semua tetap jalan tanpa internet.",
                style = MaterialTheme.typography.bodyLarge,
                color = PosColors.TextInk,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.welcome_illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            )
        }
        PrimaryActionButton(text = "Mulai", onClick = onStart)
        Spacer(modifier = Modifier.height(PosSpacing.lg))
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    UmkmPosTheme {
        WelcomeScreen(onStart = {})
    }
}
