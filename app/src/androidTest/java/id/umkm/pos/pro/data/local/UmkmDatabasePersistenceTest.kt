package id.umkm.pos.pro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.umkm.pos.pro.core.model.Product
import id.umkm.pos.pro.data.local.repository.ProductRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UmkmDatabasePersistenceTest {

    private lateinit var context: Context

    @Before
    fun prepareContext() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun removeDatabaseFile() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun productSurvivesCloseAndReopenOnFile() = runBlocking {
        val firstDatabase = openFileDatabase()
        val productId = ProductRepositoryImpl(firstDatabase.productDao()).upsertProduct(persistedProduct())
        assertEquals(
            persistedProduct().copy(id = productId),
            ProductRepositoryImpl(firstDatabase.productDao()).getProductById(productId),
        )
        firstDatabase.close()

        val reopenedDatabase = openFileDatabase()
        val storedAfterReopen =
            ProductRepositoryImpl(reopenedDatabase.productDao()).getProductById(productId)
        assertNotNull(storedAfterReopen)
        assertEquals(persistedProduct().copy(id = productId), storedAfterReopen)
        reopenedDatabase.close()
    }

    private fun openFileDatabase(): UmkmDatabase =
        Room.databaseBuilder(context, UmkmDatabase::class.java, DATABASE_NAME)
            .allowMainThreadQueries()
            .build()

    private fun persistedProduct(): Product = Product(
        id = 0L,
        sku = "SKU-PERSIST",
        barcode = "8999001",
        name = "Kopi Bubuk",
        category = "Minuman",
        baseUnit = "pcs",
        baseHpp = 1_000L,
        basePrice = 2_000L,
        stockBaseQty = 100.0,
        minStockAlert = 5.0,
        isActive = true,
    )

    private companion object {
        const val DATABASE_NAME = "wave3-persistence-test.db"
    }
}
