package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1. الحسابات المالية والصناديق (Accounts & Wallets)
 * مثل: الصندوق النقدي الرئيسي، حساب مصرف الرافدين، محفظة زين كاش
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val type: String = "CASH", // "CASH" (نقدي), "BANK" (مصرفي), "WALLET" (محفظة رقمية), "CAPITAL" (رأس مال), "REVENUE" (إيرادات)
    val balance: Double = 0.0,
    val currency: String = "IQD",
    val description: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * سجل الحركات المالية للحسابات (Account Transactions)
 */
@Entity(tableName = "account_transactions")
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val accountName: String = "",
    val type: String = "DEPOSIT", // "DEPOSIT", "WITHDRAW", "TRANSFER_IN", "TRANSFER_OUT"
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val category: String = "عام",
    val referenceType: String = "MANUAL", // "INVOICE", "DEBT_PAYMENT", "EXPENSE", "AMPERE", "MANUAL"
    val referenceId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 2. سجل الديون والذمم (Debts & Receivables / Payables Ledger)
 * يشمل ديون العملاء (لنا) وديون الموردين (علينا) والديون العامة
 */
@Entity(tableName = "debts")
data class DebtRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtorName: String,
    val debtorPhone: String = "",
    val debtType: String = "RECEIVABLE", // "RECEIVABLE" (لنا على عميل), "PAYABLE" (علينا لمورد), "GENERAL" (دين عام)
    val relatedCustomerId: Long? = null,
    val relatedSupplierId: Long? = null,
    val initialAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val currency: String = "IQD",
    val dueDateMillis: Long? = null,
    val status: String = "ACTIVE", // "ACTIVE", "PAID", "OVERDUE"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 3. الموردون (Suppliers)
 */
@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val company: String = "",
    val balanceOwed: Double = 0.0,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 4. المخزون والمواد (Inventory & Products)
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val currency: String = "IQD", // "IQD" or "USD"
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val category: String = "عام",
    val unit: String = "قطعة",
    val location: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * حركة المخزون (Inventory Movements)
 */
@Entity(tableName = "inventory_movements")
data class InventoryMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: String = "OUT_SALE", // "IN_PURCHASE", "OUT_SALE", "ADJUSTMENT", "RETURN"
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val referenceId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 5. الفواتير والمبيعات (Invoices)
 */
@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val type: String = "SALE", // "SALE", "PURCHASE", "AMPERE_BILL", "RETURN"
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val supplierId: Long? = null,
    val supplierName: String = "",
    val accountId: Long? = null, // الحساب المالي المستخدم (الصندوق/البنك)
    val totalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingDebt: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val currency: String = "IQD",
    val exchangeRate: Double = 1.0,
    val paymentMethod: String = "CASH", // "CASH", "DEBT", "BANK_TRANSFER", "PARTIAL"
    val isDelivered: Boolean = false,
    val photoUri: String? = null,
    val notes: String = "",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDateMillis: Long? = null
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long? = null,
    val productName: String,
    val barcode: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val buyPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

/**
 * 6. صرف ومصاريف المولد (Generator Expenses: كاز، زيت، صيانة وفلاتر)
 */
@Entity(tableName = "generator_expenses")
data class GeneratorExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = "DIESEL", // "DIESEL" (كاز / ديزل), "OIL" (زيت المحرك), "MAINTENANCE" (صيانة وفلاتر), "SALARY" (أجور تشغيل), "OTHER" (أخرى)
    val title: String = "",
    val amount: Double = 0.0,
    val quantity: Double = 0.0,
    val unit: String = "لتر",
    val paidFromAccountId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val currentDebt: Double = 0.0,
    val notes: String = "",
    val dueDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_receipts")
data class CustomerReceipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val currency: String = "IQD",
    val paymentMethod: String = "نقدي",
    val receiptNumber: String,
    val photoUri: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * نظام بيع الأمبيرات والمولدات
 */
@Entity(tableName = "amp_subscribers")
data class AmpSubscriber(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val amperes: Double = 5.0, // كم أمبير يسحب
    val pricePerAmpere: Double = 15000.0, // سعر الأمبير
    val monthlyTotal: Double = 75000.0, // المبلغ الشهري = الأمبيرات * السعر
    val debtAmount: Double = 0.0, // كم عليه ديون
    val isBillDelivered: Boolean = false, // تم توصيل الفاتورة؟ (أخضر / أحمر)
    val meterNumberOrLocation: String = "",
    val notes: String = "",
    val lastDeliveryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_users")
data class SystemUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val role: String, // "ADMIN", "ACCOUNTANT", "CASHIER"
    val canViewReports: Boolean = true,
    val canEditProducts: Boolean = true,
    val canManageDebts: Boolean = true,
    val canExportBackup: Boolean = true,
    val canManageUsers: Boolean = false,
    val isActive: Boolean = true
)

@Entity(tableName = "scheduled_tasks")
data class ScheduledNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val relatedType: String, // "DEBT_DUE", "LOW_STOCK", "AMPERE_COLLECTION"
    val targetDateMillis: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
