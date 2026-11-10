package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AccountingRepository
import com.example.util.JsonBackupManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomBackupRestoreTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AccountingRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AccountingRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomDatabaseExportAndRestoreBackup() = runBlocking {
        // 1. Seed initial data
        val accountId = repository.insertAccount(
            Account(
                name = "صندوق الكاشير التجريبي",
                code = "CASH-01",
                type = "CASH",
                balance = 500000.0,
                currency = "IQD"
            )
        )

        val customerId = repository.insertCustomer(
            Customer(
                name = "حيدر الكرخي",
                phone = "07701234567",
                currentDebt = 75000.0
            )
        )

        val supplierId = repository.insertSupplier(
            Supplier(
                name = "شركة النور للمواد الغذائية",
                phone = "07809988776",
                balanceOwed = 120000.0
            )
        )

        val productId = repository.insertProduct(
            Product(
                name = "أرز بسمتي فاخر 5 كغم",
                barcode = "6291001002003",
                buyPrice = 8000.0,
                sellPrice = 11000.0,
                stockQuantity = 40.0
            )
        )

        val invoiceId = repository.createInvoice(
            Invoice(
                invoiceNumber = "INV-9901",
                type = "SALE",
                customerId = customerId,
                customerName = "حيدر الكرخي",
                totalAmount = 22000.0,
                paidAmount = 20000.0,
                remainingDebt = 2000.0
            ),
            items = listOf(
                InvoiceItem(
                    invoiceId = 0,
                    productId = productId,
                    productName = "أرز بسمتي فاخر 5 كغم",
                    quantity = 2.0,
                    unitPrice = 11000.0,
                    buyPrice = 8000.0,
                    totalPrice = 22000.0
                )
            )
        )

        repository.insertGeneratorExpense(
            GeneratorExpense(
                type = "DIESEL",
                title = "تجهيز وقود كاز للمولد",
                amount = 150000.0,
                quantity = 200.0,
                unit = "لتر"
            )
        )

        // 2. Export database to JSON
        val jsonString = repository.exportAllDataToJson()
        assertNotNull("Exported JSON string should not be null", jsonString)
        assertTrue("JSON should contain app title or identifier", jsonString.contains("Ledger") || jsonString.contains("accounting"))

        // 3. Parse and validate JSON payload
        val parsedPayload = JsonBackupManager.parseBackupJson(jsonString)
        assertEquals("Accounts count should match", 1, parsedPayload.accounts.size)
        assertEquals("Account name should match", "صندوق الكاشير التجريبي", parsedPayload.accounts[0].name)
        assertEquals("Customers count should match", 1, parsedPayload.customers.size)
        assertEquals("Customer name should match", "حيدر الكرخي", parsedPayload.customers[0].name)
        assertEquals("Suppliers count should match", 1, parsedPayload.suppliers.size)
        assertEquals("Products count should match", 1, parsedPayload.products.size)
        assertEquals("Invoices count should match", 1, parsedPayload.invoices.size)
        assertEquals("Invoice items count should match", 1, parsedPayload.invoiceItems.size)
        assertEquals("Generator expenses count should match", 1, parsedPayload.generatorExpenses.size)

        // 4. Modify database state (clear or add bogus data)
        database.accountDao().deleteAllAccounts()
        database.customerDao().deleteAllCustomers()
        database.supplierDao().deleteAllSuppliers()
        database.productDao().deleteAllProducts()
        database.invoiceDao().deleteAllInvoices()
        database.generatorExpenseDao().deleteAllExpenses()

        // Verify database is empty now
        assertEquals(0, repository.allAccounts.first().size)
        assertEquals(0, repository.allCustomers.first().size)
        assertEquals(0, repository.allSuppliers.first().size)
        assertEquals(0, repository.allProducts.first().size)
        assertEquals(0, repository.allInvoices.first().size)

        // 5. Restore data from parsed payload into Room database
        repository.restoreDataFromJson(parsedPayload, wipeBeforeRestore = true)

        // 6. Verify full recovery of Room database
        val restoredAccounts = repository.allAccounts.first()
        val restoredCustomers = repository.allCustomers.first()
        val restoredSuppliers = repository.allSuppliers.first()
        val restoredProducts = repository.allProducts.first()
        val restoredInvoices = repository.allInvoices.first()
        val restoredExpenses = repository.allGeneratorExpenses.first()

        assertEquals("Restored accounts count", 1, restoredAccounts.size)
        assertEquals("صندوق الكاشير التجريبي", restoredAccounts[0].name)
        assertEquals(500000.0, restoredAccounts[0].balance, 0.01)

        assertEquals("Restored customers count", 1, restoredCustomers.size)
        assertEquals("حيدر الكرخي", restoredCustomers[0].name)
        assertEquals(77000.0, restoredCustomers[0].currentDebt, 0.01)

        assertEquals("Restored suppliers count", 1, restoredSuppliers.size)
        assertEquals("شركة النور للمواد الغذائية", restoredSuppliers[0].name)

        assertEquals("Restored products count", 1, restoredProducts.size)
        assertEquals("أرز بسمتي فاخر 5 كغم", restoredProducts[0].name)
        assertEquals(38.0, restoredProducts[0].stockQuantity, 0.01)

        assertEquals("Restored invoices count", 1, restoredInvoices.size)
        assertEquals("INV-9901", restoredInvoices[0].invoiceNumber)

        assertEquals("Restored generator expenses count", 1, restoredExpenses.size)
        assertEquals("تجهيز وقود كاز للمولد", restoredExpenses[0].title)
    }
}
