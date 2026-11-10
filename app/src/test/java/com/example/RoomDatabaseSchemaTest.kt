package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.*
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
class RoomDatabaseSchemaTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAccountsAndTransactionsSchema() = runBlocking {
        val accountDao = db.accountDao()
        val accountId = accountDao.insertAccount(
            Account(
                name = "صندوق الكاش الرئيسي",
                code = "101",
                type = "CASH",
                balance = 500000.0,
                currency = "IQD",
                isDefault = true
            )
        )
        assertTrue(accountId > 0)

        accountDao.deposit(accountId, 100000.0)
        val updated = accountDao.getAccountById(accountId)
        assertNotNull(updated)
        assertEquals(600000.0, updated!!.balance, 0.01)

        val txId = accountDao.insertTransaction(
            AccountTransaction(
                accountId = accountId,
                accountName = "صندوق الكاش الرئيسي",
                type = "DEPOSIT",
                amount = 100000.0,
                balanceAfter = 600000.0,
                category = "مبيعات"
            )
        )
        assertTrue(txId > 0)
        val allTx = accountDao.getAllTransactions().first()
        assertEquals(1, allTx.size)
    }

    @Test
    fun testDebtsSchema() = runBlocking {
        val debtDao = db.debtDao()
        val debtId = debtDao.insertDebt(
            DebtRecord(
                debtorName = "أحمد الركابي",
                debtorPhone = "07701234567",
                debtType = "RECEIVABLE",
                initialAmount = 150000.0,
                paidAmount = 0.0,
                remainingAmount = 150000.0,
                currency = "IQD"
            )
        )
        assertTrue(debtId > 0)

        debtDao.recordDebtPayment(debtId, 50000.0, System.currentTimeMillis())
        val updated = debtDao.getDebtById(debtId)
        assertNotNull(updated)
        assertEquals(100000.0, updated!!.remainingAmount, 0.01)
        assertEquals(50000.0, updated.paidAmount, 0.01)
    }

    @Test
    fun testSuppliersSchema() = runBlocking {
        val supplierDao = db.supplierDao()
        val supId = supplierDao.insertSupplier(
            Supplier(
                name = "شركة النور للتجارة",
                phone = "07712349999",
                company = "النور",
                balanceOwed = 350000.0
            )
        )
        assertTrue(supId > 0)
        val suppliers = supplierDao.getAllSuppliers().first()
        assertEquals(1, suppliers.size)
        assertEquals("شركة النور للتجارة", suppliers[0].name)
    }

    @Test
    fun testInventoryAndMovementsSchema() = runBlocking {
        val productDao = db.productDao()
        val movementDao = db.inventoryMovementDao()

        val prodId = productDao.insertProduct(
            Product(
                name = "زيت محرك 1 لتر",
                barcode = "123456789012",
                buyPrice = 8000.0,
                sellPrice = 12000.0,
                stockQuantity = 20.0
            )
        )
        assertTrue(prodId > 0)

        productDao.decreaseStock(prodId, 5.0)
        val product = productDao.getProductById(prodId)
        assertNotNull(product)
        assertEquals(15.0, product!!.stockQuantity, 0.01)

        val movId = movementDao.insertMovement(
            InventoryMovement(
                productId = prodId,
                productName = "زيت محرك 1 لتر",
                movementType = "OUT_SALE",
                quantity = 5.0,
                unitPrice = 12000.0
            )
        )
        assertTrue(movId > 0)
        val movements = movementDao.getAllMovements().first()
        assertEquals(1, movements.size)
    }

    @Test
    fun testGeneratorExpensesSchema() = runBlocking {
        val expenseDao = db.generatorExpenseDao()
        val expId = expenseDao.insertExpense(
            GeneratorExpense(
                type = "DIESEL",
                title = "تعبئة كاز ديزل",
                amount = 130000.0,
                quantity = 150.0,
                unit = "لتر"
            )
        )
        assertTrue(expId > 0)
        val expenses = expenseDao.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals("DIESEL", expenses[0].type)
    }

    @Test
    fun testInvoicesSchema() = runBlocking {
        val invoiceDao = db.invoiceDao()
        val invId = invoiceDao.insertInvoice(
            Invoice(
                invoiceNumber = "1001",
                type = "SALE",
                customerName = "علي حسن",
                totalAmount = 50000.0,
                paidAmount = 30000.0,
                remainingDebt = 20000.0
            )
        )
        assertTrue(invId > 0)

        invoiceDao.insertInvoiceItems(
            listOf(
                InvoiceItem(
                    invoiceId = invId,
                    productName = "قاطع دورة",
                    quantity = 2.0,
                    unitPrice = 25000.0,
                    totalPrice = 50000.0
                )
            )
        )

        val items = invoiceDao.getInvoiceItemsSync(invId)
        assertEquals(1, items.size)
        assertEquals("قاطع دورة", items[0].productName)
    }
}
