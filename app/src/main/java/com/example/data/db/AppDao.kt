package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 1. عمليات الحسابات المالية والصناديق (Account DAO)
 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun deposit(id: Long, amount: Double)

    @Query("UPDATE accounts SET balance = balance - :amount WHERE id = :id")
    suspend fun withdraw(id: Long, amount: Double)

    // حركة الحسابات المالية
    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<AccountTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AccountTransaction): Long

    // Backup & Restore
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<Account>

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Query("SELECT * FROM account_transactions")
    suspend fun getAllTransactionsSync(): List<AccountTransaction>

    @Query("DELETE FROM account_transactions")
    suspend fun deleteAllTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<AccountTransaction>)
}

/**
 * 2. عمليات سجل الديون والذمم (Debts DAO)
 */
@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<DebtRecord>>

    @Query("SELECT * FROM debts WHERE debtType = 'RECEIVABLE' ORDER BY createdAt DESC")
    fun getCustomerDebts(): Flow<List<DebtRecord>>

    @Query("SELECT * FROM debts WHERE debtType = 'PAYABLE' ORDER BY createdAt DESC")
    fun getSupplierDebts(): Flow<List<DebtRecord>>

    @Query("SELECT * FROM debts WHERE remainingAmount > 0 ORDER BY dueDateMillis ASC, createdAt DESC")
    fun getActiveDebts(): Flow<List<DebtRecord>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtRecord?

    @Query("SELECT * FROM debts WHERE debtorName LIKE '%' || :query || '%' OR debtorPhone LIKE '%' || :query || '%'")
    fun searchDebts(query: String): Flow<List<DebtRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtRecord): Long

    @Update
    suspend fun updateDebt(debt: DebtRecord)

    @Delete
    suspend fun deleteDebt(debt: DebtRecord)

    @Query("UPDATE debts SET paidAmount = paidAmount + :amount, remainingAmount = remainingAmount - :amount, updatedAt = :timestamp, status = CASE WHEN remainingAmount - :amount <= 0 THEN 'PAID' ELSE 'ACTIVE' END WHERE id = :id")
    suspend fun recordDebtPayment(id: Long, amount: Double, timestamp: Long)

    // Backup & Restore
    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsSync(): List<DebtRecord>

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtRecord>)
}

/**
 * 3. عمليات الموردين (Supplier DAO)
 */
@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%' OR company LIKE '%' || :query || '%'")
    fun searchSuppliers(query: String): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET balanceOwed = balanceOwed + :amount WHERE id = :supplierId")
    suspend fun addSupplierBalance(supplierId: Long, amount: Double)

    @Query("UPDATE suppliers SET balanceOwed = balanceOwed - :amount WHERE id = :supplierId")
    suspend fun reduceSupplierBalance(supplierId: Long, amount: Double)

    // Backup & Restore
    @Query("SELECT * FROM suppliers")
    suspend fun getAllSuppliersSync(): List<Supplier>

    @Query("DELETE FROM suppliers")
    suspend fun deleteAllSuppliers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>)
}

/**
 * 4. عمليات المخزون والمواد (Product & Inventory DAO)
 */
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockAlert ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty, updatedAt = :updatedAt WHERE id = :id")
    suspend fun decreaseStock(id: Long, qty: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = stockQuantity + :qty, updatedAt = :updatedAt WHERE id = :id")
    suspend fun increaseStock(id: Long, qty: Double, updatedAt: Long = System.currentTimeMillis())

    // Backup & Restore
    @Query("SELECT * FROM products")
    suspend fun getAllProductsSync(): List<Product>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)
}

@Dao
interface InventoryMovementDao {
    @Query("SELECT * FROM inventory_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<InventoryMovement>>

    @Query("SELECT * FROM inventory_movements ORDER BY createdAt DESC")
    fun getAllMovements(): Flow<List<InventoryMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryMovement): Long

    // Backup & Restore
    @Query("SELECT * FROM inventory_movements")
    suspend fun getAllMovementsSync(): List<InventoryMovement>

    @Query("DELETE FROM inventory_movements")
    suspend fun deleteAllMovements()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<InventoryMovement>)
}

/**
 * 5. عمليات الفواتير (Invoice DAO)
 */
@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE type = :type ORDER BY createdAt DESC")
    fun getInvoicesByType(type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getInvoicesForCustomer(customerId: Long): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getInvoicesForSupplier(supplierId: Long): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItemsSync(invoiceId: Long): List<InvoiceItem>

    // Backup & Restore
    @Query("SELECT * FROM invoices")
    suspend fun getAllInvoicesSync(): List<Invoice>

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<Invoice>)

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsSync(): List<InvoiceItem>

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAllInvoiceItems()
}

/**
 * 6. عمليات صرفيات المولد (Generator Expense DAO)
 */
@Dao
interface GeneratorExpenseDao {
    @Query("SELECT * FROM generator_expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<GeneratorExpense>>

    @Query("SELECT * FROM generator_expenses WHERE type = :type ORDER BY createdAt DESC")
    fun getExpensesByType(type: String): Flow<List<GeneratorExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: GeneratorExpense): Long

    @Update
    suspend fun updateExpense(expense: GeneratorExpense)

    @Delete
    suspend fun deleteExpense(expense: GeneratorExpense)

    // Backup & Restore
    @Query("SELECT * FROM generator_expenses")
    suspend fun getAllExpensesSync(): List<GeneratorExpense>

    @Query("DELETE FROM generator_expenses")
    suspend fun deleteAllExpenses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<GeneratorExpense>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET currentDebt = currentDebt + :amount WHERE id = :customerId")
    suspend fun addCustomerDebt(customerId: Long, amount: Double)

    @Query("UPDATE customers SET currentDebt = currentDebt - :amount WHERE id = :customerId")
    suspend fun reduceCustomerDebt(customerId: Long, amount: Double)

    // Backup & Restore
    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)
}

@Dao
interface CustomerReceiptDao {
    @Query("SELECT * FROM customer_receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<CustomerReceipt>>

    @Query("SELECT * FROM customer_receipts WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getReceiptsForCustomer(customerId: Long): Flow<List<CustomerReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: CustomerReceipt): Long

    @Delete
    suspend fun deleteReceipt(receipt: CustomerReceipt)

    // Backup & Restore
    @Query("SELECT * FROM customer_receipts")
    suspend fun getAllReceiptsSync(): List<CustomerReceipt>

    @Query("DELETE FROM customer_receipts")
    suspend fun deleteAllReceipts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<CustomerReceipt>)
}

@Dao
interface AmpSubscriberDao {
    @Query("SELECT * FROM amp_subscribers ORDER BY name ASC")
    fun getAllSubscribers(): Flow<List<AmpSubscriber>>

    @Query("SELECT * FROM amp_subscribers WHERE isBillDelivered = 1 ORDER BY name ASC")
    fun getDeliveredSubscribers(): Flow<List<AmpSubscriber>>

    @Query("SELECT * FROM amp_subscribers WHERE isBillDelivered = 0 ORDER BY name ASC")
    fun getUndeliveredSubscribers(): Flow<List<AmpSubscriber>>

    @Query("SELECT * FROM amp_subscribers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchSubscribers(query: String): Flow<List<AmpSubscriber>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriber(subscriber: AmpSubscriber): Long

    @Update
    suspend fun updateSubscriber(subscriber: AmpSubscriber)

    @Delete
    suspend fun deleteSubscriber(subscriber: AmpSubscriber)

    @Query("UPDATE amp_subscribers SET isBillDelivered = :isDelivered, lastDeliveryDate = :date WHERE id = :id")
    suspend fun updateDeliveryStatus(id: Long, isDelivered: Boolean, date: Long?)

    @Query("UPDATE amp_subscribers SET debtAmount = debtAmount - :amount WHERE id = :id")
    suspend fun reduceDebt(id: Long, amount: Double)

    @Query("UPDATE amp_subscribers SET debtAmount = debtAmount + :amount WHERE id = :id")
    suspend fun addDebt(id: Long, amount: Double)

    // Backup & Restore
    @Query("SELECT * FROM amp_subscribers")
    suspend fun getAllSubscribersSync(): List<AmpSubscriber>

    @Query("DELETE FROM amp_subscribers")
    suspend fun deleteAllSubscribers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscribers(subscribers: List<AmpSubscriber>)
}

@Dao
interface SystemUserDao {
    @Query("SELECT * FROM system_users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<SystemUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: SystemUser): Long

    @Update
    suspend fun updateUser(user: SystemUser)

    @Delete
    suspend fun deleteUser(user: SystemUser)

    // Backup & Restore
    @Query("SELECT * FROM system_users")
    suspend fun getAllUsersSync(): List<SystemUser>

    @Query("DELETE FROM system_users")
    suspend fun deleteAllUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<SystemUser>)
}

@Dao
interface ScheduledNotificationDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY targetDateMillis ASC")
    fun getAllNotifications(): Flow<List<ScheduledNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: ScheduledNotification): Long

    @Update
    suspend fun updateNotification(notification: ScheduledNotification)

    @Delete
    suspend fun deleteNotification(notification: ScheduledNotification)
}

