package id.umkm.pos.pro.ui.products

import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.core.contract.AppResult
import id.umkm.pos.pro.core.contract.ProductImageRepository
import kotlinx.coroutines.CompletableDeferred

class FakeProductImageRepository : ProductImageRepository {

    val importedSourceUris = mutableListOf<String>()
    val deletedImagePaths = mutableListOf<String>()
    var importFailure: AppError? = null
    var importGate: CompletableDeferred<Unit>? = null

    fun storedPathFor(sourceUri: String): String = "product-images/$sourceUri.jpg"

    override suspend fun importImage(sourceUri: String): AppResult<String> {
        importedSourceUris += sourceUri
        importGate?.await()
        val failure = importFailure
        if (failure != null) {
            return AppResult.Failure(failure)
        }
        return AppResult.Success(storedPathFor(sourceUri))
    }

    override suspend fun deleteImage(imagePath: String?) {
        if (imagePath != null) {
            deletedImagePaths += imagePath
        }
    }
}
