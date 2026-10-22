package id.umkm.pos.pro.core.model

data class Staff(
    val id: Long,
    val name: String,
    val role: SecurityRole = SecurityRole.CASHIER,
    val isActive: Boolean = true,
)
