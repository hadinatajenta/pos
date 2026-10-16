package id.umkm.pos.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector

enum class PosDestination(
    val label: String,
    val icon: ImageVector,
) {
    CASHIER("Cashier", Icons.Filled.PointOfSale),
    PRODUCTS("Products", Icons.Filled.Inventory2),
    CUSTOMERS("Customers", Icons.Filled.Groups),
    REPORTS("Reports", Icons.Filled.QueryStats),
    MORE("More", Icons.Filled.MoreHoriz),
}
