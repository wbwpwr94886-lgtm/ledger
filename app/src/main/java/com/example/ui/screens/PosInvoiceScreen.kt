package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppLanguage
import com.example.util.DirectPrintHelper
import com.example.util.LanguageManager
import com.example.util.PdfInvoiceExportHelper
import com.example.util.WhatsAppHelper
import com.example.util.AmountInputField
import com.example.util.ValidationHelper
import com.example.util.ValidationResult
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosInvoiceScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val taxRate by viewModel.taxRatePercent.collectAsState()
    val discount by viewModel.discountAmount.collectAsState()
    val paidInput by viewModel.paidAmountInput.collectAsState()
    val isDelivered by viewModel.isDeliveredCheck.collectAsState()
    val attachedPhoto by viewModel.attachedReceiptPhotoUri.collectAsState()

    var showCameraScanner by remember { mutableStateOf(false) }
    var barcodeInput by remember { mutableStateOf("") }
    var productSearchQuery by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var lastCompletedInvoice by remember { mutableStateOf<Pair<Invoice, List<InvoiceItem>>?>(null) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val subtotal = cartItems.sumOf { it.totalPrice } - discount
    val safeSubtotal = maxOf(0.0, subtotal)
    val taxAmount = safeSubtotal * (taxRate / 100.0)
    val totalAmount = safeSubtotal + taxAmount
    val enteredPaid = paidInput.toDoubleOrNull() ?: totalAmount
    val paidAmount = minOf(totalAmount, maxOf(0.0, enteredPaid))
    val remainingDebt = maxOf(0.0, totalAmount - paidAmount)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Barcode Scanner Toolbar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "مسح الباركود والبحث السريع" else "Barcode Scanner & Quick Search",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { showCameraScanner = !showCameraScanner }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "كاميرا الباركود",
                                tint = if (showCameraScanner) AmberAccent else NavyPrimary
                            )
                        }
                    }

                    // Interactive Camera Viewfinder
                    AnimatedVisibility(visible = showCameraScanner) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(Color.Black, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp, 120.dp)
                                    .border(BorderStroke(2.dp, GreenSuccess), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAr) "وجّه الكاميرا نحو الباركود" else "Align barcode in frame",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                    // Animated scan line effect
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(2.dp)
                                            .background(RedAlert)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAr) "انقر على أحد المنتجات أدناه لمحاكاة القراءة الفورية:" else "Tap any sample barcode below to simulate instant scan:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick test barcode chips from available products
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(products.take(4)) { prod ->
                                    ElevatedFilterChip(
                                        selected = false,
                                        onClick = {
                                            viewModel.scanBarcode(prod.barcode) { found, name ->
                                                Toast.makeText(context, "تم قراءة: $name", Toast.LENGTH_SHORT).show()
                                                showCameraScanner = false
                                            }
                                        },
                                        label = { Text("${prod.name} (${prod.barcode.takeLast(4)})", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.QrCode, null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }
                    }

                    // Barcode text input with quick scan button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { barcodeInput = it },
                            label = { Text(if (isAr) "أدخل رقم الباركود يدويًا" else "Manual Barcode") },
                            leadingIcon = { Icon(Icons.Default.QrCode, null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (barcodeInput.isNotBlank()) {
                                    viewModel.scanBarcode(barcodeInput) { found, msg ->
                                        if (found) {
                                            Toast.makeText(context, "تمت إضافة $msg إلى السلة", Toast.LENGTH_SHORT).show()
                                            barcodeInput = ""
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isAr) "إضافة" else "Add")
                        }
                    }
                }
            }
        }

        // Quick Product Selector Grid/Chips
        item {
            Text(
                text = if (isAr) "المنتجات المتوفرة (انقر للإضافة السريعة)" else "Available Products (Tap to Add)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products) { product ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { viewModel.addToCart(product) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LanguageManager.formatMoney(product.sellPrice, currency, isAr),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (product.stockQuantity > product.minStockAlert) GreenSuccess else RedAlert, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${product.stockQuantity.toInt()} ${product.unit}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cart Items List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "سلة الفاتورة (${cartItems.size} أصناف)" else "Cart Items (${cartItems.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text(if (isAr) "إفراغ السلة" else "Clear", color = RedAlert, fontSize = 12.sp)
                    }
                }
            }

            if (cartItems.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isAr) "السلة فارغة. قم بمسح باركود أو اختر منتجاً من القائمة." else "Cart is empty. Scan a barcode or pick a product.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cartItems.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${LanguageManager.formatMoney(item.customPrice, currency, isAr)} × ${item.quantity} = ${LanguageManager.formatMoney(item.totalPrice, currency, isAr)}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, -1.0) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "تقليل", tint = RedAlert)
                                    }
                                    Text(
                                        text = "${item.quantity.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, 1.0) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "زيادة", tint = GreenSuccess)
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeCartItem(item.product.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Customer Selection & Debt assignment
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isAr) "بيانات العميل وحساب الديون" else "Customer & Ledger Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showCustomerPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCustomer?.let { "${it.name} (دين سابق: ${it.currentDebt.toInt()})" } ?: if (isAr) "عميل نقدي (اختر عميل لتقييد دين)" else "Cash Customer (Tap to select)",
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }

                        if (selectedCustomer != null) {
                            IconButton(onClick = { viewModel.selectedCustomer.value = null }) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = RedAlert)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Receipt photo attachment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (attachedPhoto != null) (if (isAr) "تم إرفاق صورة الوصل ✓" else "Receipt attached ✓") else (if (isAr) "إرفاق صورة الفاتورة / الوصل" else "Attach Receipt Photo"),
                                fontSize = 12.sp,
                                color = if (attachedPhoto != null) GreenSuccess else Color.Gray
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                // Simulate capturing receipt / invoice photo
                                viewModel.attachedReceiptPhotoUri.value = "file:///receipt_${System.currentTimeMillis()}.jpg"
                                Toast.makeText(context, "تم حفظ وتوثيق صورة الوصل بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(if (attachedPhoto != null) (if (isAr) "تغيير الصورة" else "Change") else (if (isAr) "تصوير" else "Capture"), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delivery checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDelivered,
                            onCheckedChange = { viewModel.isDeliveredCheck.value = it }
                        )
                        Text(
                            text = if (isAr) "تم تسليم المواد والفاتورة للزبون (توصيل فوري)" else "Items & Invoice delivered to customer",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Calculation & Totals Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tax & Discount Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmountInputField(
                            value = if (taxRate == 0.0) "" else taxRate.toString(),
                            onValueChange = { raw ->
                                val cleaned = ValidationHelper.normalizeArabicDigits(raw)
                                val parsed = cleaned.toDoubleOrNull() ?: 0.0
                                if (parsed in 0.0..100.0) {
                                    viewModel.taxRatePercent.value = parsed
                                }
                            },
                            label = if (isAr) "نسبة الضريبة %" else "Tax %",
                            allowZero = true,
                            maxAmount = 100.0,
                            isAr = isAr,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = GoldAccent,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        AmountInputField(
                            value = paidInput,
                            onValueChange = { viewModel.paidAmountInput.value = it },
                            label = if (isAr) "المبلغ المدفوع" else "Paid Amount",
                            placeholder = totalAmount.toInt().toString(),
                            allowZero = true,
                            isAr = isAr,
                            currencySymbol = currency.code,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = GoldAccent,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "المجموع قبل الضريبة:" else "Subtotal:", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text(LanguageManager.formatMoney(safeSubtotal, currency, isAr), color = Color.White, fontSize = 13.sp)
                    }

                    if (taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isAr) "الضريبة المحتسبة ($taxRate%):" else "Tax Amount:", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Text(LanguageManager.formatMoney(taxAmount, currency, isAr), color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "المبلغ الإجمالي النهائي:" else "Total Bill:", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(LanguageManager.formatMoney(totalAmount, currency, isAr), color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "المدفوع نقداً:" else "Cash Paid:", color = GreenSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(LanguageManager.formatMoney(paidAmount, currency, isAr), color = GreenSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (remainingDebt > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isAr) "المتبقي (يسجل ديناً على العميل):" else "Remaining Debt:", color = RedAlert, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(LanguageManager.formatMoney(remainingDebt, currency, isAr), color = RedAlert, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Final Action Buttons
                    Button(
                        onClick = {
                            if (cartItems.isEmpty()) {
                                Toast.makeText(context, "يرجى إضافة مواد للسلة أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (paidInput.isNotBlank()) {
                                val validation = ValidationHelper.validateAmount(paidInput, allowZero = true, isAr = isAr)
                                if (validation is ValidationResult.Invalid) {
                                    Toast.makeText(context, validation.message, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }
                            viewModel.completeInvoice { inv, items ->
                                lastCompletedInvoice = Pair(inv, items)
                                showCompletionDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = NavyPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "إتمام وحفظ الفاتورة" else "Complete & Save Invoice", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // Customer Selection Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text(if (isAr) "اختر عميلاً من القائمة" else "Select Customer") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectedCustomer.value = null
                                    showCustomerPicker = false
                                }
                        ) {
                            Text(
                                text = if (isAr) "• عميل نقدي (بدون تسجيل دين)" else "• Cash Walk-in (No Debt)",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(customers) { c ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectedCustomer.value = c
                                    showCustomerPicker = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = c.name, fontWeight = FontWeight.Bold)
                                Text(text = "هاتف: ${c.phone} | دين حالي: ${c.currentDebt.toInt()} ${currency.code}", fontSize = 11.sp, color = if (c.currentDebt > 0) RedAlert else Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            }
        )
    }

    // Invoice Completed Dialog with Direct WhatsApp & Direct Print
    if (showCompletionDialog && lastCompletedInvoice != null) {
        val (invoice, items) = lastCompletedInvoice!!
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(48.dp)) },
            title = { Text(if (isAr) "تم إصدار الفاتورة #${invoice.invoiceNumber} بنجاح!" else "Invoice #${invoice.invoiceNumber} Issued!") },
            text = {
                Column {
                    Text(
                        text = if (isAr) "تم تسجيل الفاتورة وخصم الكميات من المخزون وتحديث الحساب." else "Invoice saved, stock deducted, and accounts updated.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "العميل: ${invoice.customerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "المبلغ الإجمالي: ${invoice.totalAmount} ${currency.code}", fontSize = 13.sp)
                    if (invoice.remainingDebt > 0) {
                        Text(text = "المتبقي كدين: ${invoice.remainingDebt} ${currency.code}", color = RedAlert, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct Action Buttons
                    Button(
                        onClick = {
                            val pdfFile = PdfInvoiceExportHelper.exportInvoicePdf(
                                context = context,
                                invoice = invoice,
                                items = items,
                                currencySymbol = currency.code,
                                isArabic = isAr
                            )
                            if (pdfFile != null) {
                                PdfInvoiceExportHelper.sharePdf(context, pdfFile, "فاتورة مبيعات #${invoice.invoiceNumber}")
                            } else {
                                Toast.makeText(context, if (isAr) "فشل إنشاء ملف PDF" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_invoice_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "تصدير الفاتورة كملف PDF" else "Export as PDF Invoice")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            WhatsAppHelper.sendInvoiceNotification(
                                context = context,
                                customerPhone = invoice.customerPhone,
                                customerName = invoice.customerName,
                                invoice = invoice,
                                items = items,
                                currencySymbol = currency.code
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "إرسال الفاتورة عبر واتساب" else "Send via WhatsApp")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            DirectPrintHelper.printInvoice(
                                context = context,
                                invoice = invoice,
                                items = items,
                                currencySymbol = currency.code
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "طباعة الفاتورة مباشرة" else "Direct Print")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = {
                            DirectPrintHelper.shareInvoiceText(
                                context = context,
                                invoice = invoice,
                                items = items,
                                currencySymbol = currency.code
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "مشاركة نص الفاتورة" else "Share Slip Text")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCompletionDialog = false }) {
                    Text(if (isAr) "تم" else "Done")
                }
            }
        )
    }
}
