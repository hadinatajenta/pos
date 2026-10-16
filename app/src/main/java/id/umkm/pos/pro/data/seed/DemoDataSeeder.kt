package id.umkm.pos.pro.data.seed

import id.umkm.pos.pro.core.contract.AppResult

data class DemoSeedSummary(
    val products: Int,
    val customers: Int,
    val transactions: Int,
    val debtPayments: Int,
    val returns: Int,
)

data class DemoClearSummary(
    val products: Int,
    val customers: Int,
    val transactions: Int,
)

interface DemoDataSeeder {

    suspend fun load(): AppResult<DemoSeedSummary>

    suspend fun clear(): AppResult<DemoClearSummary>
}
