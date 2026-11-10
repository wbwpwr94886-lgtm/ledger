package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AccountingRepository
import com.example.util.AppCurrency
import com.example.util.AppLanguage
import com.example.util.DashboardSearchFilter
import com.example.util.DashboardSearchHelper
import com.example.util.DashboardSearchResults
import com.example.util.JsonBackupManager
import com.example.util.ParsedBackupPayload
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var customPrice: Double = product.sellPrice
) {
    val totalPrice: Double get() = quantity * customPrice
    val totalCost: Double get() = quantity * product.buyPrice
    val profit: Double get() = totalPrice - totalCost
}

enum class NavigationScreen(val titleAr: String, val titleEn: String) {
    DASHBOARD("لوحة التحكم", "Dashboard"),
    POS_SALE("نقطة البيع", "POS & Sales"),
    CUSTOMERS("العملاء والديون", "Customers & Debt"),
    PRODUCTS("المخزون والباركود", "Inventory & Barcode"),
    AMPERES("بيع الأمبيرات", "Amperes & Generators"),
    SUPPLIERS("إدارة الموردين", "Suppliers"),
    ACCOUNTS("الحسابات والصناديق", "Accounts & Treasuries"),
    REPORTS("التقارير المالية", "Financial Reports"),
    BACKUP("النسخ الاحتياطي والاستعادة", "Backup & Restore")
}

data class FinancialMetrics(
    val totalSales: Double = 0.0,
    val totalProfits: Double = 0.0,
    val totalCustomerDebt: Double = 0.0,
    val totalSupplierDebt: Double = 0.0,
    val totalTreasuryBalance: Double = 0.0,
    val totalTaxes: Double = 0.0,
    val lowStockCount: Int = 0,
    val totalAmperesSold: Double = 0.0,
    val totalAmpRevenue: Double = 0.0,
    val averageAmpPrice: Double = 15000.0,
    val totalGeneratorExpenses: Double = 0.0,
    val dieselExpenses: Double = 0.0,
    val oilExpenses: Double = 0.0,
    val maintenanceExpenses: Double = 0.0,
    val generatorNetProfit: Double = 0.0,
    val ampDeliveredCount: Int = 0,
    val ampUndeliveredCount: Int = 0
)

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AccountingRepository(database)

    // Language & Currency
    val currentLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val currentCurrency = MutableStateFlow(AppCurrency.IQD)
    val exchangeRate = MutableStateFlow(1500.0) // 1 USD = 1500 IQD

    // Navigation
    val currentScreen = MutableStateFlow(NavigationScreen.DASHBOARD)

    // Current logged in user
    val activeUser = MutableStateFlow(
        SystemUser(
            id = 1,
            username = "admin",
            displayName = "أحمد فرحان السلماني (المدير)",
            role = "ADMIN",
            canViewReports = true,
            canEditProducts = true,
            canManageDebts = true,
            canExportBackup = true,
            canManageUsers = true
        )
    )

    // Data streams
    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accountTransactions = repository.allAccountTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = repository.allDebts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customerDebts = repository.customerDebts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supplierDebts = repository.supplierDebts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.allSuppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventoryMovements = repository.allInventoryMovements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoices = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val receipts = repository.allReceipts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ampSubscribers = repository.allAmpSubscribers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val generatorExpenses = repository.allGeneratorExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val systemUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications = repository.allNotifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search queries
    val searchQuery = MutableStateFlow("")
    val dashboardSearchQuery = MutableStateFlow("")
    val dashboardSearchFilter = MutableStateFlow(DashboardSearchFilter.ALL)

    fun setDashboardSearchQuery(query: String) {
        dashboardSearchQuery.value = query
    }

    fun setDashboardSearchFilter(filter: DashboardSearchFilter) {
        dashboardSearchFilter.value = filter
    }

    fun clearDashboardSearch() {
        dashboardSearchQuery.value = ""
        dashboardSearchFilter.value = DashboardSearchFilter.ALL
    }

    // POS Cart
    val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val selectedCustomer = MutableStateFlow<Customer?>(null)
    val discountAmount = MutableStateFlow(0.0)
    val taxRatePercent = MutableStateFlow(0.0) // e.g. 0% or 5%
    val paidAmountInput = MutableStateFlow("")
    val isDeliveredCheck = MutableStateFlow(true)
    val attachedReceiptPhotoUri = MutableStateFlow<String?>(null)

    // Computed Financial Metrics
    val financialMetrics: StateFlow<FinancialMetrics> = combine(
        invoices, customers, suppliers, lowStockProducts, ampSubscribers, generatorExpenses, accounts
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val invs = args[0] as List<Invoice>
        @Suppress("UNCHECKED_CAST")
        val custs = args[1] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val supps = args[2] as List<Supplier>
        @Suppress("UNCHECKED_CAST")
        val lowStock = args[3] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val amps = args[4] as List<AmpSubscriber>
        @Suppress("UNCHECKED_CAST")
        val expenses = args[5] as List<GeneratorExpense>
        @Suppress("UNCHECKED_CAST")
        val accs = args[6] as List<Account>

        val totalSales = invs.sumOf { it.totalAmount }
        val totalProfits = invs.sumOf { inv ->
            inv.totalAmount * 0.25
        }
        val totalCustomerDebt = custs.sumOf { it.currentDebt }
        val totalSupplierDebt = supps.sumOf { it.balanceOwed }
        val totalTreasuryBalance = accs.sumOf { it.balance }
        val totalTaxes = invs.sumOf { it.taxAmount }
        val totalAmperesSold = amps.sumOf { it.amperes }
        val totalAmpRevenue = amps.sumOf { it.monthlyTotal }
        val averageAmpPrice = if (totalAmperesSold > 0) totalAmpRevenue / totalAmperesSold else 15000.0
        val totalGeneratorExpenses = expenses.sumOf { it.amount }
        val dieselExpenses = expenses.filter { it.type == "DIESEL" }.sumOf { it.amount }
        val oilExpenses = expenses.filter { it.type == "OIL" }.sumOf { it.amount }
        val maintenanceExpenses = expenses.filter { it.type != "DIESEL" && it.type != "OIL" }.sumOf { it.amount }
        val generatorNetProfit = totalAmpRevenue - totalGeneratorExpenses
        val ampDeliveredCount = amps.count { it.isBillDelivered }
        val ampUndeliveredCount = amps.count { !it.isBillDelivered }

        FinancialMetrics(
            totalSales = totalSales,
            totalProfits = totalProfits,
            totalCustomerDebt = totalCustomerDebt,
            totalSupplierDebt = totalSupplierDebt,
            totalTreasuryBalance = totalTreasuryBalance,
            totalTaxes = totalTaxes,
            lowStockCount = lowStock.size,
            totalAmperesSold = totalAmperesSold,
            totalAmpRevenue = totalAmpRevenue,
            averageAmpPrice = averageAmpPrice,
            totalGeneratorExpenses = totalGeneratorExpenses,
            dieselExpenses = dieselExpenses,
            oilExpenses = oilExpenses,
            maintenanceExpenses = maintenanceExpenses,
            generatorNetProfit = generatorNetProfit,
            ampDeliveredCount = ampDeliveredCount,
            ampUndeliveredCount = ampUndeliveredCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialMetrics())

    // Language Toggle
    fun toggleLanguage() {
        currentLanguage.value = if (currentLanguage.value == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
    }

    // Currency Toggle
    fun toggleCurrency() {
        currentCurrency.value = if (currentCurrency.value == AppCurrency.IQD) AppCurrency.USD else AppCurrency.IQD
    }

    fun setScreen(screen: NavigationScreen) {
        currentScreen.value = screen
    }

    // Switch User
    fun switchUser(user: SystemUser) {
        activeUser.value = user
    }

    // POS Cart Operations
    fun addToCart(product: Product) {
        val current = cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + 1.0)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, delta: Double) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            cartItems.value = current
        }
    }

    fun removeCartItem(productId: Long) {
        cartItems.value = cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        cartItems.value = emptyList()
        selectedCustomer.value = null
        discountAmount.value = 0.0
        taxRatePercent.value = 0.0
        paidAmountInput.value = ""
        attachedReceiptPhotoUri.value = null
    }

    fun scanBarcode(barcode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            if (product != null) {
                addToCart(product)
                onResult(true, product.name)
            } else {
                onResult(false, "المنتج غير موجود: $barcode")
            }
        }
    }

    fun completeInvoice(
        onComplete: (Invoice, List<InvoiceItem>) -> Unit
    ) {
        val items = cartItems.value
        if (items.isEmpty()) return

        val subtotal = items.sumOf { it.totalPrice } - discountAmount.value
        val effectiveSubtotal = maxOf(0.0, subtotal)
        val taxAmount = effectiveSubtotal * (taxRatePercent.value / 100.0)
        val totalAmount = effectiveSubtotal + taxAmount

        val enteredPaid = paidAmountInput.value.toDoubleOrNull() ?: totalAmount
        val paidAmount = minOf(totalAmount, maxOf(0.0, enteredPaid))
        val remainingDebt = maxOf(0.0, totalAmount - paidAmount)

        val customer = selectedCustomer.value
        val invNumber = (1000 + (invoices.value.size + 1)).toString()

        val invoice = Invoice(
            invoiceNumber = invNumber,
            type = "SALE",
            customerId = customer?.id,
            customerName = customer?.name ?: "عميل نقدي",
            customerPhone = customer?.phone ?: "",
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            remainingDebt = remainingDebt,
            taxRate = taxRatePercent.value,
            taxAmount = taxAmount,
            currency = currentCurrency.value.code,
            exchangeRate = exchangeRate.value,
            isDelivered = isDeliveredCheck.value,
            photoUri = attachedReceiptPhotoUri.value,
            notes = if (remainingDebt > 0) "فاتورة بيع آجل (دين متبقي)" else "فاتورة مدفوعة بالكامل",
            createdBy = activeUser.value.displayName
        )

        val invoiceItems = items.map { item ->
            InvoiceItem(
                invoiceId = 0, // will be set by repository
                productId = item.product.id,
                productName = item.product.name,
                barcode = item.product.barcode,
                quantity = item.quantity,
                unitPrice = item.customPrice,
                buyPrice = item.product.buyPrice,
                totalPrice = item.totalPrice
            )
        }

        viewModelScope.launch {
            repository.createInvoice(invoice, invoiceItems)
            clearCart()
            onComplete(invoice, invoiceItems)
        }
    }

    // Customer operations
    fun addCustomer(customer: Customer) {
        viewModelScope.launch { repository.insertCustomer(customer) }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch { repository.updateCustomer(customer) }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repository.deleteCustomer(customer) }
    }

    fun addReceiptPayment(customerId: Long, customerName: String, amount: Double, notes: String, photoUri: String?) {
        viewModelScope.launch {
            val receipt = CustomerReceipt(
                customerId = customerId,
                customerName = customerName,
                amount = amount,
                currency = currentCurrency.value.code,
                receiptNumber = "REC-${System.currentTimeMillis().toString().takeLast(6)}",
                photoUri = photoUri,
                notes = notes
            )
            repository.addReceipt(receipt)
        }
    }

    // Product operations
    fun addProduct(product: Product) {
        viewModelScope.launch { repository.insertProduct(product) }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch { repository.updateProduct(product) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    // Supplier operations
    fun addSupplier(supplier: Supplier) {
        viewModelScope.launch { repository.insertSupplier(supplier) }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch { repository.updateSupplier(supplier) }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch { repository.deleteSupplier(supplier) }
    }

    // Amp Subscriber operations (بيع واشتراك الأمبيرات)
    fun addAmpSubscriber(subscriber: AmpSubscriber) {
        viewModelScope.launch { repository.insertAmpSubscriber(subscriber) }
    }

    fun updateAmpSubscriber(subscriber: AmpSubscriber) {
        viewModelScope.launch { repository.updateAmpSubscriber(subscriber) }
    }

    fun deleteAmpSubscriber(subscriber: AmpSubscriber) {
        viewModelScope.launch { repository.deleteAmpSubscriber(subscriber) }
    }

    fun toggleAmpDeliveryStatus(subscriberId: Long, currentStatus: Boolean) {
        viewModelScope.launch { repository.toggleAmpDelivery(subscriberId, currentStatus) }
    }

    fun recordAmpPayment(subscriberId: Long, amount: Double) {
        viewModelScope.launch { repository.recordAmpPayment(subscriberId, amount) }
    }

    // User operations
    fun addUser(user: SystemUser) {
        viewModelScope.launch { repository.insertUser(user) }
    }

    fun updateUser(user: SystemUser) {
        viewModelScope.launch { repository.updateUser(user) }
    }

    fun deleteUser(user: SystemUser) {
        viewModelScope.launch { repository.deleteUser(user) }
    }

    // Notification operations
    fun addNotification(notification: ScheduledNotification) {
        viewModelScope.launch { repository.insertNotification(notification) }
    }

    fun deleteNotification(notification: ScheduledNotification) {
        viewModelScope.launch { repository.deleteNotification(notification) }
    }

    // Generator expense operations (صرفيات المولد)
    fun addGeneratorExpense(
        type: String,
        title: String,
        amount: Double,
        quantity: Double,
        unit: String,
        notes: String
    ) {
        viewModelScope.launch {
            val expense = GeneratorExpense(
                type = type,
                title = title.ifBlank {
                    when (type) {
                        "DIESEL" -> "تعبئة كاز (وقود)"
                        "OIL" -> "تبديل زيت المحرك"
                        "MAINTENANCE" -> "صيانة دورية وفلاتر"
                        else -> "مصروفات مولد أخرى"
                    }
                },
                amount = amount,
                quantity = quantity,
                unit = unit,
                notes = notes
            )
            repository.insertGeneratorExpense(expense)
        }
    }

    fun deleteGeneratorExpense(expense: GeneratorExpense) {
        viewModelScope.launch { repository.deleteGeneratorExpense(expense) }
    }

    // Account operations (الصناديق والحسابات المالية)
    fun addAccount(
        name: String,
        code: String,
        type: String,
        balance: Double,
        currency: String = "IQD",
        isDefault: Boolean = false,
        description: String = ""
    ) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                code = code,
                type = type,
                balance = balance,
                currency = currency,
                isDefault = isDefault,
                description = description
            )
            val id = repository.insertAccount(account)
            if (balance > 0) {
                repository.depositToAccount(id, balance, "رصيد افتتاحي", "إيداع رصيد افتتاحي للحساب")
            }
        }
    }

    fun depositToAccount(accountId: Long, amount: Double, notes: String, category: String = "إيداع نقدي") {
        viewModelScope.launch {
            repository.depositToAccount(accountId, amount, category, notes)
        }
    }

    fun withdrawFromAccount(accountId: Long, amount: Double, notes: String, category: String = "سحب نقدي") {
        viewModelScope.launch {
            repository.withdrawFromAccount(accountId, amount, category, notes)
        }
    }

    fun transferBetweenAccounts(fromId: Long, toId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.transferBetweenAccounts(fromId, toId, amount, notes)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // Debt operations (سجل الديون والذمم)
    fun addDebt(
        debtorName: String,
        debtorPhone: String,
        debtType: String, // RECEIVABLE or PAYABLE
        amount: Double,
        dueDateMillis: Long?,
        notes: String
    ) {
        viewModelScope.launch {
            val debt = DebtRecord(
                debtorName = debtorName,
                debtorPhone = debtorPhone,
                debtType = debtType,
                initialAmount = amount,
                paidAmount = 0.0,
                remainingAmount = amount,
                currency = currentCurrency.value.code,
                dueDateMillis = dueDateMillis,
                status = "ACTIVE",
                notes = notes
            )
            repository.insertDebt(debt)
        }
    }

    fun recordDebtPayment(debtId: Long, amount: Double, notes: String = "") {
        viewModelScope.launch {
            repository.recordDebtPayment(debtId, amount, notes)
        }
    }

    fun deleteDebt(debt: DebtRecord) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    // Inventory movement operations (حركة المواد)
    fun recordStockMovement(
        productId: Long,
        productName: String,
        type: String, // IN_PURCHASE, OUT_SALE, ADJUSTMENT, RETURN
        quantity: Double,
        unitPrice: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val movement = InventoryMovement(
                productId = productId,
                productName = productName,
                movementType = type,
                quantity = quantity,
                unitPrice = unitPrice,
                notes = notes
            )
            repository.recordStockMovement(movement)
        }
    }

    // ==========================================
    // النسخ الاحتياطي والاستعادة المحلية (Local JSON Backup & Restore)
    // ==========================================
    val isExportingBackup = MutableStateFlow(false)
    val isRestoringBackup = MutableStateFlow(false)
    val localBackupsList = MutableStateFlow<List<File>>(emptyList())

    fun refreshLocalBackups(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = JsonBackupManager.listLocalBackups(context)
            localBackupsList.value = list
        }
    }

    fun exportDatabaseToJsonFile(
        context: Context,
        onComplete: (success: Boolean, file: File?, message: String) -> Unit
    ) {
        viewModelScope.launch {
            isExportingBackup.value = true
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    repository.exportAllDataToJson()
                }
                val savedFile = withContext(Dispatchers.IO) {
                    JsonBackupManager.saveBackupToFile(context, jsonString)
                }
                refreshLocalBackups(context)
                isExportingBackup.value = false
                val msg = "تم حفظ النسخة الاحتياطية بنجاح:\n${savedFile.name}"
                onComplete(true, savedFile, msg)
            } catch (e: Exception) {
                isExportingBackup.value = false
                onComplete(false, null, "فشل إنشاء النسخة الاحتياطية: ${e.localizedMessage}")
            }
        }
    }

    fun parseBackupFromUri(
        context: Context,
        uri: Uri,
        onParsed: (ParsedBackupPayload) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonText = JsonBackupManager.readTextFromUri(context, uri)
                val payload = JsonBackupManager.parseBackupJson(jsonText)
                withContext(Dispatchers.Main) {
                    onParsed(payload)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("الملف المحدد غير صالح أو تالف: ${e.localizedMessage}")
                }
            }
        }
    }

    fun parseBackupFromFile(
        file: File,
        onParsed: (ParsedBackupPayload) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonText = file.readText()
                val payload = JsonBackupManager.parseBackupJson(jsonText)
                withContext(Dispatchers.Main) {
                    onParsed(payload)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("الملف غير صالح أو تالف: ${e.localizedMessage}")
                }
            }
        }
    }

    fun restoreBackupPayload(
        payload: ParsedBackupPayload,
        context: Context,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            isRestoringBackup.value = true
            try {
                withContext(Dispatchers.IO) {
                    repository.restoreDataFromJson(payload, wipeBeforeRestore = true)
                }
                refreshLocalBackups(context)
                isRestoringBackup.value = false
                val countSummary = "تمت استعادة ${payload.accounts.size} حساب، ${payload.debts.size} دين، ${payload.invoices.size} فاتورة، ${payload.products.size} مادة، ${payload.suppliers.size} مورد بنجاح!"
                onComplete(true, countSummary)
            } catch (e: Exception) {
                isRestoringBackup.value = false
                onComplete(false, "حدث خطأ أثناء استعادة البيانات: ${e.localizedMessage}")
            }
        }
    }

    fun deleteLocalBackup(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
            }
            refreshLocalBackups(context)
        }
    }
}
