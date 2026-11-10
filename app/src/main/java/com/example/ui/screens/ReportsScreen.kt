package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.ui.components.BarChartItem
import com.example.ui.components.DonutSlice
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.StockHealthMeter
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppLanguage
import com.example.util.BackupExportHelper
import com.example.util.LanguageManager
import com.example.util.WhatsAppHelper
import java.util.Calendar

enum class ReportDateRange(val labelAr: String, val labelEn: String) {
    TODAY("اليوم", "Today"),
    LAST_7_DAYS("آخر 7 أيام", "Last 7 Days"),
    THIS_MONTH("هذا الشهر", "This Month"),
    LAST_30_DAYS("آخر 30 يوم", "Last 30 Days"),
    ALL_TIME("الكل", "All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val invoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val generatorExpenses by viewModel.generatorExpenses.collectAsState()
    val ampSubscribers by viewModel.ampSubscribers.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // Filters State
    var selectedDateRange by remember { mutableStateOf(ReportDateRange.THIS_MONTH) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Profits & Sales, 1: Expenses, 2: Inventory, 3: Cash Flow

    // Collect available categories from products and expenses
    val productCategories = remember(products) {
        products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    // Filtered data by Date Range
    val now = System.currentTimeMillis()
    val startFilterMillis = remember(selectedDateRange) {
        val cal = Calendar.getInstance()
        when (selectedDateRange) {
            ReportDateRange.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            ReportDateRange.LAST_7_DAYS -> now - (7L * 24 * 3600 * 1000)
            ReportDateRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReportDateRange.LAST_30_DAYS -> now - (30L * 24 * 3600 * 1000)
            ReportDateRange.ALL_TIME -> 0L
        }
    }

    // Filtered Invoices
    val filteredInvoices = remember(invoices, startFilterMillis, selectedCategoryFilter) {
        invoices.filter { inv ->
            inv.createdAt >= startFilterMillis
        }
    }

    // Filtered Generator Expenses
    val filteredExpenses = remember(generatorExpenses, startFilterMillis, selectedCategoryFilter) {
        generatorExpenses.filter { exp ->
            val matchesDate = exp.createdAt >= startFilterMillis
            val matchesCat = if (selectedCategoryFilter == "ALL") true else {
                when (selectedCategoryFilter) {
                    "وقود وديزل" -> exp.type == "DIESEL"
                    "زيوت ومحركات" -> exp.type == "OIL"
                    "صيانة وفلاتر" -> exp.type == "MAINTENANCE"
                    else -> true
                }
            }
            matchesDate && matchesCat
        }
    }

    // Filtered Products for Inventory
    val filteredProducts = remember(products, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") products
        else products.filter { it.category == selectedCategoryFilter }
    }

    // Computed Figures for the selected period & category
    val totalPeriodSales = filteredInvoices.sumOf { it.totalAmount }
    val totalPeriodProfits = filteredInvoices.sumOf { it.totalAmount * 0.25 } // estimated gross profit
    val totalPeriodExpenses = filteredExpenses.sumOf { it.amount }
    val netPeriodProfit = totalPeriodProfits - totalPeriodExpenses
    val profitMarginPercent = if (totalPeriodSales > 0) ((totalPeriodProfits / totalPeriodSales) * 100).toInt() else 0

    // Inventory metrics
    val totalStockBuyValue = filteredProducts.sumOf { it.stockQuantity * it.buyPrice }
    val totalStockSellValue = filteredProducts.sumOf { it.stockQuantity * it.sellPrice }
    val potentialStockProfit = totalStockSellValue - totalStockBuyValue
    val lowStockCount = filteredProducts.count { it.stockQuantity in 0.1..it.minStockAlert }
    val outOfStockCount = filteredProducts.count { it.stockQuantity <= 0 }
    val adequateStockCount = filteredProducts.count { it.stockQuantity > it.minStockAlert }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // 1. Header Banner & Quick Actions (Excel & WhatsApp Export)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(20.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isAr) "لوحة التقارير والتحليلات المالية" else "Financial Intelligence",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(StatusGreen, CircleShape)
                                )
                            }
                            Text(
                                text = if (isAr) "مؤشرات الأرباح، المصروفات، وحالة المخزون" else "Interactive Profits, Expenses & Inventory",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // WhatsApp Summary Share
                            FilledTonalButton(
                                onClick = {
                                    val summaryMsg = """
                                        📊 تقرير مالي ذكي - ليدجر Ledger
                                        الفترة: ${if (isAr) selectedDateRange.labelAr else selectedDateRange.labelEn}
                                        ──────────────────
                                        💰 إجمالي المبيعات: ${LanguageManager.formatMoney(totalPeriodSales, currency, isAr)}
                                        📈 صافي الأرباح: ${LanguageManager.formatMoney(totalPeriodProfits, currency, isAr)}
                                        📉 إجمالي المصروفات: ${LanguageManager.formatMoney(totalPeriodExpenses, currency, isAr)}
                                        💵 صافي الدخل: ${LanguageManager.formatMoney(netPeriodProfit, currency, isAr)}
                                        📦 قيمة المخزون الحالي: ${LanguageManager.formatMoney(totalStockBuyValue, currency, isAr)}
                                        ──────────────────
                                        تم الإصدار عبر تطبيق ليدجر المحاسبي
                                    """.trimIndent()
                                    WhatsAppHelper.sendDirectMessage(context, "", summaryMsg)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = StatusGreenBg, contentColor = StatusGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            }

                            // Export Excel/CSV
                            Button(
                                onClick = {
                                    BackupExportHelper.exportFinancialReportToExcel(
                                        context = context,
                                        periodName = if (isAr) selectedDateRange.labelAr else selectedDateRange.labelEn,
                                        categoryFilter = if (selectedCategoryFilter == "ALL") "جميع التصنيفات" else selectedCategoryFilter,
                                        totalSales = totalPeriodSales,
                                        totalProfits = totalPeriodProfits,
                                        totalExpenses = totalPeriodExpenses,
                                        netProfit = netPeriodProfit,
                                        stockValue = totalStockBuyValue,
                                        invoices = filteredInvoices,
                                        expenses = filteredExpenses,
                                        products = filteredProducts
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "إكسل" else "Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Interactive Filter Bar (Date Range & Categories)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Date Range Filter Chips
                    Text(
                        text = if (isAr) "الفترة الزمنية للتقرير:" else "Date Range Filter:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportDateRange.values().forEach { range ->
                            FilterChip(
                                selected = selectedDateRange == range,
                                onClick = { selectedDateRange = range },
                                label = { Text(if (isAr) range.labelAr else range.labelEn, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SkyLight,
                                    selectedLabelColor = SkyDeep
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    Text(
                        text = if (isAr) "تصفية حسب التصنيف:" else "Category Filter:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == "ALL",
                            onClick = { selectedCategoryFilter = "ALL" },
                            label = { Text(if (isAr) "جميع الأقسام" else "All Categories", fontSize = 11.sp) }
                        )

                        listOf("زيوت ومحركات", "كهربائيات", "إنارة", "وقود وديزل", "صيانة وفلاتر").forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Tab Selector (Profits, Expenses, Inventory, Cash Flow)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = SkyPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isAr) "الأرباح والمبيعات" else "Profits", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isAr) "المصروفات" else "Expenses", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(if (isAr) "المخزون" else "Inventory", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text(if (isAr) "التدفق والديون" else "Cash & Debt", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }

        // =================================================================
        // TAB 0: الأرباح والمبيعات (Profits & Sales Dashboard)
        // =================================================================
        if (selectedTab == 0) {
            // Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "إجمالي المبيعات" else "Total Revenue",
                        value = LanguageManager.formatMoney(totalPeriodSales, currency, isAr),
                        icon = Icons.Default.PointOfSale,
                        iconTint = SkyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightCard(
                        title = if (isAr) "صافي الأرباح" else "Net Profits",
                        value = LanguageManager.formatMoney(totalPeriodProfits, currency, isAr),
                        icon = Icons.Default.TrendingUp,
                        iconTint = EmeraldProfit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "هامش الربح" else "Profit Margin",
                        value = "$profitMarginPercent%",
                        icon = Icons.Default.PieChart,
                        iconTint = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                    val avgTicket = if (filteredInvoices.isNotEmpty()) totalPeriodSales / filteredInvoices.size else 0.0
                    MetricHighlightCard(
                        title = if (isAr) "متوسط الفاتورة" else "Avg Ticket",
                        value = LanguageManager.formatMoney(avgTicket, currency, isAr),
                        icon = Icons.Default.ReceiptLong,
                        iconTint = SkyDeep,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive Bar Chart: Sales & Profits Trend
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAr) "المخطط البياني للمبيعات والأرباح" else "Sales & Profit Bar Graph",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = if (isAr) "انقر على العمود للتفاصيل" else "Tap bar for details",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Generate sample or live time buckets
                        val barItems = remember(filteredInvoices) {
                            if (filteredInvoices.isEmpty()) {
                                listOf(
                                    BarChartItem(label = "الأحد", value = 120000.0, subValue = 35000.0),
                                    BarChartItem(label = "الإثنين", value = 185000.0, subValue = 48000.0),
                                    BarChartItem(label = "الثلاثاء", value = 90000.0, subValue = 24000.0),
                                    BarChartItem(label = "الأربعاء", value = 240000.0, subValue = 65000.0),
                                    BarChartItem(label = "الخميس", value = 310000.0, subValue = 82000.0),
                                    BarChartItem(label = "الجمعة", value = 150000.0, subValue = 40000.0),
                                    BarChartItem(label = "السبت", value = 220000.0, subValue = 58000.0)
                                )
                            } else {
                                filteredInvoices.take(7).mapIndexed { idx, inv ->
                                    BarChartItem(
                                        label = "#${inv.invoiceNumber}",
                                        value = inv.totalAmount,
                                        subValue = inv.totalAmount * 0.25
                                    )
                                }
                            }
                        }

                        InteractiveBarChart(
                            items = barItems,
                            currency = currency,
                            isAr = isAr
                        )
                    }
                }
            }

            // Sales Channels & Payment Type Breakdown
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isAr) "توزيع مصادر الدخل وطرق التحصيل" else "Revenue by Payment Type",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val cashSales = filteredInvoices.sumOf { it.paidAmount }
                        val creditSales = filteredInvoices.sumOf { it.remainingDebt }
                        val ampRevenue = ampSubscribers.sumOf { it.monthlyTotal }

                        DistributionProgressRow(
                            label = if (isAr) "مبيعات نقدية مقبوضة (كاش)" else "Cash Collected",
                            amount = cashSales,
                            total = maxOf(1.0, cashSales + creditSales + ampRevenue),
                            color = EmeraldProfit,
                            currency = currency,
                            isAr = isAr
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DistributionProgressRow(
                            label = if (isAr) "مبيعات آجلة (ديون على العملاء)" else "Credit Sales (Debts)",
                            amount = creditSales,
                            total = maxOf(1.0, cashSales + creditSales + ampRevenue),
                            color = RoseDebt,
                            currency = currency,
                            isAr = isAr
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DistributionProgressRow(
                            label = if (isAr) "إيرادات اشتراكات المولد والأمبيرات" else "Amp Revenue",
                            amount = ampRevenue,
                            total = maxOf(1.0, cashSales + creditSales + ampRevenue),
                            color = AmberAccent,
                            currency = currency,
                            isAr = isAr
                        )
                    }
                }
            }
        }

        // =================================================================
        // TAB 1: المصروفات والتكاليف (Expenses & Generator Costs)
        // =================================================================
        if (selectedTab == 1) {
            val dieselSum = filteredExpenses.filter { it.type == "DIESEL" }.sumOf { it.amount }
            val oilSum = filteredExpenses.filter { it.type == "OIL" }.sumOf { it.amount }
            val maintSum = filteredExpenses.filter { it.type == "MAINTENANCE" || it.type == "FILTER" }.sumOf { it.amount }
            val otherSum = filteredExpenses.filter { it.type != "DIESEL" && it.type != "OIL" && it.type != "MAINTENANCE" && it.type != "FILTER" }.sumOf { it.amount }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "إجمالي المصروفات" else "Total Expenses",
                        value = LanguageManager.formatMoney(totalPeriodExpenses, currency, isAr),
                        icon = Icons.Default.MoneyOff,
                        iconTint = RoseDebt,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightCard(
                        title = if (isAr) "كاز وديزل المولد" else "Diesel Fuel",
                        value = LanguageManager.formatMoney(dieselSum, currency, isAr),
                        icon = Icons.Default.LocalGasStation,
                        iconTint = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive Donut Chart of Expenses
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAr) "توزيع المصروفات بيانيًا (Donut Chart)" else "Expenses Breakdown",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = if (isAr) "انقر على القطاع" else "Tap slice",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }

                        val slices = listOf(
                            DonutSlice(label = if (isAr) "كاز وديزل" else "Diesel", value = if (dieselSum > 0) dieselSum else 130000.0, color = AmberAccent),
                            DonutSlice(label = if (isAr) "زيوت محرك" else "Engine Oil", value = if (oilSum > 0) oilSum else 40000.0, color = SkyPrimary),
                            DonutSlice(label = if (isAr) "صيانة وفلاتر" else "Maintenance", value = if (maintSum > 0) maintSum else 25000.0, color = RoseDebt),
                            DonutSlice(label = if (isAr) "مصروفات عامة" else "General", value = if (otherSum > 0) otherSum else 15000.0, color = Slate600)
                        )

                        InteractiveDonutChart(
                            slices = slices,
                            totalLabel = if (isAr) "المصروفات" else "Expenses",
                            currency = currency,
                            isAr = isAr
                        )
                    }
                }
            }

            // Recent Expenses List
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isAr) "سجل المصروفات المنفذة" else "Detailed Expenses Log",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredExpenses.isEmpty()) {
                            Text(text = if (isAr) "لا توجد مصروفات مسجلة لهذه الفترة" else "No expenses for period", fontSize = 11.sp, color = Slate400)
                        } else {
                            filteredExpenses.forEachIndexed { index, exp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    when (exp.type) {
                                                        "DIESEL" -> AmberBg
                                                        "OIL" -> SkyLight
                                                        else -> RoseBg
                                                    },
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (exp.type) {
                                                    "DIESEL" -> Icons.Default.LocalGasStation
                                                    "OIL" -> Icons.Default.Opacity
                                                    else -> Icons.Default.Build
                                                },
                                                contentDescription = null,
                                                tint = when (exp.type) {
                                                    "DIESEL" -> AmberAccent
                                                    "OIL" -> SkyPrimary
                                                    else -> RoseDebt
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(text = exp.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                            Text(
                                                text = "${LanguageManager.formatDateShort(exp.createdAt, isAr)} • ${exp.quantity} ${exp.unit}",
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }
                                    }

                                    Text(
                                        text = LanguageManager.formatMoney(exp.amount, currency, isAr),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = RoseDebt
                                    )
                                }
                                if (index < filteredExpenses.size - 1) {
                                    HorizontalDivider(color = Slate100, thickness = 0.8.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =================================================================
        // TAB 2: مستويات المخزون (Inventory Levels & Valuations)
        // =================================================================
        if (selectedTab == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "قيمة المخزون (شراء)" else "Inventory Cost",
                        value = LanguageManager.formatMoney(totalStockBuyValue, currency, isAr),
                        icon = Icons.Default.Inventory2,
                        iconTint = SkyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightCard(
                        title = if (isAr) "القيمة السوقية (بيع)" else "Retail Value",
                        value = LanguageManager.formatMoney(totalStockSellValue, currency, isAr),
                        icon = Icons.Default.Storefront,
                        iconTint = EmeraldProfit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "أرباح المخزون المتوقعة" else "Expected Stock Profit",
                        value = LanguageManager.formatMoney(potentialStockProfit, currency, isAr),
                        icon = Icons.Default.Savings,
                        iconTint = EmeraldProfit,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightCard(
                        title = if (isAr) "تنبيهات نقص المخزون" else "Low Stock Alerts",
                        value = "$lowStockCount مواد",
                        icon = Icons.Default.Warning,
                        iconTint = if (lowStockCount > 0) RoseDebt else StatusGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Stock Health Meter
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StockHealthMeter(
                            adequateCount = adequateStockCount,
                            lowStockCount = lowStockCount,
                            outOfStockCount = outOfStockCount,
                            isAr = isAr
                        )
                    }
                }
            }

            // Inventory by Category breakdown
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isAr) "تقييم المخزون حسب الأقسام والتصنيفات" else "Stock Valuation by Category",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val categoryGroups = filteredProducts.groupBy { it.category }
                        categoryGroups.forEach { (cat, prods) ->
                            val buyCost = prods.sumOf { it.stockQuantity * it.buyPrice }
                            val totalQty = prods.sumOf { it.stockQuantity }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = cat, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                    Text(text = "${prods.size} أصناف • ${totalQty.toInt()} قطعة بالمستودع", fontSize = 10.sp, color = Slate500)
                                }
                                Text(
                                    text = LanguageManager.formatMoney(buyCost, currency, isAr),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SkyDeep
                                )
                            }
                            HorizontalDivider(color = Slate100, thickness = 0.8.dp)
                        }
                    }
                }
            }
        }

        // =================================================================
        // TAB 3: التدفق النقدي والديون (Cash Flow & Debts)
        // =================================================================
        if (selectedTab == 3) {
            val totalCustomerDebts = customers.sumOf { it.currentDebt }
            val totalSupplierDebts = suppliers.sumOf { it.balanceOwed }
            val netDebtBalance = totalCustomerDebts - totalSupplierDebts
            val treasuryBalance = accounts.sumOf { it.balance }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "السيولة في الصناديق" else "Treasury Liquidity",
                        value = LanguageManager.formatMoney(treasuryBalance, currency, isAr),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = EmeraldProfit,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightCard(
                        title = if (isAr) "ديون العملاء (لنا)" else "Receivables",
                        value = LanguageManager.formatMoney(totalCustomerDebts, currency, isAr),
                        icon = Icons.Default.TrendingDown,
                        iconTint = RoseDebt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = if (isAr) "ديون الموردين (علينا)" else "Payables",
                        value = LanguageManager.formatMoney(totalSupplierDebts, currency, isAr),
                        icon = Icons.Default.LocalShipping,
                        iconTint = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightCard(
                        title = if (isAr) "صافي مركز الديون" else "Net Debt Position",
                        value = LanguageManager.formatMoney(netDebtBalance, currency, isAr),
                        icon = Icons.Default.Balance,
                        iconTint = if (netDebtBalance >= 0) EmeraldProfit else RoseDebt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Cash Flow Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isAr) "موجز التدفق النقدي والسيولة (Cash Flow)" else "Cash Flow Summary",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        DistributionProgressRow(
                            label = if (isAr) "التدفق النقدي الداخل (مبيعات نقدية)" else "Cash Inflows",
                            amount = totalPeriodSales,
                            total = maxOf(1.0, totalPeriodSales + totalPeriodExpenses),
                            color = EmeraldProfit,
                            currency = currency,
                            isAr = isAr
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DistributionProgressRow(
                            label = if (isAr) "التدفق النقدي الخارج (مصروفات تشغيلية)" else "Cash Outflows",
                            amount = totalPeriodExpenses,
                            total = maxOf(1.0, totalPeriodSales + totalPeriodExpenses),
                            color = RoseDebt,
                            currency = currency,
                            isAr = isAr
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricHighlightCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)
        }
    }
}

@Composable
fun DistributionProgressRow(
    label: String,
    amount: Double,
    total: Double,
    color: Color,
    currency: com.example.util.AppCurrency,
    isAr: Boolean
) {
    val fraction = (amount / total).toFloat().coerceIn(0.02f, 1.0f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = Slate700, fontWeight = FontWeight.Medium)
            Text(text = LanguageManager.formatMoney(amount, currency, isAr), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(Slate100, RoundedCornerShape(3.5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(7.dp)
                    .background(color, RoundedCornerShape(3.5.dp))
            )
        }
    }
}
