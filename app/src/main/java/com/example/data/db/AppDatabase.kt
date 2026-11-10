package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Account::class,
        AccountTransaction::class,
        DebtRecord::class,
        Supplier::class,
        Product::class,
        InventoryMovement::class,
        GeneratorExpense::class,
        Invoice::class,
        InvoiceItem::class,
        Customer::class,
        CustomerReceipt::class,
        AmpSubscriber::class,
        SystemUser::class,
        ScheduledNotification::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun debtDao(): DebtDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun generatorExpenseDao(): GeneratorExpenseDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerReceiptDao(): CustomerReceiptDao
    abstract fun ampSubscriberDao(): AmpSubscriberDao
    abstract fun systemUserDao(): SystemUserDao
    abstract fun scheduledNotificationDao(): ScheduledNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accounting_master.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val accountDao = database.accountDao()
            val debtDao = database.debtDao()
            val supplierDao = database.supplierDao()
            val productDao = database.productDao()
            val movementDao = database.inventoryMovementDao()
            val expenseDao = database.generatorExpenseDao()
            val invoiceDao = database.invoiceDao()
            val customerDao = database.customerDao()
            val ampDao = database.ampSubscriberDao()
            val userDao = database.systemUserDao()
            val notificationDao = database.scheduledNotificationDao()

            // 1. Initial Financial Accounts (الصناديق والحسابات المالية)
            val mainCashId = accountDao.insertAccount(
                Account(
                    name = "الصندوق النقدي الرئيسي (كاش)",
                    code = "101",
                    type = "CASH",
                    balance = 3500000.0,
                    currency = "IQD",
                    isDefault = true,
                    description = "صندوق الكاش اليومي لحركات البيع والمشتريات"
                )
            )
            val bankAccountId = accountDao.insertAccount(
                Account(
                    name = "حساب مصرف الرافدين",
                    code = "102",
                    type = "BANK",
                    balance = 8200000.0,
                    currency = "IQD",
                    isDefault = false,
                    description = "الحساب المصرفي التجاري للتحويلات والودائع"
                )
            )
            val zainCashId = accountDao.insertAccount(
                Account(
                    name = "محفظة زين كاش التجارية",
                    code = "103",
                    type = "WALLET",
                    balance = 1450000.0,
                    currency = "IQD",
                    isDefault = false,
                    description = "استلام الدفعات الإلكترونية وفواتير الزبائن"
                )
            )
            accountDao.insertAccount(
                Account(
                    name = "حساب إيرادات المولدات والأمبيرات",
                    code = "104",
                    type = "REVENUE",
                    balance = 480000.0,
                    currency = "IQD",
                    isDefault = false,
                    description = "مخصص لحصيلة بيع الأمبيرات وسداد كاز المولد"
                )
            )

            // Initial Account Transactions (سجل القيود)
            accountDao.insertTransaction(
                AccountTransaction(
                    accountId = mainCashId,
                    accountName = "الصندوق النقدي الرئيسي (كاش)",
                    type = "DEPOSIT",
                    amount = 3500000.0,
                    balanceAfter = 3500000.0,
                    category = "رصيد افتتاحي",
                    referenceType = "MANUAL",
                    notes = "افتتاح الخزينة النقدية الرئيسية"
                )
            )
            accountDao.insertTransaction(
                AccountTransaction(
                    accountId = bankAccountId,
                    accountName = "حساب مصرف الرافدين",
                    type = "DEPOSIT",
                    amount = 8200000.0,
                    balanceAfter = 8200000.0,
                    category = "رصيد افتتاحي",
                    referenceType = "MANUAL",
                    notes = "افتتاح الحساب البنكي التجاري"
                )
            )

            // 2. Initial Suppliers (الموردون)
            val sup1Id = supplierDao.insertSupplier(
                Supplier(
                    name = "شركة النور للتجارة والتوزيع",
                    phone = "07712349999",
                    company = "مجموعة النور للتوريدات الكهربائية",
                    balanceOwed = 350000.0,
                    address = "بغداد - الكفاح",
                    notes = "توريد أسبوعي للمواد والأسلاك الكهربائية"
                )
            )
            val sup2Id = supplierDao.insertSupplier(
                Supplier(
                    name = "مستودعات الفرات الكبرى",
                    phone = "07802223344",
                    company = "الفرات للمواد الصناعية والزيوت",
                    balanceOwed = 120000.0,
                    address = "البصرة - الميناء",
                    notes = "دفعة مؤجلة لشحنة الزيوت الصناعية"
                )
            )

            // 3. Initial Inventory Products & Movements (المخزون وحركة المواد)
            val p1Id = productDao.insertProduct(
                Product(
                    name = "زيت المحرك الذهبي 1 لتر",
                    barcode = "629110022331",
                    buyPrice = 8000.0,
                    sellPrice = 12000.0,
                    stockQuantity = 24.0,
                    minStockAlert = 5.0,
                    category = "زيوت ومحركات",
                    location = "الرف A1"
                )
            )
            val p2Id = productDao.insertProduct(
                Product(
                    name = "قاطع دورة كهربائي 32 أمبير",
                    barcode = "789123456789",
                    buyPrice = 4500.0,
                    sellPrice = 7500.0,
                    stockQuantity = 12.0,
                    minStockAlert = 4.0,
                    category = "كهربائيات",
                    location = "الرف B2"
                )
            )
            val p3Id = productDao.insertProduct(
                Product(
                    name = "سلك نحاسي معزول 2.5 ملم (لفة)",
                    barcode = "590123456789",
                    buyPrice = 25000.0,
                    sellPrice = 35000.0,
                    stockQuantity = 3.0, // Low stock alert!
                    minStockAlert = 5.0,
                    category = "كهربائيات",
                    location = "المستودع الرئيسي"
                )
            )
            val p4Id = productDao.insertProduct(
                Product(
                    name = "مصباح LED توفير طاقة 40 واط",
                    barcode = "692123456789",
                    buyPrice = 2000.0,
                    sellPrice = 3500.0,
                    stockQuantity = 45.0,
                    minStockAlert = 10.0,
                    category = "إنارة",
                    location = "الواجهة"
                )
            )

            // Inventory Movement Records
            movementDao.insertMovement(
                InventoryMovement(
                    productId = p1Id,
                    productName = "زيت المحرك الذهبي 1 لتر",
                    movementType = "IN_PURCHASE",
                    quantity = 24.0,
                    unitPrice = 8000.0,
                    notes = "فاتورة توريد واردة من شركة الفرات"
                )
            )
            movementDao.insertMovement(
                InventoryMovement(
                    productId = p3Id,
                    productName = "سلك نحاسي معزول 2.5 ملم (لفة)",
                    movementType = "IN_PURCHASE",
                    quantity = 10.0,
                    unitPrice = 25000.0,
                    notes = "رصيد افتتاحي للمخزون"
                )
            )

            // 4. Initial Customers (العملاء)
            val c1Id = customerDao.insertCustomer(
                Customer(
                    name = "محمد علي الركابي",
                    phone = "07701234567",
                    address = "بغداد - الكرادة",
                    currentDebt = 85000.0,
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 3,
                    notes = "زبون دائم، استحقاق الدفعة نهاية الأسبوع"
                )
            )
            val c2Id = customerDao.insertCustomer(
                Customer(
                    name = "حيدر الكعبي",
                    phone = "07809876543",
                    address = "البصرة - العشار",
                    currentDebt = 40000.0,
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 5,
                    notes = "تسديد دفعات عبر زين كاش"
                )
            )
            customerDao.insertCustomer(
                Customer(
                    name = "سعد التميمي",
                    phone = "07505551234",
                    address = "أربيل",
                    currentDebt = 0.0,
                    notes = "حساب مسدد بالكامل"
                )
            )

            // 5. Initial Debts Ledger (سجل الديون والذمم المالي)
            // Customer debts (ديون العملاء - لنا)
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = "محمد علي الركابي",
                    debtorPhone = "07701234567",
                    debtType = "RECEIVABLE",
                    relatedCustomerId = c1Id,
                    initialAmount = 85000.0,
                    paidAmount = 0.0,
                    remainingAmount = 85000.0,
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 3,
                    status = "ACTIVE",
                    notes = "دين فاتورة قواطع وأسلاك كهربائية"
                )
            )
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = "حيدر الكعبي",
                    debtorPhone = "07809876543",
                    debtType = "RECEIVABLE",
                    relatedCustomerId = c2Id,
                    initialAmount = 40000.0,
                    paidAmount = 0.0,
                    remainingAmount = 40000.0,
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 5,
                    status = "ACTIVE",
                    notes = "دفعة مؤجلة من فاتورة صيانة"
                )
            )
            // Supplier debts (ديون الموردين - علينا)
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = "شركة النور للتجارة والتوزيع",
                    debtorPhone = "07712349999",
                    debtType = "PAYABLE",
                    relatedSupplierId = sup1Id,
                    initialAmount = 350000.0,
                    paidAmount = 0.0,
                    remainingAmount = 350000.0,
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 10,
                    status = "ACTIVE",
                    notes = "مستحق توريد أسلاك ومواد كهربائية"
                )
            )
            debtDao.insertDebt(
                DebtRecord(
                    debtorName = "مستودعات الفرات الكبرى",
                    debtorPhone = "07802223344",
                    debtType = "PAYABLE",
                    relatedSupplierId = sup2Id,
                    initialAmount = 120000.0,
                    paidAmount = 0.0,
                    remainingAmount = 120000.0,
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 14,
                    status = "ACTIVE",
                    notes = "دفعة مؤجلة لشحنة زيوت"
                )
            )

            // 6. Initial Generator Expenses (صرفيات المولد: كاز، زيت، صيانة وفلاتر)
            expenseDao.insertExpense(
                GeneratorExpense(
                    type = "DIESEL",
                    title = "تعبئة كاز (ديزل) خزان المولد الرئيسي",
                    amount = 130000.0,
                    quantity = 150.0,
                    unit = "لتر",
                    paidFromAccountId = mainCashId,
                    notes = "تجهيز وجبة مسائية بسعر 860 د.ع للتر"
                )
            )
            expenseDao.insertExpense(
                GeneratorExpense(
                    type = "OIL",
                    title = "تبديل زيت المحرك (دهن 15W40)",
                    amount = 40000.0,
                    quantity = 20.0,
                    unit = "لتر",
                    paidFromAccountId = mainCashId,
                    notes = "تبديل دوري بعد 150 ساعة تشغيل"
                )
            )
            expenseDao.insertExpense(
                GeneratorExpense(
                    type = "MAINTENANCE",
                    title = "تبديل فلاتر ديزل وقايش دينامو",
                    amount = 25000.0,
                    quantity = 2.0,
                    unit = "قطعة",
                    paidFromAccountId = mainCashId,
                    notes = "صيانة دورية مع تنظيف الراديتر"
                )
            )

            // 7. Initial Amp Subscribers (نظام بيع واشتراك الأمبيرات)
            ampDao.insertSubscriber(
                AmpSubscriber(
                    name = "أبو مروان (عمارة الشروق)",
                    phone = "07701112233",
                    amperes = 10.0,
                    pricePerAmpere = 15000.0,
                    monthlyTotal = 150000.0,
                    debtAmount = 0.0,
                    isBillDelivered = true, // تم توصيل الفاتورة 🟢
                    meterNumberOrLocation = "جوزة رقم 14 - لوحة A",
                    notes = "تم تسليم الوصل والقبض نقداً"
                )
            )
            ampDao.insertSubscriber(
                AmpSubscriber(
                    name = "حسين جاسم (محل الحلاقة)",
                    phone = "07804445566",
                    amperes = 6.0,
                    pricePerAmpere = 15000.0,
                    monthlyTotal = 90000.0,
                    debtAmount = 90000.0, // لم يسدد وعليه ديون
                    isBillDelivered = false, // لم يتم توصيل الفاتورة 🔴
                    meterNumberOrLocation = "جوزة رقم 08 - لوحة B",
                    notes = "يرجى إرسال الفاتورة وتذكير الواتساب"
                )
            )
            ampDao.insertSubscriber(
                AmpSubscriber(
                    name = "مكتب الرافدين للعقار",
                    phone = "07718889900",
                    amperes = 4.0,
                    pricePerAmpere = 15000.0,
                    monthlyTotal = 60000.0,
                    debtAmount = 30000.0,
                    isBillDelivered = false, // 🔴 لم يتم توصيل الفاتورة
                    meterNumberOrLocation = "جوزة رقم 22 - لوحة C",
                    notes = "طلب مراجعة قراءة العداد"
                )
            )
            ampDao.insertSubscriber(
                AmpSubscriber(
                    name = "الدكتور مصطفى النعيمي",
                    phone = "07817776655",
                    amperes = 15.0,
                    pricePerAmpere = 15000.0,
                    monthlyTotal = 225000.0,
                    debtAmount = 0.0,
                    isBillDelivered = true, // 🟢 تم التوصيل
                    meterNumberOrLocation = "جوزة رقم 01 - العيادة",
                    notes = "تسديد فوري عبر المندوب"
                )
            )

            // 8. Initial Invoices (فواتير سابقة)
            val inv1Id = invoiceDao.insertInvoice(
                Invoice(
                    invoiceNumber = "1001",
                    type = "SALE",
                    customerId = c1Id,
                    customerName = "محمد علي الركابي",
                    customerPhone = "07701234567",
                    accountId = mainCashId,
                    totalAmount = 85000.0,
                    paidAmount = 0.0,
                    remainingDebt = 85000.0,
                    paymentMethod = "DEBT",
                    isDelivered = true,
                    notes = "فاتورة بيع آجل (دين مسجل)",
                    createdBy = "أحمد فرحان السلماني"
                )
            )
            invoiceDao.insertInvoiceItems(
                listOf(
                    InvoiceItem(
                        invoiceId = inv1Id,
                        productId = p2Id,
                        productName = "قاطع دورة كهربائي 32 أمبير",
                        barcode = "789123456789",
                        quantity = 2.0,
                        unitPrice = 7500.0,
                        buyPrice = 4500.0,
                        totalPrice = 15000.0
                    ),
                    InvoiceItem(
                        invoiceId = inv1Id,
                        productId = p3Id,
                        productName = "سلك نحاسي معزول 2.5 ملم (لفة)",
                        barcode = "590123456789",
                        quantity = 2.0,
                        unitPrice = 35000.0,
                        buyPrice = 25000.0,
                        totalPrice = 70000.0
                    )
                )
            )

            // 9. Initial Users & Notifications
            userDao.insertUser(
                SystemUser(
                    username = "admin",
                    displayName = "أحمد فرحان السلماني (المدير العام)",
                    role = "ADMIN",
                    canViewReports = true,
                    canEditProducts = true,
                    canManageDebts = true,
                    canExportBackup = true,
                    canManageUsers = true
                )
            )
            userDao.insertUser(
                SystemUser(
                    username = "cashier1",
                    displayName = "كاشير المبيعات",
                    role = "CASHIER",
                    canViewReports = false,
                    canEditProducts = false,
                    canManageDebts = true,
                    canExportBackup = false,
                    canManageUsers = false
                )
            )
            userDao.insertUser(
                SystemUser(
                    username = "accountant1",
                    displayName = "محاسب النظام",
                    role = "ACCOUNTANT",
                    canViewReports = true,
                    canEditProducts = true,
                    canManageDebts = true,
                    canExportBackup = true,
                    canManageUsers = false
                )
            )

            notificationDao.insertNotification(
                ScheduledNotification(
                    title = "استحقاق دين: محمد علي الركابي",
                    description = "مستحق 85,000 د.ع - يرجى إرسال تذكير عبر واتساب",
                    relatedType = "DEBT_DUE",
                    targetDateMillis = System.currentTimeMillis() + 86400000L * 2
                )
            )
            notificationDao.insertNotification(
                ScheduledNotification(
                    title = "انخفاض مخزون: سلك نحاسي معزول",
                    description = "الكمية المتبقية 3 لفات (الحد الأدنى 5)",
                    relatedType = "LOW_STOCK",
                    targetDateMillis = System.currentTimeMillis()
                )
            )
            notificationDao.insertNotification(
                ScheduledNotification(
                    title = "توزيع فواتير اشتراك الأمبيرات لشهر 9",
                    description = "متبقي توصيل فواتير لـ 2 مشتركين (دائرة حمراء)",
                    relatedType = "AMPERE_COLLECTION",
                    targetDateMillis = System.currentTimeMillis() + 86400000L
                )
            )
        }
    }
}
