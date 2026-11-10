package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AmpSubscriber
import com.example.data.model.GeneratorExpense
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.WhatsAppHelper
import com.example.util.AmountInputField
import com.example.util.ValidationHelper
import com.example.util.ValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmperesScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val allSubscribers by viewModel.ampSubscribers.collectAsState()
    val generatorExpenses by viewModel.generatorExpenses.collectAsState()
    val metrics by viewModel.financialMetrics.collectAsState()

    // Top View Mode: 0: المشتركون والفواتير, 1: صرفيات المولد (كاز وزيت), 2: الأرباح الصافية والحسابات
    var selectedScreenMode by remember { mutableIntStateOf(0) }

    // Subscribers filter
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Delivered (Green), 2: Undelivered (Red)
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var showAddSubscriberDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showPaymentDialogFor by remember { mutableStateOf<AmpSubscriber?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<GeneratorExpense?>(null) }

    val filteredSubscribers = remember(allSubscribers, selectedFilterIndex, searchQuery) {
        allSubscribers.filter { sub ->
            val matchesFilter = when (selectedFilterIndex) {
                1 -> sub.isBillDelivered
                2 -> !sub.isBillDelivered
                else -> true
            }
            val matchesSearch = sub.name.contains(searchQuery, ignoreCase = true) || sub.phone.contains(searchQuery)
            matchesFilter && matchesSearch
        }
    }

    val totalAmperesSold = metrics.totalAmperesSold
    val averagePricePerAmp = metrics.averageAmpPrice
    val totalRevenue = metrics.totalAmpRevenue
    val totalExpenses = metrics.totalGeneratorExpenses
    val netProfit = metrics.generatorNetProfit
    val totalDebt = allSubscribers.sumOf { it.debtAmount }
    val deliveredCount = allSubscribers.count { it.isBillDelivered }
    val undeliveredCount = allSubscribers.count { !it.isBillDelivered }

    // Fuel & Oil calculations
    val dieselLiters = generatorExpenses.filter { it.type == "DIESEL" }.sumOf { it.quantity }
    val dieselCost = metrics.dieselExpenses
    val oilLiters = generatorExpenses.filter { it.type == "OIL" }.sumOf { it.quantity }
    val oilCost = metrics.oilExpenses
    val maintenanceCost = metrics.maintenanceExpenses

    val profitMarginPercent = if (totalRevenue > 0) ((netProfit / totalRevenue) * 100).coerceIn(-100.0, 100.0) else 0.0

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary FAB: Add Generator Expense
                SmallFloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = Slate800,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAr) "+ صرفية مولد" else "+ Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Primary FAB: Add Subscriber
                FloatingActionButton(
                    onClick = { showAddSubscriberDialog = true },
                    containerColor = SkyPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAr) "مشترك جديد" else "New Subscriber", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
            // 1. Header KPI Summary Card (Slate 900 Dark Card with Live Polish Aesthetic)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAr) "حسابات المولد والأمبيرات ⚡" else "Generator & Amperes Accounting ⚡",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (isAr) "متابعة المبيوع، التسعيرة، صرفيات الكاز والزيت، وصافي الأرباح" else "Track Amps Sold, Tariff, Fuel/Oil Expenses & Net Profit",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(SkyAccent.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = SkyAccent, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Slate700, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Primary Figures requested by user:
                        // 1. عدد الأمبيرات المبيوعة
                        // 2. التسعيرة
                        // 3. الأرباح / إجمالي الوارد
                        // 4. ربح صافي بعد خصم الكاز والزيت
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Amperes Sold
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isAr) "الأمبيرات المبيوعة" else "Amps Sold", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = "$totalAmperesSold A",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Tariff / Price per Amp
                            Column(modifier = Modifier.weight(1.1f)) {
                                Text(if (isAr) "التسعيرة" else "Tariff / Rate", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = LanguageManager.formatMoney(averagePricePerAmp, currency, isAr),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyAccent
                                )
                            }

                            // Gross Revenue (كم الأرباح/الوارد)
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(if (isAr) "إجمالي الوارد" else "Gross Revenue", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = LanguageManager.formatMoney(totalRevenue, currency, isAr),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Highlight Bar: Net Profit (ربح صافي) & Expenses
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) "صرف المولد (كاز + زيت + صيانة)" else "Generator Expenses (Fuel+Oil)",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                                Text(
                                    text = "- ${LanguageManager.formatMoney(totalExpenses, currency, isAr)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDebt
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAr) "الربح الصافي:" else "Net Profit:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate300
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", profitMarginPercent)}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netProfit >= 0) EmeraldProfit else RoseDebt
                                    )
                                }
                                Text(
                                    text = LanguageManager.formatMoney(netProfit, currency, isAr),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netProfit >= 0) EmeraldProfit else RoseDebt
                                )
                            }
                        }
                    }
                }
            }

            // 2. Mode Selector Tabs:
            // 0: المشتركون والفواتير
            // 1: صرفيات المولد (كاز وزيت)
            // 2: الأرباح الصافية والحسابات
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedScreenMode == 0,
                        onClick = { selectedScreenMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text(
                            text = if (isAr) "المشتركون (${allSubscribers.size})" else "Subscribers (${allSubscribers.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedScreenMode == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = selectedScreenMode == 1,
                        onClick = { selectedScreenMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text(
                            text = if (isAr) "صرفيات المولد ⛽" else "Expenses ⛽",
                            fontSize = 11.sp,
                            fontWeight = if (selectedScreenMode == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = selectedScreenMode == 2,
                        onClick = { selectedScreenMode = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text(
                            text = if (isAr) "الأرباح الصافية 📊" else "Net Profits 📊",
                            fontSize = 11.sp,
                            fontWeight = if (selectedScreenMode == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // MODE 0: SUBSCRIBERS & BILLS
            if (selectedScreenMode == 0) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (isAr) "ابحث عن مشترك بالاسم أو رقم الهاتف..." else "Search subscriber by name or phone...", color = Slate400, fontSize = 13.sp) },
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

                // Delivery Status Filter Buttons (Green & Red Circles)
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedFilterIndex == 0,
                            onClick = { selectedFilterIndex = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text(if (isAr) "الكل (${allSubscribers.size})" else "All (${allSubscribers.size})", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = selectedFilterIndex == 1,
                            onClick = { selectedFilterIndex = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(StatusGreen, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "تم التوصيل ($deliveredCount)" else "Delivered ($deliveredCount)", fontSize = 12.sp)
                            }
                        }
                        SegmentedButton(
                            selected = selectedFilterIndex == 2,
                            onClick = { selectedFilterIndex = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(StatusRed, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "متبقي ($undeliveredCount)" else "Pending ($undeliveredCount)", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Subscribers List
                if (filteredSubscribers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isAr) "لا يوجد مشتركين في هذه الفئة حالياً" else "No subscribers found in this category",
                                    color = Slate400,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSubscribers) { subscriber ->
                        val isDelivered = subscriber.isBillDelivered
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isDelivered) Slate200 else RoseDebt.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Status Circle Indicator (Green if delivered, Red if not delivered)
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(
                                                    if (isDelivered) StatusGreenBg else StatusRedBg,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isDelivered) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (isDelivered) StatusGreen else StatusRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = subscriber.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Slate800
                                            )
                                            if (subscriber.meterNumberOrLocation.isNotBlank()) {
                                                Text(
                                                    text = subscriber.meterNumberOrLocation,
                                                    fontSize = 11.sp,
                                                    color = Slate400
                                                )
                                            }
                                        }
                                    }

                                    // Delivery Status Toggle Pill
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isDelivered) StatusGreenBg else StatusRedBg,
                                        modifier = Modifier.clickable {
                                            viewModel.toggleAmpDeliveryStatus(subscriber.id, subscriber.isBillDelivered)
                                            val msg = if (!isDelivered) {
                                                if (isAr) "تم تسجيل تسليم الفاتورة بنجاح 🟢" else "Bill marked delivered 🟢"
                                            } else {
                                                if (isAr) "تم إلغاء حالة التسليم 🔴" else "Bill marked pending 🔴"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (isDelivered) StatusGreen else StatusRed, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isDelivered) {
                                                    if (isAr) "تم التوصيل" else "Delivered"
                                                } else {
                                                    if (isAr) "لم يتم التوصيل" else "Pending"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDelivered) StatusGreen else StatusRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Slate100, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Details Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(if (isAr) "سحب الأمبيرات" else "Amps", fontSize = 10.sp, color = Slate400)
                                        Text("${subscriber.amperes} A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                    }
                                    Column {
                                        Text(if (isAr) "السعر / أمبير" else "Rate", fontSize = 10.sp, color = Slate400)
                                        Text(LanguageManager.formatMoney(subscriber.pricePerAmpere, currency, isAr), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                                    }
                                    Column {
                                        Text(if (isAr) "المبلغ الشهري" else "Monthly", fontSize = 10.sp, color = Slate400)
                                        Text(LanguageManager.formatMoney(subscriber.monthlyTotal, currency, isAr), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyPrimary)
                                    }
                                    Column {
                                        Text(if (isAr) "الديون السابقة" else "Debts", fontSize = 10.sp, color = Slate400)
                                        Text(
                                            text = LanguageManager.formatMoney(subscriber.debtAmount, currency, isAr),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (subscriber.debtAmount > 0) RoseDebt else EmeraldProfit
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Settle Debt / Pay
                                    if (subscriber.debtAmount > 0) {
                                        TextButton(
                                            onClick = {
                                                showPaymentDialogFor = subscriber
                                                paymentAmountInput = subscriber.debtAmount.toLong().toString()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldProfit)
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAr) "تسديد دين" else "Pay Debt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    // WhatsApp Invoice / Debt Notice
                                    IconButton(
                                        onClick = {
                                            val message = if (subscriber.debtAmount > 0) {
                                                "مرحباً أستاذ ${subscriber.name}، يرجى العلم بأن مستحقات اشتراك الأمبيرات (${subscriber.amperes} أمبير) لشهرنا الحالي تبلغ ${subscriber.monthlyTotal} ${currency.code}، ومجموع الديون السابقة المتبقية بذمتكم هي ${subscriber.debtAmount} ${currency.code}. شاكرين تعاونكم معنا."
                                            } else {
                                                "مرحباً أستاذ ${subscriber.name}، نود إعلامكم بصدور فاتورة اشتراك الأمبيرات (${subscriber.amperes} أمبير) بمبلغ ${subscriber.monthlyTotal} ${currency.code}. حسابكم بريء من أي ديون سابقة. شكراً لاشتراككم."
                                            }
                                            WhatsAppHelper.sendDirectMessage(context, subscriber.phone, message)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "واتساب", tint = StatusGreen)
                                    }

                                    // Call Subscriber
                                    if (subscriber.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${subscriber.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = SkyPrimary)
                                        }
                                    }

                                    // Delete Subscriber
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteAmpSubscriber(subscriber)
                                            Toast.makeText(context, if (isAr) "تم حذف المشترك" else "Deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Slate400)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MODE 1: GENERATOR EXPENSES (صرفيات المولد من الزيت والكاز وغيرها)
            if (selectedScreenMode == 1) {
                // 3 Top KPI Cards for Expenses
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Diesel (الكاز)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "الكاز (ديزل)" else "Diesel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = LanguageManager.formatMoney(dieselCost, currency, isAr),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDebt
                                )
                                Text(
                                    text = "${String.format("%.1f", dieselLiters)} لتر",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                        }

                        // Engine Oil (زيت المحرك)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = SkyAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "زيت المحرك" else "Engine Oil", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = LanguageManager.formatMoney(oilCost, currency, isAr),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDebt
                                )
                                Text(
                                    text = "${String.format("%.1f", oilLiters)} لتر",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                        }

                        // Maintenance & Others (صيانة وفلاتر)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "صيانة وأخرى" else "Maintenance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = LanguageManager.formatMoney(maintenanceCost, currency, isAr),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDebt
                                )
                                Text(
                                    text = if (isAr) "فلاتر وقطع" else "Filters/Parts",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }

                // Add Expense Fast Button
                item {
                    Button(
                        onClick = { showAddExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "تسجيل صرفية جديدة للمولد (كاز / زيت / صيانة)" else "Record New Generator Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Expenses List
                if (generatorExpenses.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isAr) "لا توجد صرفيات مسجلة للمولد حتى الآن" else "No generator expenses recorded yet",
                                    color = Slate400,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(generatorExpenses) { expense ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Slate200),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Expense Icon Badge
                                    val (badgeBg, iconColor, icon) = when (expense.type) {
                                        "DIESEL" -> Triple(SkyLight, SkyPrimary, Icons.Default.LocalGasStation)
                                        "OIL" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.WaterDrop)
                                        "MAINTENANCE" -> Triple(Color(0xFFF1F5F9), Slate700, Icons.Default.Build)
                                        else -> Triple(Color(0xFFF1F5F9), Slate600, Icons.Default.ReceiptLong)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(badgeBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = expense.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate800
                                        )
                                        if (expense.quantity > 0) {
                                            Text(
                                                text = "الكمية: ${expense.quantity} ${expense.unit}" +
                                                    if (expense.notes.isNotBlank()) " • ${expense.notes}" else "",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        } else if (expense.notes.isNotBlank()) {
                                            Text(text = expense.notes, fontSize = 11.sp, color = Slate500)
                                        }
                                        Text(
                                            text = LanguageManager.formatDateShort(expense.createdAt, isAr),
                                            fontSize = 10.sp,
                                            color = Slate400
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = LanguageManager.formatMoney(expense.amount, currency, isAr),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = RoseDebt
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { expenseToDelete = expense },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Slate400)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MODE 2: NET PROFITS & FINANCIAL STATEMENT (الحسابات والأرباح الصافية)
            if (selectedScreenMode == 2) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Slate200),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isAr) "كشف صافي أرباح المولد 📊" else "Generator Net Profit Statement 📊",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Slate800
                                    )
                                    Text(
                                        text = if (isAr) "حسابات الإيرادات مقابل صرف الوقود والزيت والصيانة" else "Revenues vs Fuel, Oil & Maintenance Expenses",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(EmeraldProfit.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Slate200, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 1. Total Amperes Revenue
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(if (isAr) "(+) إجمالي وارد الأمبيرات المبيوعة" else "(+) Total Amperes Revenue", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                                    Text("$totalAmperesSold أمبير مباع × ${LanguageManager.formatMoney(averagePricePerAmp, currency, isAr)}", fontSize = 10.sp, color = Slate400)
                                }
                                Text(
                                    text = LanguageManager.formatMoney(totalRevenue, currency, isAr),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldProfit
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. Diesel Cost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(if (isAr) "(-) تكلفة صرف الكاز (الديزل)" else "(-) Diesel Fuel Expenses", fontSize = 12.sp, color = Slate700)
                                    Text("مجموع الكمية المستهلكة: $dieselLiters لتر", fontSize = 10.sp, color = Slate400)
                                }
                                Text(
                                    text = "- ${LanguageManager.formatMoney(dieselCost, currency, isAr)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoseDebt
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Oil Cost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(if (isAr) "(-) تكلفة زيت المحرك (الدهن)" else "(-) Engine Oil Expenses", fontSize = 12.sp, color = Slate700)
                                    Text("مجموع الكمية: $oilLiters لتر", fontSize = 10.sp, color = Slate400)
                                }
                                Text(
                                    text = "- ${LanguageManager.formatMoney(oilCost, currency, isAr)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoseDebt
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. Maintenance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isAr) "(-) مصاريف الصيانة والفلاتر وقطع الغيار" else "(-) Maintenance & Filters", fontSize = 12.sp, color = Slate700)
                                Text(
                                    text = "- ${LanguageManager.formatMoney(maintenanceCost, currency, isAr)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoseDebt
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Slate200, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Net Profit Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (netProfit >= 0) StatusGreenBg else StatusRedBg
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (netProfit >= 0) StatusGreen.copy(alpha = 0.3f) else StatusRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isAr) "(=) صافي الربح الحقيقي للمولد" else "(=) Net Profit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (netProfit >= 0) StatusGreen else StatusRed
                                        )
                                        Text(
                                            text = "نسبة هامش الربح الصافي: ${String.format("%.1f", profitMarginPercent)}%",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                    Text(
                                        text = LanguageManager.formatMoney(netProfit, currency, isAr),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (netProfit >= 0) StatusGreen else StatusRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Share Summary via WhatsApp Button
                            Button(
                                onClick = {
                                    val summaryMsg = """
                                        ⚡ تقرير أرباح وصرفيات المولد ⚡
                                        • عدد الأمبيرات المبيوعة: $totalAmperesSold أمبير
                                        • تسعيرة الأمبير: ${LanguageManager.formatMoney(averagePricePerAmp, currency, isAr)}
                                        • إجمالي الوارد الشهري: ${LanguageManager.formatMoney(totalRevenue, currency, isAr)}
                                        ------------------------
                                        • صرفيات الكاز (الوقود): ${LanguageManager.formatMoney(dieselCost, currency, isAr)} ($dieselLiters لتر)
                                        • صرفيات زيت المحرك: ${LanguageManager.formatMoney(oilCost, currency, isAr)} ($oilLiters لتر)
                                        • الصيانة والفلاتر: ${LanguageManager.formatMoney(maintenanceCost, currency, isAr)}
                                        • إجمالي الصرفيات: ${LanguageManager.formatMoney(totalExpenses, currency, isAr)}
                                        ------------------------
                                        💰 صافي الربح الحقيقي: ${LanguageManager.formatMoney(netProfit, currency, isAr)}
                                        📊 هامش الربح: ${String.format("%.1f", profitMarginPercent)}%
                                    """.trimIndent()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, summaryMsg)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير المولد"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isAr) "مشاركة كشف حساب المولد وصافي الربح" else "Share Profit Statement", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: Add New Amp Subscriber
    if (showAddSubscriberDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var amps by remember { mutableStateOf("5") }
        var pricePerAmp by remember { mutableStateOf("15000") }
        var initialDebt by remember { mutableStateOf("0") }
        var location by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSubscriberDialog = false },
            title = { Text(if (isAr) "إضافة مشترك أمبيرات جديد ⚡" else "Add New Amp Subscriber ⚡") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isAr) "اسم المشترك" else "Subscriber Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isAr) "رقم الهاتف (لإرسال الواتساب)" else "Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountInputField(
                            value = amps,
                            onValueChange = { amps = it },
                            label = if (isAr) "كم أمبير يسحب؟" else "Amperes",
                            allowZero = false,
                            maxAmount = 1000.0,
                            isAr = isAr,
                            modifier = Modifier.weight(1f)
                        )
                        AmountInputField(
                            value = pricePerAmp,
                            onValueChange = { pricePerAmp = it },
                            label = if (isAr) "تسعيرة الأمبير" else "Rate/Amp",
                            allowZero = false,
                            isAr = isAr,
                            currencySymbol = currency.code,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AmountInputField(
                        value = initialDebt,
                        onValueChange = { initialDebt = it },
                        label = if (isAr) "ديون سابقة إن وجدت" else "Initial Debt",
                        allowZero = true,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(if (isAr) "رقم الجوزة أو موقع السحب" else "Breaker No. / Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم المشترك", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val ampsVal = ValidationHelper.validateAmount(amps, allowZero = false, isAr = isAr)
                        if (ampsVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "الأمبيرات: ${ampsVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val priceVal = ValidationHelper.validateAmount(pricePerAmp, allowZero = false, isAr = isAr)
                        if (priceVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "سعر الأمبير: ${priceVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val debtVal = ValidationHelper.validateAmount(initialDebt, allowZero = true, isAr = isAr)
                        if (debtVal is ValidationResult.Invalid) {
                            Toast.makeText(context, "الدين السابق: ${debtVal.message}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val a = (ampsVal as ValidationResult.Valid).value
                        val p = (priceVal as ValidationResult.Valid).value
                        val d = (debtVal as ValidationResult.Valid).value

                        viewModel.addAmpSubscriber(
                            AmpSubscriber(
                                name = name,
                                phone = phone,
                                amperes = a,
                                pricePerAmpere = p,
                                monthlyTotal = a * p,
                                debtAmount = d,
                                isBillDelivered = false,
                                meterNumberOrLocation = location
                            )
                        )
                        showAddSubscriberDialog = false
                        Toast.makeText(context, "تمت إضافة المشترك بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "حفظ المشترك" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubscriberDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // DIALOG 2: Add Generator Expense (صرف المولد: كاز، زيت، صيانة)
    if (showAddExpenseDialog) {
        var expenseType by remember { mutableStateOf("DIESEL") }
        var titleInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var quantityInput by remember { mutableStateOf("") }
        var unitInput by remember { mutableStateOf("لتر") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text(if (isAr) "تسجيل صرفية مولد ⛽" else "Record Generator Expense ⛽") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Type selector
                    Text(if (isAr) "نوع الصرفية:" else "Expense Type:", fontSize = 12.sp, color = Slate600)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = expenseType == "DIESEL",
                            onClick = {
                                expenseType = "DIESEL"
                                unitInput = "لتر"
                                if (titleInput.isBlank()) titleInput = "تعبئة كاز (ديزل)"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text(if (isAr) "كاز" else "Diesel", fontSize = 11.sp)
                        }
                        SegmentedButton(
                            selected = expenseType == "OIL",
                            onClick = {
                                expenseType = "OIL"
                                unitInput = "لتر"
                                if (titleInput.isBlank()) titleInput = "تبديل زيت المحرك"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text(if (isAr) "زيت" else "Oil", fontSize = 11.sp)
                        }
                        SegmentedButton(
                            selected = expenseType == "MAINTENANCE",
                            onClick = {
                                expenseType = "MAINTENANCE"
                                unitInput = "قطعة"
                                if (titleInput.isBlank()) titleInput = "صيانة وفلاتر"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text(if (isAr) "صيانة" else "Repair", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text(if (isAr) "بيان الصرفية / الوصف" else "Description") },
                        singleLine = true,
                        placeholder = {
                            Text(
                                when (expenseType) {
                                    "DIESEL" -> "تعبئة كاز للديزل"
                                    "OIL" -> "تبديل زيت المحرك وفلتر"
                                    else -> "تبديل قايش أو فلاتر أو تصليح"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AmountInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = if (isAr) "المبلغ الكلي (${currency.code})" else "Total Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountInputField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = if (isAr) "الكمية (باللتر / العدد)" else "Quantity",
                            allowZero = true,
                            isAr = isAr,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = unitInput,
                            onValueChange = { unitInput = it },
                            label = { Text(if (isAr) "الوحدة" else "Unit") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(if (isAr) "ملاحظات إضافية (سعر اللتر، اسم المحطة)" else "Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = ValidationHelper.validateAmount(amountInput, allowZero = false, isAr = isAr)
                        if (amountVal is ValidationResult.Invalid) {
                            Toast.makeText(context, amountVal.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val qtyVal = ValidationHelper.validateAmount(quantityInput, allowZero = true, isAr = isAr)
                        if (qtyVal is ValidationResult.Invalid) {
                            Toast.makeText(context, qtyVal.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val amount = (amountVal as ValidationResult.Valid).value
                        val qty = (qtyVal as ValidationResult.Valid).value

                        viewModel.addGeneratorExpense(
                            type = expenseType,
                            title = titleInput,
                            amount = amount,
                            quantity = qty,
                            unit = unitInput,
                            notes = notesInput
                        )
                        showAddExpenseDialog = false
                        Toast.makeText(context, "تم حفظ صرفية المولد بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "حفظ الصرفية" else "Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // DIALOG 3: Payment Dialog for Subscriber
    if (showPaymentDialogFor != null) {
        val subscriber = showPaymentDialogFor!!
        AlertDialog(
            onDismissRequest = { showPaymentDialogFor = null },
            title = { Text(if (isAr) "تسديد دفعة / دين: ${subscriber.name}" else "Record Payment") },
            text = {
                Column {
                    Text(
                        text = "الديون الحالية: ${subscriber.debtAmount} ${currency.code}",
                        color = RoseDebt,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AmountInputField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = if (isAr) "المبلغ المسدد" else "Payment Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = ValidationHelper.validateAmount(paymentAmountInput, allowZero = false, isAr = isAr)
                        if (amountVal is ValidationResult.Invalid) {
                            Toast.makeText(context, amountVal.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amount = (amountVal as ValidationResult.Valid).value
                        if (amount > 0) {
                            viewModel.recordAmpPayment(subscriber.id, amount)
                            Toast.makeText(context, "تم تسديد $amount بنجاح", Toast.LENGTH_SHORT).show()
                            showPaymentDialogFor = null
                        }
                    }
                ) {
                    Text(if (isAr) "تأكيد التسديد" else "Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialogFor = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // DIALOG 4: Delete Expense Confirmation
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(if (isAr) "تأكيد حذف الصرفية" else "Confirm Delete") },
            text = {
                Text(
                    text = if (isAr) {
                        "هل أنت متأكد من حذف صرفية \"${exp.title}\" بمبلغ ${exp.amount} ${currency.code}؟ سيتم تحديث صافي الربح تلقائياً."
                    } else {
                        "Are you sure you want to delete \"${exp.title}\"? Net profit will be updated automatically."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGeneratorExpense(exp)
                        expenseToDelete = null
                        Toast.makeText(context, if (isAr) "تم الحذف" else "Deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDebt)
                ) {
                    Text(if (isAr) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
