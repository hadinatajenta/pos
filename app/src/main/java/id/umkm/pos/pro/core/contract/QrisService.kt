package id.umkm.pos.pro.core.contract

data class QrisImport(
    val imagePath: String,
    val payload: String,
)

interface QrisService {
    suspend fun importStaticQris(sourceUri: String): AppResult<QrisImport>
    suspend fun deleteQrisImage(imagePath: String?)
}
