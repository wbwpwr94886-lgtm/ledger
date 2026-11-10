package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.util.JsonBackupManager
import com.example.util.ParsedBackupPayload
import kotlinx.coroutines.flow.Flow

class AccountingRepository(private val database: AppDatabase) {
    private val customerDao = database.customerDao()
    private val accountDao = database.accountDao()
    private val debtDao = database.debtDao()
    private val supplierDao = database.supplierDao()
    private val productDao = database.productDao()
    private val movementDao = database.inventoryMovementDao()
    private val invoiceDao = database.invoiceDao()
    private val receiptDao = database.customerReceiptDao()
    private val ampDao = database.ampSubscriberDao()
    private val userDao = database.systemUserDao()
    private val notificationDao = database.scheduledNotificationDao()
    private val generatorExpenseDao = database.generatorExpenseDao()

    // ==========================================
    // 1. الحسابات المالية والصناديق (Accounts)
    // ==========================================
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allAccountTransactions: Flow<List<AccountTransaction>> = accountDao.getAllTransactions()
    fun getTransactionsForAccount(accountId: Long) = accountDao.getTransactionsForAccount(accountId)
    suspend fun getAccountById(id: Long) = accountDao.getAccountById(id)
    suspend fun getDefaultAccount() = accountDao.getDefaultAccount()
    suspend fun insertAccount(account: Account) = accountDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)

    suspend fun depositToAccount(accountId: Long, amount: Double, category: String, notes: String, refType: String = "MANUAL", refId: Long? = null) {
        val acc = accountDao.getAccountById(accountId) ?: return
        accountDao.deposit(accountId, amount)
        accountDao.insertTransaction(
            AccountTransaction(
                accountId = accountId,
                accountName = acc.name,
                type = "DEPOSIT",
                amount = amount,
                balanceAfter = acc.balance + amount,
                category = category,
                referenceType = refType,
                referenceId = refId,
                notes = notes
            )
        )
    }

    suspend fun withdrawFromAccount(accountId: Long, amount: Double, category: String, notes: String, refType: String = "MANUAL", refId: Long? = null) {
        val acc = accountDao.getAccountById(accountId) ?: return
        accountDao.withdraw(accountId, amount)
        accountDao.insertTransaction(
            AccountTransaction(
                accountId = accountId,
                accountName = acc.name,
                type = "WITHDRAW",
                amount = amount,
                balanceAfter = acc.balance - amount,
                category = category,
                referenceType = refType,
                referenceId = refId,
                notes = notes
            )
        )
    }

    suspend fun transferBetweenAccounts(fromId: Long, toId: Long, amount: Double, notes: String) {
        val fromAcc = accountDao.getAccountById(fromId) ?: return
        val toAcc = accountDao.getAccountById(toId) ?: return

        accountDao.withdraw(fromId, amount)
        accountDao.deposit(toId, amount)

        accountDao.insertTransaction(
            AccountTransaction(
                accountId = fromId,
                accountName = fromAcc.name,
                type = "TRANSFER_OUT",
                amount = amount,
                balanceAfter = fromAcc.balance - amount,
                category = "تحويل مالي",
                referenceType = "TRANSFER",
                referenceId = toId,
                notes = "تحويل إلى: ${toAcc.name} - $notes"
            )
        )
        accountDao.insertTransaction(
            AccountTransaction(
                accountId = toId,
                accountName = toAcc.name,
                type = "TRANSFER_IN",
                amount = amount,
                balanceAfter = toAcc.balance + amount,
                category = "تحويل مالي",
                referenceType = "TRANSFER",
                referenceId = fromId,
                notes = "تحويل وارد من: ${fromAcc.name} - $notes"
            )
        )
    }

    // ==========================================
    // 2. سجل الديون والذمم (Debts Ledger)
    // ==========================================
    val allDebts: Flow<List<DebtRecord>> = debtDao.getAllDebts()
    val customerDebts: Flow<List<DebtRecord>> = debtDao.getCustomerDebts()
    val supplierDebts: Flow<List<DebtRecord>> = debtDao.getSupplierDebts()
    val activeDebts: Flow<List<DebtRecord>> = debtDao.getActiveDebts()
    fun searchDebts(query: String) = debtDao.searchDebts(query)
    suspend fun getDebtById(id: Long) = debtDao.getDebtById(id)
    suspend fun insertDebt(debt: DebtRecord) = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: DebtRecord) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(debt: DebtRecord) = debtDao.deleteDebt(debt)

    suspend fun recordDebtPayment(debtId: Long, amount: Double, notes: String = "") {
        val debt = debtDao.getDebtById(debtId) ?: return
        val now = System.currentTimeMillis()
        debtDao.recordDebtPayment(debtId, amount, now)

        // If linked to a customer, also decrease customer's currentDebt
        if (debt.relatedCustomerId != null) {
            customerDao.reduceCustomerDebt(debt.relatedCustomerId, amount)
        }
        // If linked to a supplier, also decrease supplier's balanceOwed
        if (debt.relatedSupplierId != null) {
            supplierDao.reduceSupplierBalance(debt.relatedSupplierId, amount)
        }
    }

    // ==========================================
    // 3. الموردون (Suppliers)
    // ==========================================
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    fun searchSuppliers(query: String) = supplierDao.searchSuppliers(query)
    suspend fun insertSupplier(supplier: Supplier): Long {
        val id = supplierDao.insertSupplier(supplier)
        if (supplier.balanceOwed > 0) {
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = supplier.name,
                    debtorPhone = supplier.phone,
                    debtType = "PAYABLE",
                    relatedSupplierId = id,
                    initialAmount = supplier.balanceOwed,
                    paidAmount = 0.0,
                    remainingAmount = supplier.balanceOwed,
                    notes = "رصيد افتتاحي مستحق للمورد"
                )
            )
        }
        return id
    }
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.deleteSupplier(supplier)

    // ==========================================
    // 4. المخزون والمواد (Inventory & Products)
    // ==========================================
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allInventoryMovements: Flow<List<InventoryMovement>> = movementDao.getAllMovements()
    fun getMovementsForProduct(productId: Long) = movementDao.getMovementsForProduct(productId)
    fun searchProducts(query: String) = productDao.searchProducts(query)
    suspend fun getProductByBarcode(barcode: String) = productDao.getProductByBarcode(barcode)
    suspend fun getProductById(id: Long) = productDao.getProductById(id)
    suspend fun insertProduct(product: Product): Long {
        val id = productDao.insertProduct(product)
        if (product.stockQuantity > 0) {
            movementDao.insertMovement(
                InventoryMovement(
                    productId = id,
                    productName = product.name,
                    movementType = "IN_PURCHASE",
                    quantity = product.stockQuantity,
                    unitPrice = product.buyPrice,
                    notes = "رصيد افتتاحي للمنتج"
                )
            )
        }
        return id
    }
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun recordStockMovement(movement: InventoryMovement) {
        movementDao.insertMovement(movement)
        when (movement.movementType) {
            "IN_PURCHASE", "RETURN" -> productDao.increaseStock(movement.productId, movement.quantity)
            "OUT_SALE" -> productDao.decreaseStock(movement.productId, movement.quantity)
            "ADJUSTMENT" -> {
                // If positive quantity, increase; if negative, decrease
                if (movement.quantity >= 0) {
                    productDao.increaseStock(movement.productId, movement.quantity)
                } else {
                    productDao.decreaseStock(movement.productId, -movement.quantity)
                }
            }
        }
    }

    // ==========================================
    // 5. الفواتير والمبيعات (Invoices)
    // ==========================================
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    fun getInvoicesForCustomer(customerId: Long) = invoiceDao.getInvoicesForCustomer(customerId)
    fun getInvoicesForSupplier(supplierId: Long) = invoiceDao.getInvoicesForSupplier(supplierId)
    fun getInvoicesByType(type: String) = invoiceDao.getInvoicesByType(type)
    fun getInvoiceItems(invoiceId: Long) = invoiceDao.getInvoiceItems(invoiceId)
    suspend fun getInvoiceItemsSync(invoiceId: Long) = invoiceDao.getInvoiceItemsSync(invoiceId)

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = invoiceDao.insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        invoiceDao.insertInvoiceItems(itemsWithId)

        // 1. Decrement product stock and record inventory movement
        for (item in items) {
            item.productId?.let { pId ->
                productDao.decreaseStock(pId, item.quantity)
                movementDao.insertMovement(
                    InventoryMovement(
                        productId = pId,
                        productName = item.productName,
                        movementType = "OUT_SALE",
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        referenceId = invoiceId,
                        notes = "فاتورة بيع #${invoice.invoiceNumber}"
                    )
                )
            }
        }

        // 2. Add remaining debt to customer and create DebtRecord if applicable
        if (invoice.customerId != null && invoice.remainingDebt > 0) {
            customerDao.addCustomerDebt(invoice.customerId, invoice.remainingDebt)
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = invoice.customerName,
                    debtorPhone = invoice.customerPhone,
                    debtType = "RECEIVABLE",
                    relatedCustomerId = invoice.customerId,
                    initialAmount = invoice.remainingDebt,
                    paidAmount = 0.0,
                    remainingAmount = invoice.remainingDebt,
                    dueDateMillis = invoice.dueDateMillis,
                    notes = "دين فاتورة بيع #${invoice.invoiceNumber}"
                )
            )
        }

        // 3. Deposit paid amount into financial Account if paid > 0
        if (invoice.paidAmount > 0) {
            val targetAccountId = invoice.accountId ?: accountDao.getDefaultAccount()?.id
            targetAccountId?.let { accId ->
                val acc = accountDao.getAccountById(accId)
                if (acc != null) {
                    accountDao.deposit(accId, invoice.paidAmount)
                    accountDao.insertTransaction(
                        AccountTransaction(
                            accountId = accId,
                            accountName = acc.name,
                            type = "DEPOSIT",
                            amount = invoice.paidAmount,
                            balanceAfter = acc.balance + invoice.paidAmount,
                            category = "مبيعات",
                            referenceType = "INVOICE",
                            referenceId = invoiceId,
                            notes = "سداد فاتورة بيع #${invoice.invoiceNumber} - ${invoice.customerName}"
                        )
                    )
                }
            }
        }

        return invoiceId
    }

    suspend fun deleteInvoice(invoice: Invoice) = invoiceDao.deleteInvoice(invoice)

    // ==========================================
    // 6. صرف ومصاريف المولد (Generator Expenses)
    // ==========================================
    val allGeneratorExpenses: Flow<List<GeneratorExpense>> = generatorExpenseDao.getAllExpenses()
    fun getExpensesByType(type: String) = generatorExpenseDao.getExpensesByType(type)

    suspend fun insertGeneratorExpense(expense: GeneratorExpense): Long {
        val id = generatorExpenseDao.insertExpense(expense)
        // If an account is specified (or default cash account), withdraw expense amount
        val accountId = expense.paidFromAccountId ?: accountDao.getDefaultAccount()?.id
        accountId?.let { accId ->
            val acc = accountDao.getAccountById(accId)
            if (acc != null && expense.amount > 0) {
                accountDao.withdraw(accId, expense.amount)
                accountDao.insertTransaction(
                    AccountTransaction(
                        accountId = accId,
                        accountName = acc.name,
                        type = "WITHDRAW",
                        amount = expense.amount,
                        balanceAfter = acc.balance - expense.amount,
                        category = "صرفيات مولد (${expense.type})",
                        referenceType = "EXPENSE",
                        referenceId = id,
                        notes = "${expense.title} - ${expense.notes}"
                    )
                )
            }
        }
        return id
    }

    suspend fun updateGeneratorExpense(expense: GeneratorExpense) = generatorExpenseDao.updateExpense(expense)
    suspend fun deleteGeneratorExpense(expense: GeneratorExpense) = generatorExpenseDao.deleteExpense(expense)

    // ==========================================
    // العملاء وسندات القبض (Customers & Receipts)
    // ==========================================
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    fun searchCustomers(query: String) = customerDao.searchCustomers(query)
    suspend fun insertCustomer(customer: Customer): Long {
        val id = customerDao.insertCustomer(customer)
        if (customer.currentDebt > 0) {
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = customer.name,
                    debtorPhone = customer.phone,
                    debtType = "RECEIVABLE",
                    relatedCustomerId = id,
                    initialAmount = customer.currentDebt,
                    paidAmount = 0.0,
                    remainingAmount = customer.currentDebt,
                    dueDateMillis = customer.dueDateMillis,
                    notes = "رصيد افتتاحي دين على العميل"
                )
            )
        }
        return id
    }
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    val allReceipts: Flow<List<CustomerReceipt>> = receiptDao.getAllReceipts()
    fun getReceiptsForCustomer(customerId: Long) = receiptDao.getReceiptsForCustomer(customerId)
    suspend fun addReceipt(receipt: CustomerReceipt, accountId: Long? = null) {
        receiptDao.insertReceipt(receipt)
        customerDao.reduceCustomerDebt(receipt.customerId, receipt.amount)

        // Deposit receipt amount into treasury account
        val targetAccId = accountId ?: accountDao.getDefaultAccount()?.id
        targetAccId?.let { accId ->
            val acc = accountDao.getAccountById(accId)
            if (acc != null) {
                accountDao.deposit(accId, receipt.amount)
                accountDao.insertTransaction(
                    AccountTransaction(
                        accountId = accId,
                        accountName = acc.name,
                        type = "DEPOSIT",
                        amount = receipt.amount,
                        balanceAfter = acc.balance + receipt.amount,
                        category = "سند قبض عميل",
                        referenceType = "DEBT_PAYMENT",
                        referenceId = receipt.customerId,
                        notes = "قبض من ${receipt.customerName} - سند #${receipt.receiptNumber}"
                    )
                )
            }
        }
    }

    // ==========================================
    // الأمبيرات (Amperes)
    // ==========================================
    val allAmpSubscribers: Flow<List<AmpSubscriber>> = ampDao.getAllSubscribers()
    val deliveredSubscribers: Flow<List<AmpSubscriber>> = ampDao.getDeliveredSubscribers()
    val undeliveredSubscribers: Flow<List<AmpSubscriber>> = ampDao.getUndeliveredSubscribers()
    fun searchAmpSubscribers(query: String) = ampDao.searchSubscribers(query)

    suspend fun insertAmpSubscriber(subscriber: AmpSubscriber) = ampDao.insertSubscriber(subscriber)
    suspend fun updateAmpSubscriber(subscriber: AmpSubscriber) = ampDao.updateSubscriber(subscriber)
    suspend fun deleteAmpSubscriber(subscriber: AmpSubscriber) = ampDao.deleteSubscriber(subscriber)

    suspend fun toggleAmpDelivery(subscriberId: Long, currentStatus: Boolean) {
        val newStatus = !currentStatus
        val date = if (newStatus) System.currentTimeMillis() else null
        ampDao.updateDeliveryStatus(subscriberId, newStatus, date)
    }

    suspend fun recordAmpPayment(subscriberId: Long, amount: Double) {
        ampDao.reduceDebt(subscriberId, amount)
    }

    // ==========================================
    // المستخدمين والإشعارات (Users & Notifications)
    // ==========================================
    val allUsers: Flow<List<SystemUser>> = userDao.getAllUsers()
    suspend fun insertUser(user: SystemUser) = userDao.insertUser(user)
    suspend fun updateUser(user: SystemUser) = userDao.updateUser(user)
    suspend fun deleteUser(user: SystemUser) = userDao.deleteUser(user)

    val allNotifications: Flow<List<ScheduledNotification>> = notificationDao.getAllNotifications()
    suspend fun insertNotification(notification: ScheduledNotification) = notificationDao.insertNotification(notification)
    suspend fun deleteNotification(notification: ScheduledNotification) = notificationDao.deleteNotification(notification)

    // ==========================================
    // النسخ الاحتياطي والاستعادة المحلية (Local JSON Backup & Restore)
    // ==========================================
    suspend fun exportAllDataToJson(): String {
        val accounts = accountDao.getAllAccountsSync()
        val accountTransactions = accountDao.getAllTransactionsSync()
        val debts = debtDao.getAllDebtsSync()
        val suppliers = supplierDao.getAllSuppliersSync()
        val products = productDao.getAllProductsSync()
        val movements = movementDao.getAllMovementsSync()
        val invoices = invoiceDao.getAllInvoicesSync()
        val invoiceItems = invoiceDao.getAllInvoiceItemsSync()
        val generatorExpenses = generatorExpenseDao.getAllExpensesSync()
        val customers = customerDao.getAllCustomersSync()
        val customerReceipts = receiptDao.getAllReceiptsSync()
        val ampSubscribers = ampDao.getAllSubscribersSync()
        val systemUsers = userDao.getAllUsersSync()

        return JsonBackupManager.createBackupJson(
            accounts = accounts,
            accountTransactions = accountTransactions,
            debts = debts,
            suppliers = suppliers,
            products = products,
            inventoryMovements = movements,
            invoices = invoices,
            invoiceItems = invoiceItems,
            generatorExpenses = generatorExpenses,
            customers = customers,
            customerReceipts = customerReceipts,
            ampSubscribers = ampSubscribers,
            systemUsers = systemUsers
        )
    }

    suspend fun restoreDataFromJson(payload: ParsedBackupPayload, wipeBeforeRestore: Boolean = true) {
        database.withTransaction {
            if (wipeBeforeRestore) {
                // Clear tables in reverse dependency order
                invoiceDao.deleteAllInvoiceItems()
                invoiceDao.deleteAllInvoices()
                movementDao.deleteAllMovements()
                productDao.deleteAllProducts()
                receiptDao.deleteAllReceipts()
                ampDao.deleteAllSubscribers()
                generatorExpenseDao.deleteAllExpenses()
                debtDao.deleteAllDebts()
                supplierDao.deleteAllSuppliers()
                customerDao.deleteAllCustomers()
                accountDao.deleteAllTransactions()
                accountDao.deleteAllAccounts()
                userDao.deleteAllUsers()
            }

            if (payload.accounts.isNotEmpty()) accountDao.insertAccounts(payload.accounts)
            if (payload.accountTransactions.isNotEmpty()) accountDao.insertTransactions(payload.accountTransactions)
            if (payload.customers.isNotEmpty()) customerDao.insertCustomers(payload.customers)
            if (payload.suppliers.isNotEmpty()) supplierDao.insertSuppliers(payload.suppliers)
            if (payload.debts.isNotEmpty()) debtDao.insertDebts(payload.debts)
            if (payload.products.isNotEmpty()) productDao.insertProducts(payload.products)
            if (payload.inventoryMovements.isNotEmpty()) movementDao.insertMovements(payload.inventoryMovements)
            if (payload.generatorExpenses.isNotEmpty()) generatorExpenseDao.insertExpenses(payload.generatorExpenses)
            if (payload.ampSubscribers.isNotEmpty()) ampDao.insertSubscribers(payload.ampSubscribers)
            if (payload.customerReceipts.isNotEmpty()) receiptDao.insertReceipts(payload.customerReceipts)
            if (payload.invoices.isNotEmpty()) invoiceDao.insertInvoices(payload.invoices)
            if (payload.invoiceItems.isNotEmpty()) invoiceDao.insertInvoiceItems(payload.invoiceItems)
            if (payload.systemUsers.isNotEmpty()) userDao.insertUsers(payload.systemUsers)
        }
    }
}
