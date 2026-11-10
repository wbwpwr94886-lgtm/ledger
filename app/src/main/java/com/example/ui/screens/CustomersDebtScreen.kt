package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.PdfInvoiceExportHelper
import com.example.util.WhatsAppHelper
import com.example.util.AmountInputField
import com.example.util.ValidationHelper
import com.example.util.ValidationResult
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersDebtScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val customers by viewModel.customers.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val receipts by viewModel.receipts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerDetail by remember { mutableStateOf<Customer?>(null) }
    var selectedInvoiceForWhatsApp by remember { mutableStateOf<Invoice?>(null) }
    var invoiceItemsForShare by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var showReceiptPaymentDialogFor by remember { mutableStateOf<Customer?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentNotesInput by remember { mutableStateOf("") }
    var paymentReceiptPhotoAttached by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

    val totalDebt = customers.sumOf { it.currentDebt }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SkyPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل جديد")
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(padding)
        ) {
            // Header summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "إجمالي ديون العملاء" else "Total Customer Debts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LanguageManager.formatMoney(totalDebt, currency, isAr),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDebt > 0) RoseDebt else EmeraldProfit
                            )
                        }

                        Surface(
                            color = SkyLight,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isAr) "${customers.size} عميل مسجل" else "${customers.size} Customers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SkyDeep,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isAr) "بحث بالاسم أو رقم الهاتف..." else "Search customer by name/phone...", color = Slate400, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SkyPrimary,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Customer List
            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = if (isAr) "لا يوجد عملاء مطابقين" else "No customers found", color = Slate400)
                        }
                    }
                }
            } else {
                items(filteredCustomers) { customer ->
                    val hasDebt = customer.currentDebt > 0
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, if (hasDebt) RoseDebt.copy(alpha = 0.35f) else Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomerDetail = customer }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (hasDebt) StatusRedBg else StatusGreenBg,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (hasDebt) StatusRed else StatusGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                        Text(
                                            text = customer.phone.ifBlank { if (isAr) "بدون هاتف" else "No phone" },
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = LanguageManager.formatMoney(customer.currentDebt, currency, isAr),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasDebt) RoseDebt else EmeraldProfit
                                    )
                                    Text(
                                        text = if (hasDebt) (if (isAr) "مستحق (دين)" else "Debt Due") else (if (isAr) "حساب مسدد ✓" else "Settled ✓"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (hasDebt) StatusRed else StatusGreen
                                    )
                                }
                            }

                            if (customer.dueDateMillis != null && hasDebt) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = AmberBg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "تاريخ الاستحقاق: ${LanguageManager.formatDateShort(customer.dueDateMillis, isAr)}",
                                            fontSize = 11.sp,
                                            color = Slate800
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { selectedCustomerDetail = customer },
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = SkyPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "سجل الحساب والفواتير" else "Account Ledger", fontSize = 12.sp, color = SkyPrimary)
                                }

                                Row {
                                    if (hasDebt) {
                                        FilledTonalButton(
                                            onClick = {
                                                showReceiptPaymentDialogFor = customer
                                                paymentAmountInput = customer.currentDebt.toInt().toString()
                                            },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = StatusGreenBg,
                                                contentColor = StatusGreen
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAr) "سند قبض" else "Receipt", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                WhatsAppHelper.sendDebtReminder(context, customer, currency.code)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = "تذكير واتساب", tint = GreenSuccess)
                                        }
                                    }

                                    if (customer.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = BlueInfo)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var initialDebt by remember { mutableStateOf("0") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isAr) "إضافة عميل جديد" else "Add New Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isAr) "اسم العميل الكامل" else "Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isAr) "رقم الهاتف (لرسائل الواتساب)" else "Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isAr) "العنوان أو المحافظة" else "Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AmountInputField(
                        value = initialDebt,
                        onValueChange = { initialDebt = it },
                        label = if (isAr) "الرصيد الافتتاحي (دين سابق إن وجد)" else "Initial Debt",
                        allowZero = true,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isAr) "ملاحظات إضافية" else "Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال اسم العميل", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val debtValidation = ValidationHelper.validateAmount(initialDebt, allowZero = true, isAr = isAr)
                        if (debtValidation is ValidationResult.Invalid) {
                            Toast.makeText(context, debtValidation.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val d = (debtValidation as ValidationResult.Valid).value
                        viewModel.addCustomer(
                            Customer(
                                name = name,
                                phone = phone,
                                address = address,
                                currentDebt = d,
                                notes = notes,
                                dueDateMillis = if (d > 0) System.currentTimeMillis() + 86400000L * 7 else null
                            )
                        )
                        showAddDialog = false
                        Toast.makeText(context, "تم حفظ العميل بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Customer Account Ledger Detail Dialog
    if (selectedCustomerDetail != null) {
        val cust = selectedCustomerDetail!!
        val customerInvoices = invoices.filter { it.customerId == cust.id }
        val customerReceipts = receipts.filter { it.customerId == cust.id }

        AlertDialog(
            onDismissRequest = { selectedCustomerDetail = null },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "سجل حساب: ${cust.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val pdfFile = PdfInvoiceExportHelper.exportCustomerAccountStatementPdf(
                                        context = context,
                                        customer = cust,
                                        invoices = customerInvoices,
                                        receipts = customerReceipts,
                                        currencySymbol = currency.code,
                                        isArabic = isAr
                                    )
                                    if (pdfFile != null) {
                                        PdfInvoiceExportHelper.sharePdf(context, pdfFile, if (isAr) "كشف حساب عميل: ${cust.name}" else "Customer Statement: ${cust.name}")
                                    } else {
                                        Toast.makeText(context, if (isAr) "فشل إنشاء كشف الحساب" else "Failed to generate statement", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("btn_export_customer_pdf")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير كشف حساب PDF", tint = SkyPrimary)
                            }

                            if (cust.phone.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        WhatsAppHelper.sendAccountStatement(
                                            context = context,
                                            customer = cust,
                                            invoicesCount = customerInvoices.size,
                                            receiptsCount = customerReceipts.size,
                                            currencySymbol = currency.code
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة كشف الحساب واتساب", tint = GreenSuccess)
                                }
                            }
                        }
                    }
                    Text(text = "إجمالي الدين الحالي: ${cust.currentDebt} ${currency.code}", color = RedAlert, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    item {
                        Text(text = if (isAr) "الفواتير السابقة:" else "Invoices History:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (customerInvoices.isEmpty()) {
                        item { Text(text = "لا توجد فواتير سابقة", color = Color.Gray, fontSize = 12.sp) }
                    } else {
                        items(customerInvoices) { inv ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "فاتورة #${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = LanguageManager.formatDateShort(inv.createdAt, isAr), fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val items = viewModel.repository.getInvoiceItemsSync(inv.id)
                                                        val pdfFile = PdfInvoiceExportHelper.exportInvoicePdf(
                                                            context = context,
                                                            invoice = inv,
                                                            items = items,
                                                            currencySymbol = currency.code,
                                                            isArabic = isAr
                                                        )
                                                        if (pdfFile != null) {
                                                            PdfInvoiceExportHelper.sharePdf(context, pdfFile, if (isAr) "فاتورة مبيعات #${inv.invoiceNumber}" else "Invoice #${inv.invoiceNumber}")
                                                        } else {
                                                            Toast.makeText(context, if (isAr) "فشل إنشاء الفاتورة" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp).testTag("btn_export_invoice_pdf_${inv.id}")
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير فاتورة PDF", tint = SkyPrimary, modifier = Modifier.size(16.dp))
                                            }
                                            if (cust.phone.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val items = viewModel.repository.getInvoiceItemsSync(inv.id)
                                                            WhatsAppHelper.sendInvoiceNotification(
                                                                context = context,
                                                                customerPhone = cust.phone,
                                                                customerName = cust.name,
                                                                invoice = inv,
                                                                items = items,
                                                                currencySymbol = currency.code
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = "إرسال الفاتورة واتساب", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    Text(text = "المبلغ: ${inv.totalAmount} | المدفوع: ${inv.paidAmount} | المتبقي: ${inv.remainingDebt}", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = if (isAr) "سندات القبض والدفعات (الإيصالات):" else "Receipts & Payments:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (customerReceipts.isEmpty()) {
                        item { Text(text = "لا توجد سندات قبض مسجلة", color = Color.Gray, fontSize = 12.sp) }
                    } else {
                        items(customerReceipts) { rec ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "سند قبض #${rec.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenSuccess)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = LanguageManager.formatDateShort(rec.createdAt, isAr), fontSize = 11.sp, color = Color.Gray)
                                            if (cust.phone.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        WhatsAppHelper.sendPaymentReceipt(
                                                            context = context,
                                                            customerPhone = cust.phone,
                                                            customerName = cust.name,
                                                            receiptNumber = rec.receiptNumber,
                                                            amount = rec.amount,
                                                            currencySymbol = rec.currency,
                                                            remainingDebt = cust.currentDebt,
                                                            paymentMethod = rec.paymentMethod,
                                                            notes = rec.notes
                                                        )
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = "إرسال سند القبض واتساب", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    Text(text = "المبلغ المسدد: ${rec.amount} ${rec.currency} (${rec.notes})", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedCustomerDetail = null }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            }
        )
    }

    // Record Payment Receipt Dialog
    if (showReceiptPaymentDialogFor != null) {
        val cust = showReceiptPaymentDialogFor!!
        var sendViaWhatsAppCheckbox by remember { mutableStateOf(cust.phone.isNotBlank()) }

        AlertDialog(
            onDismissRequest = { showReceiptPaymentDialogFor = null },
            title = { Text(if (isAr) "تسجيل سند قبض / دفعة: ${cust.name}" else "Record Payment Receipt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "الدين الحالي: ${cust.currentDebt} ${currency.code}", color = RedAlert, fontWeight = FontWeight.Bold)
                    AmountInputField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = if (isAr) "المبلغ المقبوض" else "Received Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                    OutlinedTextField(
                        value = paymentNotesInput,
                        onValueChange = { paymentNotesInput = it },
                        label = { Text(if (isAr) "طريقة الدفع أو ملاحظة (نقداً / زين كاش)" else "Notes / Method") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // WhatsApp Receipt checkbox
                    if (cust.phone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendViaWhatsAppCheckbox,
                                onCheckedChange = { sendViaWhatsAppCheckbox = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إرسال وصل القبض فوراً إلى واتساب العميل 📲",
                                fontSize = 12.sp,
                                color = GreenSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Photo of receipt toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (paymentReceiptPhotoAttached) "تم إرفاق صورة الوصل الورقي ✓" else "إرفاق صورة الوصل الورقي",
                            fontSize = 12.sp,
                            color = if (paymentReceiptPhotoAttached) GreenSuccess else Color.Gray
                        )
                        FilledTonalButton(
                            onClick = {
                                paymentReceiptPhotoAttached = true
                                Toast.makeText(context, "تم التقاط وحفظ صورة سند القبض", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("تصوير الوصل", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = ValidationHelper.validateAmount(paymentAmountInput, allowZero = false, isAr = isAr)
                        if (validation is ValidationResult.Invalid) {
                            Toast.makeText(context, validation.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amount = (validation as ValidationResult.Valid).value
                        if (amount > 0) {
                            val notes = paymentNotesInput.ifBlank { "دفعة نقدية مسددة" }
                            val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
                            viewModel.addReceiptPayment(
                                customerId = cust.id,
                                customerName = cust.name,
                                amount = amount,
                                notes = notes,
                                photoUri = if (paymentReceiptPhotoAttached) "file:///receipt_${System.currentTimeMillis()}.jpg" else null
                            )

                            if (sendViaWhatsAppCheckbox && cust.phone.isNotBlank()) {
                                val remainingAfter = maxOf(0.0, cust.currentDebt - amount)
                                WhatsAppHelper.sendPaymentReceipt(
                                    context = context,
                                    customerPhone = cust.phone,
                                    customerName = cust.name,
                                    receiptNumber = receiptNo,
                                    amount = amount,
                                    currencySymbol = currency.code,
                                    remainingDebt = remainingAfter,
                                    paymentMethod = "نقدي",
                                    notes = notes
                                )
                            }

                            Toast.makeText(context, "تم حفظ سند القبض وخصم الدين بنجاح", Toast.LENGTH_SHORT).show()
                            showReceiptPaymentDialogFor = null
                        }
                    }
                ) {
                    Text(if (isAr) "تأكيد وقبض" else "Confirm & Receive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReceiptPaymentDialogFor = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
