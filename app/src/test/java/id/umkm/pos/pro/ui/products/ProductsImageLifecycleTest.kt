package id.umkm.pos.pro.ui.products

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.AppError
import id.umkm.pos.pro.domain.usecase.FakeClock
import id.umkm.pos.pro.ui.FakeCatalogRepository
import id.umkm.pos.pro.ui.MainDispatcherRule
import id.umkm.pos.pro.ui.common.IdStrings
import id.umkm.pos.pro.ui.uiSampleProduct
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

private const val PRODUCT_ID = 1L
private const val STORED_IMAGE_PATH = "product-images/stored.jpg"

class ProductsImageLifecycleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(currentMillis = 10_000L)
    private val products = FakeCatalogRepository()
    private val images = FakeProductImageRepository()

    private fun createViewModel(): ProductsViewModel = ProductsViewModel(
        productRepository = products,
        productImageRepository = images,
        clock = clock,
    )

    private fun runPendingCoroutines() {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun seedProductWithStoredImage() {
        products.setProducts(
            listOf(uiSampleProduct(id = PRODUCT_ID, name = "Kopi Susu").copy(imagePath = STORED_IMAGE_PATH)),
        )
    }

    @Test
    fun pickingAPhotoStoresTheImportedPathOnTheForm() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()

        viewModel.onProductImagePicked("picked-1")
        runPendingCoroutines()

        val form = requireNotNull(viewModel.state.value.form)
        assertThat(form.imagePath).isEqualTo(images.storedPathFor("picked-1"))
        assertThat(form.isImageImporting).isFalse()
        assertThat(images.deletedImagePaths).isEmpty()
    }

    @Test
    fun pickingASecondPhotoDeletesTheDiscardedSessionFile() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onProductImagePicked("picked-1")

        viewModel.onProductImagePicked("picked-2")
        runPendingCoroutines()

        val form = requireNotNull(viewModel.state.value.form)
        assertThat(form.imagePath).isEqualTo(images.storedPathFor("picked-2"))
        assertThat(images.deletedImagePaths).containsExactly(images.storedPathFor("picked-1"))
    }

    @Test
    fun removingASessionPhotoDeletesTheFileAndClearsThePath() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onProductImagePicked("picked-1")

        viewModel.onProductImageRemove()
        runPendingCoroutines()

        val form = requireNotNull(viewModel.state.value.form)
        assertThat(form.imagePath).isNull()
        assertThat(form.isImageRemoved).isFalse()
        assertThat(images.deletedImagePaths).containsExactly(images.storedPathFor("picked-1"))
    }

    @Test
    fun removingTheStoredPhotoMarksItForRemovalAndKeepsTheFile() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()
        viewModel.onOpenEditForm(PRODUCT_ID)

        viewModel.onProductImageRemove()
        runPendingCoroutines()

        val form = requireNotNull(viewModel.state.value.form)
        assertThat(form.imagePath).isNull()
        assertThat(form.isImageRemoved).isTrue()
        assertThat(images.deletedImagePaths).isEmpty()
    }

    @Test
    fun savingAReplacedPhotoPersistsTheNewFileAndDeletesTheStoredOne() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()
        viewModel.onOpenEditForm(PRODUCT_ID)
        viewModel.onProductImagePicked("picked-1")

        viewModel.onSubmitForm()
        runPendingCoroutines()

        assertThat(products.currentProducts().single().imagePath).isEqualTo(images.storedPathFor("picked-1"))
        assertThat(images.deletedImagePaths).containsExactly(STORED_IMAGE_PATH)
        assertThat(viewModel.state.value.form).isNull()
        assertThat(viewModel.state.value.message?.resolve(IdStrings)).isEqualTo("Perubahan disimpan")
    }

    @Test
    fun savingAfterRemovingTheStoredPhotoClearsTheProductPhoto() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()
        viewModel.onOpenEditForm(PRODUCT_ID)
        viewModel.onProductImageRemove()

        viewModel.onSubmitForm()
        runPendingCoroutines()

        assertThat(products.currentProducts().single().imagePath).isNull()
        assertThat(images.deletedImagePaths).containsExactly(STORED_IMAGE_PATH)
    }

    @Test
    fun savingWithoutAPhotoKeepsTheStoredFile() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()
        viewModel.onOpenEditForm(PRODUCT_ID)

        viewModel.onSubmitForm()
        runPendingCoroutines()

        assertThat(products.currentProducts().single().imagePath).isEqualTo(STORED_IMAGE_PATH)
        assertThat(images.deletedImagePaths).isEmpty()
    }

    @Test
    fun dismissingTheFormDeletesTheUnsavedImportedFile() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()
        viewModel.onProductImagePicked("picked-1")

        viewModel.onDismissForm()
        runPendingCoroutines()

        assertThat(viewModel.state.value.form).isNull()
        assertThat(images.deletedImagePaths).containsExactly(images.storedPathFor("picked-1"))
    }

    @Test
    fun dismissingTheFormKeepsTheStoredPhoto() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()
        viewModel.onOpenEditForm(PRODUCT_ID)

        viewModel.onDismissForm()
        runPendingCoroutines()

        assertThat(products.currentProducts().single().imagePath).isEqualTo(STORED_IMAGE_PATH)
        assertThat(images.deletedImagePaths).isEmpty()
    }

    @Test
    fun deletingAProductDeletesItsPhotoFile() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()

        viewModel.onRequestDelete(PRODUCT_ID)
        viewModel.onConfirmDelete()
        runPendingCoroutines()

        assertThat(products.currentProducts()).isEmpty()
        assertThat(images.deletedImagePaths).containsExactly(STORED_IMAGE_PATH)
    }

    @Test
    fun anImportFailureReportsAMessageAndKeepsTheCurrentPhoto() = runTest(mainDispatcherRule.testDispatcher) {
        seedProductWithStoredImage()
        val viewModel = createViewModel()
        viewModel.onOpenEditForm(PRODUCT_ID)
        images.importFailure = AppError.Storage("decode failed")

        viewModel.onProductImagePicked("picked-1")
        runPendingCoroutines()

        val form = requireNotNull(viewModel.state.value.form)
        assertThat(form.imagePath).isEqualTo(STORED_IMAGE_PATH)
        assertThat(form.isImageImporting).isFalse()
        assertThat(viewModel.state.value.message?.resolve(IdStrings))
            .isEqualTo("Gambar gagal dimuat. Coba pilih gambar lain.")
        assertThat(images.deletedImagePaths).isEmpty()
    }

    @Test
    fun aSecondPickWhileTheImportRunsIsIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        val importGate = CompletableDeferred<Unit>()
        images.importGate = importGate
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()

        viewModel.onProductImagePicked("picked-1")
        viewModel.onProductImagePicked("picked-2")
        importGate.complete(Unit)
        runPendingCoroutines()

        assertThat(images.importedSourceUris).containsExactly("picked-1")
    }

    @Test
    fun aFormClosedWhileImportingDeletesTheOrphanedFile() = runTest(mainDispatcherRule.testDispatcher) {
        val importGate = CompletableDeferred<Unit>()
        images.importGate = importGate
        val viewModel = createViewModel()
        viewModel.onOpenCreateForm()

        viewModel.onProductImagePicked("picked-1")
        viewModel.onDismissForm()
        importGate.complete(Unit)
        runPendingCoroutines()

        assertThat(viewModel.state.value.form).isNull()
        assertThat(images.deletedImagePaths).containsExactly(images.storedPathFor("picked-1"))
    }
}
