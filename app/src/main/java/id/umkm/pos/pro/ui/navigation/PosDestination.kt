package id.umkm.pos.pro.ui.navigation

import androidx.annotation.DrawableRes
import id.umkm.pos.pro.R

enum class PosDestination(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    BERANDA("Beranda", R.drawable.ic_nav_beranda),
    KASIR("Kasir", R.drawable.ic_nav_kasir),
    PRODUK("Produk", R.drawable.ic_nav_produk),
    LAPORAN("Laporan", R.drawable.ic_nav_laporan),
    LAINNYA("Lainnya", R.drawable.ic_nav_lainnya),
}
