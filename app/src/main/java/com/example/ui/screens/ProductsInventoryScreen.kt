package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppCurrency
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.AmountInputField
import com.example.util.ValidationHelper
import com.example.util.ValidationResult
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsInventoryScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val products by viewModel.products.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    var showCameraScanner by remember { mutableStateOf(false) }
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var showStockUpdateDialog by remember { mutableStateOf(false) }
    var newStockInput by remember { mutableStateOf("") }

    val displayedProducts = remember(products, searchQuery, filterLowStockOnly) {
        products.filter { p ->
            val matchesFilter = !filterLowStockOnly || (p.stockQuantity <= p.minStockAlert)
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) || p.barcode.contains(searchQuery)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = SkyPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة منتج جديد")
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
            // Inventory Status Card
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
                                text = if (isAr) "إجمالي الأصناف بالمخزون" else "Total Inventory Items",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${products.size} صنف",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Low stock alert counter badge
                            FilterChip(
                                selected = filterLowStockOnly,
                                onClick = { filterLowStockOnly = !filterLowStockOnly },
                                label = { Text(if (isAr) "نواقص المخزون (${lowStockProducts.size})" else "Low Stock (${lowStockProducts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (lowStockProducts.isNotEmpty()) RoseDebt else EmeraldProfit,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoseBg,
                                    selectedLabelColor = RoseDebt,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                )
                            )

                            // On-demand notification check trigger
                            IconButton(
                                onClick = {
                                    com.example.worker.InventoryCheckWorker.runImmediateInventoryCheck(context)
                                    Toast.makeText(
                                        context,
                                        if (isAr) "تم فحص المخزون وإرسال التنبيه إن وجدت نواقص" else "Inventory check completed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = "فحص النواقص الآن",
                                    tint = SkyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search by barcode or name with Camera QR Scanner Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (isAr) "ابحث بالاسم أو رقم الباركود..." else "Search by name or barcode...", color = Slate400, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Slate400, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = SkyPrimary,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Dedicated Camera QR Scanner Button
                    FilledTonalIconButton(
                        onClick = { showCameraScanner = !showCameraScanner },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (showCameraScanner) SkyPrimary else Slate100,
                            contentColor = if (showCameraScanner) Color.White else SkyPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = if (isAr) "مسح باركود بالكاميرا" else "Scan QR/Barcode",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Interactive Camera Viewfinder & QR Scanner Card
            if (showCameraScanner) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isAr) "ماسح الباركود و QR الحي" else "Live Camera QR/Barcode Scanner",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                IconButton(
                                    onClick = { showCameraScanner = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Slate400, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Camera Viewfinder Box with Laser Guide
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black, RoundedCornerShape(14.dp))
                                    .border(BorderStroke(2.dp, SkyPrimary), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = SkyPrimary.copy(alpha = 0.85f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isAr) "وجّه الكاميرا إلى باركود المنتج للتعرف الفوري" else "Align QR / Barcode within frame to scan",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Simulated Laser Line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(2.dp)
                                            .background(RoseDebt)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isAr) "اختر أو انقر على رمز باركود للتجربة السريعة:" else "Tap a quick barcode test chip:",
                                color = Slate400,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick barcode chips from inventory items
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(products.take(6)) { prod ->
                                    ElevatedFilterChip(
                                        selected = false,
                                        onClick = {
                                            searchQuery = prod.barcode
                                            scannedProduct = prod
                                            newStockInput = prod.stockQuantity.toInt().toString()
                                            showStockUpdateDialog = true
                                            Toast.makeText(
                                                context,
                                                if (isAr) "تم التعرف على: ${prod.name}" else "Identified: ${prod.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        label = {
                                            Text(
                                                text = "${prod.name} (${prod.barcode})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            containerColor = Slate800,
                                            labelColor = Color.White,
                                            iconColor = EmeraldProfit
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Products list
            if (displayedProducts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = if (isAr) "لا توجد منتجات مطابقة للبحث" else "No matching products found", color = Slate400)
                        }
                    }
                }
            } else {
                items(displayedProducts) { product ->
                    val isLowStock = product.stockQuantity <= product.minStockAlert
                    val profitMargin = if (product.buyPrice > 0) {
                        ((product.sellPrice - product.buyPrice) / product.buyPrice * 100).toInt()
                    } else 0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, if (isLowStock) RoseDebt.copy(alpha = 0.4f) else Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate400)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = product.barcode.ifBlank { "بدون باركود" }, fontSize = 11.sp, color = Slate500)
                                    }
                                }

                                Surface(
                                    color = if (isLowStock) StatusRedBg else StatusGreenBg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${product.stockQuantity.toInt()} ${product.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isLowStock) StatusRed else StatusGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Pricing & Profit Margin with Product Specific Currency
                            val prodCurrency = if (product.currency == "USD") AppCurrency.USD else AppCurrency.IQD
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(if (isAr) "سعر الشراء:" else "Buy Price:", fontSize = 11.sp, color = Color.Gray)
                                    Text(LanguageManager.formatMoney(product.buyPrice, prodCurrency, isAr), fontSize = 12.sp)
                                }
                                Column {
                                    Text(if (isAr) "سعر البيع:" else "Sell Price:", fontSize = 11.sp, color = Color.Gray)
                                    Text(LanguageManager.formatMoney(product.sellPrice, prodCurrency, isAr), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text(if (isAr) "هامش الربح:" else "Margin:", fontSize = 11.sp, color = Color.Gray)
                                    Text("+$profitMargin%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                }
                            }

                            if (isLowStock) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⚠️ تنبيه انخفاض المخزون: الحد الأدنى المطلوب (${product.minStockAlert.toInt()})",
                                    color = RedAlert,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isAr) "تعديل الكمية:" else "Adjust Qty:", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    FilledTonalIconButton(
                                        onClick = {
                                            if (product.stockQuantity > 0) {
                                                viewModel.updateProduct(product.copy(stockQuantity = product.stockQuantity - 1))
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "نقص", modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    FilledTonalIconButton(
                                        onClick = {
                                            viewModel.updateProduct(product.copy(stockQuantity = product.stockQuantity + 1))
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(14.dp))
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { productToEdit = product },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = AmberAccent)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteProduct(product)
                                            Toast.makeText(context, "تم حذف المنتج", Toast.LENGTH_SHORT).show()
                                        },
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
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog || productToEdit != null) {
        val isEditing = productToEdit != null
        var name by remember { mutableStateOf(productToEdit?.name ?: "") }
        var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "") }
        var itemCurrency by remember { mutableStateOf(productToEdit?.currency ?: currency.code) }
        var buyPrice by remember { mutableStateOf(productToEdit?.buyPrice?.toInt()?.toString() ?: "") }
        var sellPrice by remember { mutableStateOf(productToEdit?.sellPrice?.toInt()?.toString() ?: "") }
        var quantity by remember { mutableStateOf(productToEdit?.stockQuantity?.toInt()?.toString() ?: "10") }
        var minAlert by remember { mutableStateOf(productToEdit?.minStockAlert?.toInt()?.toString() ?: "5") }
        var category by remember { mutableStateOf(productToEdit?.category ?: "عام") }
        var unit by remember { mutableStateOf(productToEdit?.unit ?: "قطعة") }

        AlertDialog(
            onDismissRequest = {
                showAddProductDialog = false
                productToEdit = null
            },
            title = { Text(if (isEditing) (if (isAr) "تعديل بيانات المنتج" else "Edit Product") else (if (isAr) "إضافة منتج جديد للمخزن" else "Add New Product")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isAr) "اسم المنتج أو المادة" else "Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text(if (isAr) "الباركود" else "Barcode") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = {
                                barcode = "69" + (10000000 + Random.nextInt(89999999)).toString()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(if (isAr) "توليد" else "Gen", fontSize = 11.sp)
                        }
                    }

                    // Currency Selection for Product Pricing
                    Text(text = if (isAr) "عملة تسعير المنتج:" else "Item Pricing Currency:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "IQD" to if (isAr) "دينار عراقي (IQD)" else "Iraqi Dinar (IQD)",
                            "USD" to if (isAr) "دولار أمريكي (USD)" else "US Dollar ($)"
                        ).forEach { (currCode, label) ->
                            FilterChip(
                                selected = itemCurrency == currCode,
                                onClick = { itemCurrency = currCode },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountInputField(
                            value = buyPrice,
                            onValueChange = { buyPrice = it },
                            label = if (isAr) "سعر الشراء" else "Buy Price",
                            allowZero = true,
                            isAr = isAr,
                            currencySymbol = itemCurrency,
                            modifier = Modifier.weight(1f)
                        )
                        AmountInputField(
                            value = sellPrice,
                            onValueChange = { sellPrice = it },
                            label = if (isAr) "سعر البيع" else "Sell Price",
                            allowZero = false,
                            isAr = isAr,
                            currencySymbol = itemCurrency,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountInputField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = if (isAr) "الكمية الحالية" else "Current Stock",
                            allowZero = true,
                            isAr = isAr,
                            modifier = Modifier.weight(1f)
                        )
                        AmountInputField(
                            value = minAlert,
                            onValueChange = { minAlert = it },
                            label = if (isAr) "حد تنبيه النقص" else "Alert Limit",
                            allowZero = true,
                            isAr = isAr,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text(if (isAr) "التصنيف" else "Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(if (isAr) "الوحدة (قطعة/كغم)" else "Unit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم المنتج", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val buyVal = ValidationHelper.validateAmount(buyPrice, allowZero = true, isAr = isAr)
                        if (buyVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "سعر الشراء: ${buyVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val sellVal = ValidationHelper.validateAmount(sellPrice, allowZero = false, isAr = isAr)
                        if (sellVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "سعر البيع: ${sellVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val qtyVal = ValidationHelper.validateAmount(quantity, allowZero = true, isAr = isAr)
                        if (qtyVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "الكمية: ${qtyVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val alertVal = ValidationHelper.validateAmount(minAlert, allowZero = true, isAr = isAr)
                        if (alertVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "حد التنبيه: ${alertVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val buy = (buyVal as ValidationResult.Valid).value
                        val sell = (sellVal as ValidationResult.Valid).value
                        val qty = (qtyVal as ValidationResult.Valid).value
                        val alert = (alertVal as ValidationResult.Valid).value

                        if (isEditing) {
                            val updated = productToEdit!!.copy(
                                name = name,
                                barcode = barcode,
                                buyPrice = buy,
                                sellPrice = sell,
                                currency = itemCurrency,
                                stockQuantity = qty,
                                minStockAlert = alert,
                                category = category,
                                unit = unit
                            )
                            viewModel.updateProduct(updated)
                            Toast.makeText(context, "تم تعديل المنتج بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addProduct(
                                Product(
                                    name = name,
                                    barcode = barcode.ifBlank { "BAR-${System.currentTimeMillis().toString().takeLast(6)}" },
                                    buyPrice = buy,
                                    sellPrice = sell,
                                    currency = itemCurrency,
                                    stockQuantity = qty,
                                    minStockAlert = alert,
                                    category = category,
                                    unit = unit
                                )
                            )
                            Toast.makeText(context, "تمت إضافة المنتج للمخزون", Toast.LENGTH_SHORT).show()
                        }
                        showAddProductDialog = false
                        productToEdit = null
                    }
                ) {
                    Text(if (isAr) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddProductDialog = false
                    productToEdit = null
                }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Quick Stock Adjustment Dialog for Scanned Product
    if (showStockUpdateDialog && scannedProduct != null) {
        val prod = scannedProduct!!
        AlertDialog(
            onDismissRequest = {
                showStockUpdateDialog = false
                scannedProduct = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = SkyPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isAr) "تحديث كمية المخزون للمنتج الممسوح" else "Update Scanned Item Stock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "الباركود: ${prod.barcode}", fontSize = 12.sp, color = Slate600)
                            Text(text = "الرصيد الحالي: ${prod.stockQuantity.toInt()} ${prod.unit}", fontSize = 12.sp, color = SkyPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedTextField(
                        value = newStockInput,
                        onValueChange = { newStockInput = it },
                        label = { Text(if (isAr) "الرصيد المخزني الجديد" else "New Stock Quantity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val current = newStockInput.toDoubleOrNull() ?: prod.stockQuantity
                                newStockInput = (current + 1).toInt().toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1")
                        }

                        Button(
                            onClick = {
                                val current = newStockInput.toDoubleOrNull() ?: prod.stockQuantity
                                if (current > 0) {
                                    newStockInput = (current - 1).toInt().toString()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("-1")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qtyVal = newStockInput.toDoubleOrNull()
                        if (qtyVal == null || qtyVal < 0) {
                            Toast.makeText(context, if (isAr) "يرجى إدخال كمية صحيحة" else "Invalid quantity", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val updated = prod.copy(stockQuantity = qtyVal)
                        viewModel.updateProduct(updated)
                        Toast.makeText(
                            context,
                            if (isAr) "تم تحديث رصيد ${prod.name} إلى $qtyVal ${prod.unit}" else "Stock updated to $qtyVal",
                            Toast.LENGTH_SHORT
                        ).show()
                        showStockUpdateDialog = false
                        scannedProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                ) {
                    Text(if (isAr) "تحديث المخزون" else "Update Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStockUpdateDialog = false
                    scannedProduct = null
                }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
