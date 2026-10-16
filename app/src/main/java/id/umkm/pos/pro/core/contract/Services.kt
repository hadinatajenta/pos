package id.umkm.pos.pro.core.contract

import id.umkm.pos.pro.core.model.CartLine
import id.umkm.pos.pro.core.model.PricedCart
import id.umkm.pos.pro.core.model.PricedCartLine
import id.umkm.pos.pro.core.model.PricingContext
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.core.model.ProductUnit
import id.umkm.pos.pro.core.model.ReportRange
import kotlinx.coroutines.flow.Flow

interface PriceEngine {
    fun priceLine(line: CartLine, product: Product, units: List<ProductUnit>): PricedCartLine
    fun priceCart(
        lines: List<CartLine>,
        products: Map<Long, Product>,
        unitsByProduct: Map<Long, List<ProductUnit>>,
        context: PricingContext,
    ): PricedCart
}

interface AccountingService {
    fun netProfit(pricedCart: PricedCart): Long
}

interface Clock {
    fun nowMillis(): Long
}

data class BackupArtifact(val path: String, val byteCount: Long)

interface BackupService {
    suspend fun createLocalBackup(): AppResult<BackupArtifact>
    suspend fun restoreLocalBackup(artifact: BackupArtifact): AppResult<Unit>
    suspend fun createCloudBackup(): AppResult<BackupArtifact>
    suspend fun restoreCloudBackup(): AppResult<Unit>
}

interface ImportExportService {
    suspend fun exportCsv(range: ReportRange, targetPath: String): AppResult<BackupArtifact>
    suspend fun exportXlsx(range: ReportRange, targetPath: String): AppResult<BackupArtifact>
    suspend fun exportJson(targetPath: String): AppResult<BackupArtifact>
    suspend fun importJson(sourcePath: String): AppResult<Unit>
}

enum class AdPlacement {
    DEBT_LIST,
    STOCK_MANAGEMENT,
    EXCEL_EXPORT,
    CLOUD_BACKUP,
    SHELF_LABEL_PRINT,
}

interface AdController {
    fun observeAvailability(placement: AdPlacement): Flow<Boolean>
    suspend fun showRewarded(placement: AdPlacement): AppResult<Unit>
}
