package id.umkm.pos.pro.ui.navigation

import androidx.annotation.DrawableRes
import id.umkm.pos.pro.R

enum class PosDestination(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    BERANDA("Beranda", R.drawable.ic_house),
    PRODUK("Produk", R.drawable.ic_package),
    KASIR("Kasir", R.drawable.ic_shopping_cart),
    TRANSAKSI("Transaksi", R.drawable.ic_receipt),
    LAPORAN("Laporan", R.drawable.ic_chart_column),
    LAINNYA("Lainnya", R.drawable.ic_ellipsis),
}
