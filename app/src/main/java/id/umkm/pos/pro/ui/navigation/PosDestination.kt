package id.umkm.pos.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.ui.graphics.vector.ImageVector

enum class PosDestination(
    val label: String,
    val icon: ImageVector,
) {
    BERANDA("Beranda", Icons.Filled.Home),
    KASIR("Kasir", Icons.Filled.PointOfSale),
    PRODUK("Produk", Icons.Filled.Inventory2),
    LAPORAN("Laporan", Icons.Filled.BarChart),
    LAINNYA("Lainnya", Icons.Filled.MoreHoriz),
}
