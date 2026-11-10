package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class BackupSummary(
    val exportDateFormatted: String,
    val timestamp: Long,
    val accountsCount: Int,
    val debtsCount: Int,
    val suppliersCount: Int,
    val productsCount: Int,
    val inventoryMovementsCount: Int,
    val invoicesCount: Int,
    val generatorExpensesCount: Int,
    val customersCount: Int,
    val receiptsCount: Int,
    val ampSubscribersCount: Int,
    val totalRevenue: Double,
    val totalCustomerDebt: Double,
    val appVersion: String
)

data class ParsedBackupPayload(
    val summary: BackupSummary,
    val accounts: List<Account>,
    val accountTransactions: List<AccountTransaction>,
    val debts: List<DebtRecord>,
    val suppliers: List<Supplier>,
    val products: List<Product>,
    val inventoryMovements: List<InventoryMovement>,
    val invoices: List<Invoice>,
    val invoiceItems: List<InvoiceItem>,
    val generatorExpenses: List<GeneratorExpense>,
    val customers: List<Customer>,
    val customerReceipts: List<CustomerReceipt>,
    val ampSubscribers: List<AmpSubscriber>,
    val systemUsers: List<SystemUser>
)

object JsonBackupManager {

    private const val BACKUP_VERSION = 1
    private const val APP_IDENTIFIER = "com.aistudio.accounting.ledger"

    /**
     * Serializes all Room database tables into a structured JSON string
     */
    fun createBackupJson(
        accounts: List<Account>,
        accountTransactions: List<AccountTransaction>,
        debts: List<DebtRecord>,
        suppliers: List<Supplier>,
        products: List<Product>,
        inventoryMovements: List<InventoryMovement>,
        invoices: List<Invoice>,
        invoiceItems: List<InvoiceItem>,
        generatorExpenses: List<GeneratorExpense>,
        customers: List<Customer>,
        customerReceipts: List<CustomerReceipt>,
        ampSubscribers: List<AmpSubscriber>,
        systemUsers: List<SystemUser>
    ): String {
        val root = JSONObject()
        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        // 1. Metadata
        val meta = JSONObject().apply {
            put("app", "Ledger Accounting System")
            put("package", APP_IDENTIFIER)
            put("backupVersion", BACKUP_VERSION)
            put("timestamp", timestamp)
            put("formattedDate", sdf.format(Date(timestamp)))
            put("developer", "Ahmed Farhan Al-Salmani")
        }
        root.put("metadata", meta)

        // 2. Summary stats
        val summary = JSONObject().apply {
            put("accountsCount", accounts.size)
            put("debtsCount", debts.size)
            put("suppliersCount", suppliers.size)
            put("productsCount", products.size)
            put("invoicesCount", invoices.size)
            put("generatorExpensesCount", generatorExpenses.size)
            put("customersCount", customers.size)
            put("totalSales", invoices.sumOf { it.totalAmount })
            put("totalCustomerDebt", customers.sumOf { it.currentDebt })
        }
        root.put("summary", summary)

        // 3. Accounts
        val accArray = JSONArray()
        for (a in accounts) {
            accArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("code", a.code)
                put("type", a.type)
                put("balance", a.balance)
                put("currency", a.currency)
                put("description", a.description)
                put("isDefault", a.isDefault)
                put("createdAt", a.createdAt)
            })
        }
        root.put("accounts", accArray)

        // 4. Account Transactions
        val txArray = JSONArray()
        for (t in accountTransactions) {
            txArray.put(JSONObject().apply {
                put("id", t.id)
                put("accountId", t.accountId)
                put("accountName", t.accountName)
                put("type", t.type)
                put("amount", t.amount)
                put("balanceAfter", t.balanceAfter)
                put("category", t.category)
                put("referenceType", t.referenceType)
                put("referenceId", t.referenceId ?: JSONObject.NULL)
                put("notes", t.notes)
                put("createdAt", t.createdAt)
            })
        }
        root.put("accountTransactions", txArray)

        // 5. Debts
        val debtArray = JSONArray()
        for (d in debts) {
            debtArray.put(JSONObject().apply {
                put("id", d.id)
                put("debtorName", d.debtorName)
                put("debtorPhone", d.debtorPhone)
                put("debtType", d.debtType)
                put("relatedCustomerId", d.relatedCustomerId ?: JSONObject.NULL)
                put("relatedSupplierId", d.relatedSupplierId ?: JSONObject.NULL)
                put("initialAmount", d.initialAmount)
                put("paidAmount", d.paidAmount)
                put("remainingAmount", d.remainingAmount)
                put("currency", d.currency)
                put("dueDateMillis", d.dueDateMillis ?: JSONObject.NULL)
                put("status", d.status)
                put("notes", d.notes)
                put("createdAt", d.createdAt)
                put("updatedAt", d.updatedAt)
            })
        }
        root.put("debts", debtArray)

        // 6. Suppliers
        val supArray = JSONArray()
        for (s in suppliers) {
            supArray.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("phone", s.phone)
                put("company", s.company)
                put("balanceOwed", s.balanceOwed)
                put("address", s.address)
                put("notes", s.notes)
                put("createdAt", s.createdAt)
            })
        }
        root.put("suppliers", supArray)

        // 7. Products
        val prodArray = JSONArray()
        for (p in products) {
            prodArray.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("barcode", p.barcode)
                put("buyPrice", p.buyPrice)
                put("sellPrice", p.sellPrice)
                put("currency", p.currency)
                put("stockQuantity", p.stockQuantity)
                put("minStockAlert", p.minStockAlert)
                put("category", p.category)
                put("unit", p.unit)
                put("location", p.location)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("products", prodArray)

        // 8. Inventory Movements
        val movArray = JSONArray()
        for (m in inventoryMovements) {
            movArray.put(JSONObject().apply {
                put("id", m.id)
                put("productId", m.productId)
                put("productName", m.productName)
                put("movementType", m.movementType)
                put("quantity", m.quantity)
                put("unitPrice", m.unitPrice)
                put("referenceId", m.referenceId ?: JSONObject.NULL)
                put("notes", m.notes)
                put("createdAt", m.createdAt)
            })
        }
        root.put("inventoryMovements", movArray)

        // 9. Invoices
        val invArray = JSONArray()
        for (inv in invoices) {
            invArray.put(JSONObject().apply {
                put("id", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("type", inv.type)
                put("customerId", inv.customerId ?: JSONObject.NULL)
                put("customerName", inv.customerName)
                put("customerPhone", inv.customerPhone)
                put("supplierId", inv.supplierId ?: JSONObject.NULL)
                put("supplierName", inv.supplierName)
                put("accountId", inv.accountId ?: JSONObject.NULL)
                put("totalAmount", inv.totalAmount)
                put("discountAmount", inv.discountAmount)
                put("paidAmount", inv.paidAmount)
                put("remainingDebt", inv.remainingDebt)
                put("taxRate", inv.taxRate)
                put("taxAmount", inv.taxAmount)
                put("currency", inv.currency)
                put("exchangeRate", inv.exchangeRate)
                put("paymentMethod", inv.paymentMethod)
                put("isDelivered", inv.isDelivered)
                put("photoUri", inv.photoUri ?: JSONObject.NULL)
                put("notes", inv.notes)
                put("createdBy", inv.createdBy)
                put("createdAt", inv.createdAt)
                put("dueDateMillis", inv.dueDateMillis ?: JSONObject.NULL)
            })
        }
        root.put("invoices", invArray)

        // 10. Invoice Items
        val itemArray = JSONArray()
        for (item in invoiceItems) {
            itemArray.put(JSONObject().apply {
                put("id", item.id)
                put("invoiceId", item.invoiceId)
                put("productId", item.productId ?: JSONObject.NULL)
                put("productName", item.productName)
                put("barcode", item.barcode)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("buyPrice", item.buyPrice)
                put("totalPrice", item.totalPrice)
            })
        }
        root.put("invoiceItems", itemArray)

        // 11. Generator Expenses
        val expArray = JSONArray()
        for (e in generatorExpenses) {
            expArray.put(JSONObject().apply {
                put("id", e.id)
                put("type", e.type)
                put("title", e.title)
                put("amount", e.amount)
                put("quantity", e.quantity)
                put("unit", e.unit)
                put("paidFromAccountId", e.paidFromAccountId ?: JSONObject.NULL)
                put("notes", e.notes)
                put("createdAt", e.createdAt)
            })
        }
        root.put("generatorExpenses", expArray)

        // 12. Customers
        val custArray = JSONArray()
        for (c in customers) {
            custArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("currentDebt", c.currentDebt)
                put("notes", c.notes)
                put("dueDateMillis", c.dueDateMillis ?: JSONObject.NULL)
                put("createdAt", c.createdAt)
            })
        }
        root.put("customers", custArray)

        // 13. Customer Receipts
        val recArray = JSONArray()
        for (r in customerReceipts) {
            recArray.put(JSONObject().apply {
                put("id", r.id)
                put("customerId", r.customerId)
                put("customerName", r.customerName)
                put("amount", r.amount)
                put("currency", r.currency)
                put("paymentMethod", r.paymentMethod)
                put("receiptNumber", r.receiptNumber)
                put("photoUri", r.photoUri ?: JSONObject.NULL)
                put("notes", r.notes)
                put("createdAt", r.createdAt)
            })
        }
        root.put("customerReceipts", recArray)

        // 14. Amp Subscribers
        val ampArray = JSONArray()
        for (a in ampSubscribers) {
            ampArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("phone", a.phone)
                put("amperes", a.amperes)
                put("pricePerAmpere", a.pricePerAmpere)
                put("monthlyTotal", a.monthlyTotal)
                put("debtAmount", a.debtAmount)
                put("isBillDelivered", a.isBillDelivered)
                put("meterNumberOrLocation", a.meterNumberOrLocation)
                put("notes", a.notes)
                put("lastDeliveryDate", a.lastDeliveryDate ?: JSONObject.NULL)
                put("createdAt", a.createdAt)
            })
        }
        root.put("ampSubscribers", ampArray)

        // 15. System Users
        val userArray = JSONArray()
        for (u in systemUsers) {
            userArray.put(JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("displayName", u.displayName)
                put("role", u.role)
                put("canViewReports", u.canViewReports)
                put("canEditProducts", u.canEditProducts)
                put("canManageDebts", u.canManageDebts)
                put("canExportBackup", u.canExportBackup)
                put("canManageUsers", u.canManageUsers)
                put("isActive", u.isActive)
            })
        }
        root.put("systemUsers", userArray)

        return root.toString(2)
    }

    /**
     * Parses and validates a JSON string into entity lists
     */
    fun parseBackupJson(jsonString: String): ParsedBackupPayload {
        val root = JSONObject(jsonString)
        val meta = root.optJSONObject("metadata")
        val timestamp = meta?.optLong("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
        val formattedDate = meta?.optString("formattedDate") ?: SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        val appVer = meta?.optString("app", "Ledger 1.0") ?: "1.0"

        // Accounts
        val accounts = mutableListOf<Account>()
        val accArray = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArray.length()) {
            val obj = accArray.getJSONObject(i)
            accounts.add(
                Account(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "حساب"),
                    code = obj.optString("code", ""),
                    type = obj.optString("type", "CASH"),
                    balance = obj.optDouble("balance", 0.0),
                    currency = obj.optString("currency", "IQD"),
                    description = obj.optString("description", ""),
                    isDefault = obj.optBoolean("isDefault", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Account Transactions
        val accountTransactions = mutableListOf<AccountTransaction>()
        val txArray = root.optJSONArray("accountTransactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            accountTransactions.add(
                AccountTransaction(
                    id = obj.optLong("id", 0),
                    accountId = obj.optLong("accountId", 0),
                    accountName = obj.optString("accountName", ""),
                    type = obj.optString("type", "DEPOSIT"),
                    amount = obj.optDouble("amount", 0.0),
                    balanceAfter = obj.optDouble("balanceAfter", 0.0),
                    category = obj.optString("category", "عام"),
                    referenceType = obj.optString("referenceType", "MANUAL"),
                    referenceId = if (obj.isNull("referenceId")) null else obj.optLong("referenceId"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Debts
        val debts = mutableListOf<DebtRecord>()
        val debtArray = root.optJSONArray("debts") ?: JSONArray()
        for (i in 0 until debtArray.length()) {
            val obj = debtArray.getJSONObject(i)
            debts.add(
                DebtRecord(
                    id = obj.optLong("id", 0),
                    debtorName = obj.optString("debtorName", "مدين"),
                    debtorPhone = obj.optString("debtorPhone", ""),
                    debtType = obj.optString("debtType", "RECEIVABLE"),
                    relatedCustomerId = if (obj.isNull("relatedCustomerId")) null else obj.optLong("relatedCustomerId"),
                    relatedSupplierId = if (obj.isNull("relatedSupplierId")) null else obj.optLong("relatedSupplierId"),
                    initialAmount = obj.optDouble("initialAmount", 0.0),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    remainingAmount = obj.optDouble("remainingAmount", 0.0),
                    currency = obj.optString("currency", "IQD"),
                    dueDateMillis = if (obj.isNull("dueDateMillis")) null else obj.optLong("dueDateMillis"),
                    status = obj.optString("status", "ACTIVE"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Suppliers
        val suppliers = mutableListOf<Supplier>()
        val supArray = root.optJSONArray("suppliers") ?: JSONArray()
        for (i in 0 until supArray.length()) {
            val obj = supArray.getJSONObject(i)
            suppliers.add(
                Supplier(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "مورد"),
                    phone = obj.optString("phone", ""),
                    company = obj.optString("company", ""),
                    balanceOwed = obj.optDouble("balanceOwed", 0.0),
                    address = obj.optString("address", ""),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Products
        val products = mutableListOf<Product>()
        val prodArray = root.optJSONArray("products") ?: JSONArray()
        for (i in 0 until prodArray.length()) {
            val obj = prodArray.getJSONObject(i)
            products.add(
                Product(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "منتج"),
                    barcode = obj.optString("barcode", ""),
                    buyPrice = obj.optDouble("buyPrice", 0.0),
                    sellPrice = obj.optDouble("sellPrice", 0.0),
                    currency = obj.optString("currency", "IQD"),
                    stockQuantity = obj.optDouble("stockQuantity", 0.0),
                    minStockAlert = obj.optDouble("minStockAlert", 5.0),
                    category = obj.optString("category", "عام"),
                    unit = obj.optString("unit", "قطعة"),
                    location = obj.optString("location", ""),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Inventory Movements
        val movements = mutableListOf<InventoryMovement>()
        val movArray = root.optJSONArray("inventoryMovements") ?: JSONArray()
        for (i in 0 until movArray.length()) {
            val obj = movArray.getJSONObject(i)
            movements.add(
                InventoryMovement(
                    id = obj.optLong("id", 0),
                    productId = obj.optLong("productId", 0),
                    productName = obj.optString("productName", ""),
                    movementType = obj.optString("movementType", "OUT_SALE"),
                    quantity = obj.optDouble("quantity", 0.0),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    referenceId = if (obj.isNull("referenceId")) null else obj.optLong("referenceId"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Invoices
        val invoices = mutableListOf<Invoice>()
        val invArray = root.optJSONArray("invoices") ?: JSONArray()
        for (i in 0 until invArray.length()) {
            val obj = invArray.getJSONObject(i)
            invoices.add(
                Invoice(
                    id = obj.optLong("id", 0),
                    invoiceNumber = obj.optString("invoiceNumber", "1000"),
                    type = obj.optString("type", "SALE"),
                    customerId = if (obj.isNull("customerId")) null else obj.optLong("customerId"),
                    customerName = obj.optString("customerName", ""),
                    customerPhone = obj.optString("customerPhone", ""),
                    supplierId = if (obj.isNull("supplierId")) null else obj.optLong("supplierId"),
                    supplierName = obj.optString("supplierName", ""),
                    accountId = if (obj.isNull("accountId")) null else obj.optLong("accountId"),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    discountAmount = obj.optDouble("discountAmount", 0.0),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    remainingDebt = obj.optDouble("remainingDebt", 0.0),
                    taxRate = obj.optDouble("taxRate", 0.0),
                    taxAmount = obj.optDouble("taxAmount", 0.0),
                    currency = obj.optString("currency", "IQD"),
                    exchangeRate = obj.optDouble("exchangeRate", 1.0),
                    paymentMethod = obj.optString("paymentMethod", "CASH"),
                    isDelivered = obj.optBoolean("isDelivered", false),
                    photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri"),
                    notes = obj.optString("notes", ""),
                    createdBy = obj.optString("createdBy", "المدير"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    dueDateMillis = if (obj.isNull("dueDateMillis")) null else obj.optLong("dueDateMillis")
                )
            )
        }

        // Invoice Items
        val invoiceItems = mutableListOf<InvoiceItem>()
        val itemArray = root.optJSONArray("invoiceItems") ?: JSONArray()
        for (i in 0 until itemArray.length()) {
            val obj = itemArray.getJSONObject(i)
            invoiceItems.add(
                InvoiceItem(
                    id = obj.optLong("id", 0),
                    invoiceId = obj.optLong("invoiceId", 0),
                    productId = if (obj.isNull("productId")) null else obj.optLong("productId"),
                    productName = obj.optString("productName", ""),
                    barcode = obj.optString("barcode", ""),
                    quantity = obj.optDouble("quantity", 1.0),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    buyPrice = obj.optDouble("buyPrice", 0.0),
                    totalPrice = obj.optDouble("totalPrice", 0.0)
                )
            )
        }

        // Generator Expenses
        val generatorExpenses = mutableListOf<GeneratorExpense>()
        val expArray = root.optJSONArray("generatorExpenses") ?: JSONArray()
        for (i in 0 until expArray.length()) {
            val obj = expArray.getJSONObject(i)
            generatorExpenses.add(
                GeneratorExpense(
                    id = obj.optLong("id", 0),
                    type = obj.optString("type", "DIESEL"),
                    title = obj.optString("title", ""),
                    amount = obj.optDouble("amount", 0.0),
                    quantity = obj.optDouble("quantity", 0.0),
                    unit = obj.optString("unit", "لتر"),
                    paidFromAccountId = if (obj.isNull("paidFromAccountId")) null else obj.optLong("paidFromAccountId"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Customers
        val customers = mutableListOf<Customer>()
        val custArray = root.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until custArray.length()) {
            val obj = custArray.getJSONObject(i)
            customers.add(
                Customer(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "عميل"),
                    phone = obj.optString("phone", ""),
                    address = obj.optString("address", ""),
                    currentDebt = obj.optDouble("currentDebt", 0.0),
                    notes = obj.optString("notes", ""),
                    dueDateMillis = if (obj.isNull("dueDateMillis")) null else obj.optLong("dueDateMillis"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Customer Receipts
        val receipts = mutableListOf<CustomerReceipt>()
        val recArray = root.optJSONArray("customerReceipts") ?: JSONArray()
        for (i in 0 until recArray.length()) {
            val obj = recArray.getJSONObject(i)
            receipts.add(
                CustomerReceipt(
                    id = obj.optLong("id", 0),
                    customerId = obj.optLong("customerId", 0),
                    customerName = obj.optString("customerName", ""),
                    amount = obj.optDouble("amount", 0.0),
                    currency = obj.optString("currency", "IQD"),
                    paymentMethod = obj.optString("paymentMethod", "نقدي"),
                    receiptNumber = obj.optString("receiptNumber", ""),
                    photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Amp Subscribers
        val subscribers = mutableListOf<AmpSubscriber>()
        val ampArray = root.optJSONArray("ampSubscribers") ?: JSONArray()
        for (i in 0 until ampArray.length()) {
            val obj = ampArray.getJSONObject(i)
            subscribers.add(
                AmpSubscriber(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "مشترك"),
                    phone = obj.optString("phone", ""),
                    amperes = obj.optDouble("amperes", 5.0),
                    pricePerAmpere = obj.optDouble("pricePerAmpere", 15000.0),
                    monthlyTotal = obj.optDouble("monthlyTotal", 75000.0),
                    debtAmount = obj.optDouble("debtAmount", 0.0),
                    isBillDelivered = obj.optBoolean("isBillDelivered", false),
                    meterNumberOrLocation = obj.optString("meterNumberOrLocation", ""),
                    notes = obj.optString("notes", ""),
                    lastDeliveryDate = if (obj.isNull("lastDeliveryDate")) null else obj.optLong("lastDeliveryDate"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // System Users
        val users = mutableListOf<SystemUser>()
        val userArray = root.optJSONArray("systemUsers") ?: JSONArray()
        for (i in 0 until userArray.length()) {
            val obj = userArray.getJSONObject(i)
            users.add(
                SystemUser(
                    id = obj.optLong("id", 0),
                    username = obj.optString("username", "user"),
                    displayName = obj.optString("displayName", "مستخدم"),
                    role = obj.optString("role", "CASHIER"),
                    canViewReports = obj.optBoolean("canViewReports", true),
                    canEditProducts = obj.optBoolean("canEditProducts", true),
                    canManageDebts = obj.optBoolean("canManageDebts", true),
                    canExportBackup = obj.optBoolean("canExportBackup", true),
                    canManageUsers = obj.optBoolean("canManageUsers", false),
                    isActive = obj.optBoolean("isActive", true)
                )
            )
        }

        val summary = BackupSummary(
            exportDateFormatted = formattedDate,
            timestamp = timestamp,
            accountsCount = accounts.size,
            debtsCount = debts.size,
            suppliersCount = suppliers.size,
            productsCount = products.size,
            inventoryMovementsCount = movements.size,
            invoicesCount = invoices.size,
            generatorExpensesCount = generatorExpenses.size,
            customersCount = customers.size,
            receiptsCount = receipts.size,
            ampSubscribersCount = subscribers.size,
            totalRevenue = invoices.sumOf { it.totalAmount },
            totalCustomerDebt = customers.sumOf { it.currentDebt },
            appVersion = appVer
        )

        return ParsedBackupPayload(
            summary = summary,
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
            customerReceipts = receipts,
            ampSubscribers = subscribers,
            systemUsers = users
        )
    }

    /**
     * Saves JSON string into app's local backups directory
     */
    fun saveBackupToFile(context: Context, jsonString: String): File {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(backupDir, "ledger_backup_$timestamp.json")
        val writer = FileWriter(file)
        writer.write(jsonString)
        writer.flush()
        writer.close()
        return file
    }

    /**
     * Shares backup file using Android system share sheet
     */
    fun shareBackupFile(context: Context, file: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية لنظام ليدجر المحاسبي (${file.name})")
            putExtra(Intent.EXTRA_TEXT, "نسخة احتياطية كاملة لقاعدة بيانات تطبيق ليدجر Ledger المحاسبي.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو حفظ النسخة الاحتياطية JSON"))
    }

    /**
     * Reads text content from a content Uri picked by file picker
     */
    fun readTextFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("تعذر فتح ملف النسخة الاحتياطية المحدد")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append('\n')
        }
        reader.close()
        inputStream.close()
        return sb.toString()
    }

    /**
     * Lists existing local backups saved in internal storage
     */
    fun listLocalBackups(context: Context): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
