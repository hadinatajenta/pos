package id.umkm.pos.pro.ui.navigation

import androidx.annotation.DrawableRes
import id.umkm.pos.pro.R

enum class PosDestination(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    BERANDA("Beranda", R.drawable.ic_house),
    KASIR("Kasir", R.drawable.ic_shopping_cart),
    PRODUK("Produk", R.drawable.ic_package),
    LAPORAN("Laporan", R.drawable.ic_chart_column),
    LAINNYA("Lainnya", R.drawable.ic_ellipsis),
}
