package id.umkm.pos.pro.core.contract

interface ProductImageRepository {
    suspend fun importImage(sourceUri: String): AppResult<String>
    suspend fun deleteImage(imagePath: String?)
}
