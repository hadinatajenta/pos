package id.umkm.pos.pro.ui.navigation

import androidx.annotation.DrawableRes
import id.umkm.pos.pro.R

enum class PosDestination(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    KASIR("Kasir", R.drawable.ic_shopping_cart),
    PRODUK("Produk", R.drawable.ic_package),
    TRANSAKSI("Transaksi", R.drawable.ic_receipt),
    LAPORAN("Laporan", R.drawable.ic_chart_column),
    LAINNYA("Lainnya", R.drawable.ic_ellipsis),
}
