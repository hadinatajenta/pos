package id.umkm.pos.pro.ui.navigation

import androidx.annotation.DrawableRes
import id.umkm.pos.pro.R

enum class PosDestination(
    @DrawableRes val iconRes: Int,
) {
    KASIR(R.drawable.ic_shopping_cart),
    PRODUK(R.drawable.ic_package),
    TRANSAKSI(R.drawable.ic_receipt),
    LAPORAN(R.drawable.ic_chart_column),
    LAINNYA(R.drawable.ic_ellipsis),
}
