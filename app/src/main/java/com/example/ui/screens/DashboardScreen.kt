package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.NavigationScreen
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.Account
import com.example.data.model.DebtRecord
import com.example.data.model.Product
import com.example.util.AppCurrency
import com.example.util.AppLanguage
import com.example.util.DashboardSearchFilter
import com.example.util.DashboardSearchHelper
import com.example.util.DashboardSearchResults
import com.example.util.LanguageManager
import com.example.util.PdfInvoiceExportHelper
import com.example.util.WhatsAppHelper
import android.widget.Toast

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val metrics by viewModel.financialMetrics.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val products by viewModel.products.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val searchQuery by viewModel.dashboardSearchQuery.collectAsState()
    val selectedFilter by viewModel.dashboardSearchFilter.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val totalStockValue = remember(products) {
        products.sumOf { it.stockQuantity * it.sellPrice }
    }
    val outOfStockCount = remember(products) {
        products.count { it.stockQuantity <= 0.0 }
    }
    val healthyStockCount = remember(products) {
        products.count { it.stockQuantity > it.minStockAlert }
    }

    val searchResults = remember(searchQuery, accounts, debts, products) {
        DashboardSearchHelper.filter(searchQuery, accounts, debts, products)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Dashboard Universal Search Bar & Quick Filters
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (searchQuery.isNotBlank()) SkyPrimary else Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = if (searchQuery.isNotBlank()) 3.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(if (searchQuery.isNotBlank()) SkyPrimary.copy(alpha = 0.12f) else Slate100, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (isAr) "بحث" else "Search",
                                tint = if (searchQuery.isNotBlank()) SkyPrimary else Slate500,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setDashboardSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = if (isAr) "بحث في الحسابات، الديون، والمخزون..." else "Search accounts, debts, inventory by name...",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearDashboardSearch() },
                                        modifier = Modifier.testTag("search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = if (isAr) "مسح البحث" else "Clear search",
                                            tint = Slate500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = SkyPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input")
                        )
                    }

                    // Category Filter Chips
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == DashboardSearchFilter.ALL,
                            onClick = { viewModel.setDashboardSearchFilter(DashboardSearchFilter.ALL) },
                            label = {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        if (isAr) "الكل (${searchResults.totalCount})" else "All (${searchResults.totalCount})"
                                    } else {
                                        if (isAr) "الكل" else "All"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedFilter == DashboardSearchFilter.ALL) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_all")
                        )

                        FilterChip(
                            selected = selectedFilter == DashboardSearchFilter.ACCOUNTS,
                            onClick = { viewModel.setDashboardSearchFilter(DashboardSearchFilter.ACCOUNTS) },
                            label = {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        if (isAr) "الحسابات (${searchResults.matchingAccounts.size})" else "Accounts (${searchResults.matchingAccounts.size})"
                                    } else {
                                        if (isAr) "الحسابات" else "Accounts"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedFilter == DashboardSearchFilter.ACCOUNTS) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_accounts")
                        )

                        FilterChip(
                            selected = selectedFilter == DashboardSearchFilter.DEBTS,
                            onClick = { viewModel.setDashboardSearchFilter(DashboardSearchFilter.DEBTS) },
                            label = {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        if (isAr) "الديون (${searchResults.matchingDebts.size})" else "Debts (${searchResults.matchingDebts.size})"
                                    } else {
                                        if (isAr) "الديون والذمم" else "Debts"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedFilter == DashboardSearchFilter.DEBTS) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_debts")
                        )

                        FilterChip(
                            selected = selectedFilter == DashboardSearchFilter.INVENTORY,
                            onClick = { viewModel.setDashboardSearchFilter(DashboardSearchFilter.INVENTORY) },
                            label = {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        if (isAr) "المخزون (${searchResults.matchingProducts.size})" else "Inventory (${searchResults.matchingProducts.size})"
                                    } else {
                                        if (isAr) "المخزون والمواد" else "Inventory"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedFilter == DashboardSearchFilter.INVENTORY) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_inventory")
                        )
                    }
                }
            }
        }

        // Search Results Section when Search Query is Active
        if (searchQuery.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageSearch,
                                contentDescription = null,
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isAr) "نتائج البحث عن: \"$searchQuery\" (${searchResults.totalCount})" else "Results for \"$searchQuery\" (${searchResults.totalCount})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { viewModel.clearDashboardSearch() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFDE047))
                        ) {
                            Text(
                                text = if (isAr) "إلغاء البحث ✕" else "Clear ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (searchResults.isEmpty) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_no_results_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(RoseBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = RoseDebt,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isAr) "لم يتم العثور على أي نتائج مطابقة" else "No matching results found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAr) "لا يوجد أي حساب مالي أو دين أو مادة مخزون تطابق \"$searchQuery\"" else "No account, debt, or inventory item matched \"$searchQuery\"",
                                fontSize = 11.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.clearDashboardSearch() },
                                colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isAr) "مسح كلمة البحث" else "Clear search", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // Matching Accounts Section
                if ((selectedFilter == DashboardSearchFilter.ALL || selectedFilter == DashboardSearchFilter.ACCOUNTS) && searchResults.matchingAccounts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isAr) "الحسابات المالية (${searchResults.matchingAccounts.size})" else "Accounts (${searchResults.matchingAccounts.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }
                            TextButton(onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) }) {
                                Text(if (isAr) "عرض الكل" else "View all", fontSize = 11.sp, color = SkyPrimary)
                            }
                        }
                    }

                    items(searchResults.matchingAccounts, key = { "search_acc_${it.id}" }) { account ->
                        DashboardAccountSearchResultItem(
                            account = account,
                            currency = currency,
                            isAr = isAr,
                            onOpen = { viewModel.setScreen(NavigationScreen.ACCOUNTS) }
                        )
                    }
                }

                // Matching Debts Section
                if ((selectedFilter == DashboardSearchFilter.ALL || selectedFilter == DashboardSearchFilter.DEBTS) && searchResults.matchingDebts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.People, contentDescription = null, tint = RoseDebt, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isAr) "الديون والذمم (${searchResults.matchingDebts.size})" else "Debts (${searchResults.matchingDebts.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }
                            TextButton(onClick = { viewModel.setScreen(NavigationScreen.CUSTOMERS) }) {
                                Text(if (isAr) "عرض الكل" else "View all", fontSize = 11.sp, color = SkyPrimary)
                            }
                        }
                    }

                    items(searchResults.matchingDebts, key = { "search_debt_${it.id}" }) { debt ->
                        DashboardDebtSearchResultItem(
                            debt = debt,
                            currency = currency,
                            isAr = isAr,
                            onOpen = { viewModel.setScreen(NavigationScreen.CUSTOMERS) },
                            onWhatsAppReminder = {
                                if (debt.debtorPhone.isNotBlank()) {
                                    val msg = if (isAr) {
                                        "مرحباً ${debt.debtorName}، نود تذكيركم بالمبلغ المستحق بذمتكم وقدره ${LanguageManager.formatMoney(debt.remainingAmount, currency, isAr)}."
                                    } else {
                                        "Hello ${debt.debtorName}, this is a friendly reminder regarding your outstanding balance of ${LanguageManager.formatMoney(debt.remainingAmount, currency, isAr)}."
                                    }
                                    WhatsAppHelper.openWhatsApp(context, debt.debtorPhone, msg)
                                }
                            }
                        )
                    }
                }

                // Matching Inventory Items Section
                if ((selectedFilter == DashboardSearchFilter.ALL || selectedFilter == DashboardSearchFilter.INVENTORY) && searchResults.matchingProducts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isAr) "المخزون والمواد (${searchResults.matchingProducts.size})" else "Inventory Items (${searchResults.matchingProducts.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }
                            TextButton(onClick = { viewModel.setScreen(NavigationScreen.PRODUCTS) }) {
                                Text(if (isAr) "عرض الكل" else "View all", fontSize = 11.sp, color = SkyPrimary)
                            }
                        }
                    }

                    items(searchResults.matchingProducts, key = { "search_prod_${it.id}" }) { product ->
                        DashboardInventorySearchResultItem(
                            product = product,
                            currency = currency,
                            isAr = isAr,
                            onOpen = { viewModel.setScreen(NavigationScreen.PRODUCTS) }
                        )
                    }
                }

                item {
                    HorizontalDivider(color = Slate200, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // 1. Ledger Brand Header Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ledger_brand),
                        contentDescription = "Ledger Emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LEDGER",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE047),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "ليدجر",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (isAr) "النظام المحاسبي الذكي والمتكامل" else "Smart Financial Ledger System",
                            fontSize = 11.sp,
                            color = Slate300,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(StatusGreen, CircleShape)
                            )
                            Text(
                                text = if (isAr) "المحاسب المسؤول: ${activeUser.displayName}" else "Accountant: ${activeUser.displayName}",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }

        // 1. Current Balances Summary Card (أرصدة الحسابات والصناديق الحالية)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, EmeraldProfit.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_card_balances")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(EmeraldBg, CircleShape)
                                    .border(1.dp, EmeraldProfit.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = EmeraldProfit,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isAr) "أرصدة الحسابات والصناديق الحالية" else "Current Balances & Treasury",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = if (isAr) "السيولة النقدية والمصرفية المتوفرة" else "Available liquid funds & bank accounts",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val pdfFile = PdfInvoiceExportHelper.exportAllAccountsSummaryPdf(
                                        context = context,
                                        accounts = accounts,
                                        totalBalance = metrics.totalTreasuryBalance,
                                        currencySymbol = currency.code,
                                        isArabic = isAr
                                    )
                                    if (pdfFile != null) {
                                        PdfInvoiceExportHelper.sharePdf(context, pdfFile, if (isAr) "تقرير الأرصدة الشامل" else "All Balances Summary")
                                    } else {
                                        Toast.makeText(context, if (isAr) "فشل إنشاء تقرير PDF" else "Failed to generate report", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("btn_dashboard_export_balances_pdf")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "تصدير الأرصدة PDF",
                                    tint = EmeraldProfit,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("btn_view_accounts_summary"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldBg,
                                    contentColor = EmeraldProfit
                                )
                            ) {
                                Text(
                                    text = if (isAr) "إدارة الحسابات" else "Manage",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Balance Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "إجمالي الرصيد والسيولة المتاحة" else "Total Available Liquidity",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = LanguageManager.formatMoney(metrics.totalTreasuryBalance, currency, isAr),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldProfit
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(StatusGreenBg, RoundedCornerShape(10.dp))
                                .border(1.dp, StatusGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(StatusGreen, CircleShape)
                                )
                                Text(
                                    text = if (isAr) "سيولة نشطة ✓" else "Active Liquidity",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Account Breakdown Preview
                    val previewAccounts = accounts.take(3)
                    if (previewAccounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            previewAccounts.forEach { acc ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.8.dp, Slate200),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setScreen(NavigationScreen.ACCOUNTS) }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = acc.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = LanguageManager.formatMoney(acc.balance, currency, isAr),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (acc.balance >= 0) EmeraldProfit else RoseDebt,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Action Row for Balances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            border = BorderStroke(1.dp, Slate300)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate700)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isAr) "إيداع / حركة نقدية" else "Deposit / Transfer", fontSize = 11.sp, color = Slate700)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setScreen(NavigationScreen.REPORTS) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            border = BorderStroke(1.dp, Slate300)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate700)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isAr) "كشف الحسابات" else "Treasury Report", fontSize = 11.sp, color = Slate700)
                        }
                    }
                }
            }
        }

        // 2. Debts & Receivables Summary Card (ملخص الديون والذمم المالية)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, RoseDebt.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_card_debts")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(RoseBg, CircleShape)
                                    .border(1.dp, RoseDebt.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = RoseDebt,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isAr) "ملخص الديون والذمم المالية" else "Debts & Receivables Summary",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = if (isAr) "متابعة ديون العملاء والتزامات الموردين" else "Customer receivables & supplier payables",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setScreen(NavigationScreen.CUSTOMERS) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("btn_view_debts_summary"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = RoseBg,
                                contentColor = RoseDebt
                            )
                        ) {
                            Text(
                                text = if (isAr) "سجل الديون" else "All Debts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Two Columns: Customer Debt (لنا) & Supplier Debt (علينا)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Customer Debt Panel
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RoseBg.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RoseDebt.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setScreen(NavigationScreen.CUSTOMERS) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = RoseDebt, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = if (isAr) "ديون العملاء (لنا)" else "Customer Debts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseDebt
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = LanguageManager.formatMoney(metrics.totalCustomerDebt, currency, isAr),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isAr) "${customers.count { it.currentDebt > 0 }} عملاء بذمتهم مبالغ" else "${customers.count { it.currentDebt > 0 }} in debt",
                                    fontSize = 10.sp,
                                    color = Slate600
                                )
                            }
                        }

                        // Supplier Debt Panel
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberBg.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setScreen(NavigationScreen.SUPPLIERS) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = if (isAr) "ديون الموردين (علينا)" else "Supplier Debts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = LanguageManager.formatMoney(metrics.totalSupplierDebt, currency, isAr),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isAr) "${suppliers.count { it.balanceOwed > 0 }} موردين مستحقي الدفع" else "${suppliers.count { it.balanceOwed > 0 }} suppliers owed",
                                    fontSize = 10.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Net Debt Position Ribbon (Customer Debt - Supplier Debt)
                    val netDebtBalance = metrics.totalCustomerDebt - metrics.totalSupplierDebt
                    val isPositiveBalance = netDebtBalance >= 0
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPositiveBalance) EmeraldBg.copy(alpha = 0.6f) else RoseBg.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isPositiveBalance) EmeraldProfit.copy(alpha = 0.3f) else RoseDebt.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositiveBalance) Icons.Default.TrendingUp else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isPositiveBalance) EmeraldProfit else RoseDebt,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isPositiveBalance) {
                                        if (isAr) "صافي الذمم لصالحنا (فائض مستحق)" else "Net Receivable Surplus"
                                    } else {
                                        if (isAr) "صافي التزامات واجبة السداد" else "Net Payable Deficit"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }

                            Text(
                                text = (if (isPositiveBalance) "+" else "") + LanguageManager.formatMoney(netDebtBalance, currency, isAr),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositiveBalance) EmeraldProfit else RoseDebt
                            )
                        }
                    }
                }
            }
        }

        // 3. Inventory Status Summary Card (حالة ومؤشرات المخزون والمواد)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SkyPrimary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_card_inventory")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SkyLight, CircleShape)
                                    .border(1.dp, SkyPrimary.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = SkyPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isAr) "حالة ومؤشرات المخزون والمواد" else "Inventory & Stock Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = if (isAr) "تقييم البضاعة، الأصناف، وتنبيهات النواقص" else "Valuation, SKUs & stock health alerts",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setScreen(NavigationScreen.PRODUCTS) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("btn_view_inventory_summary"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SkyLight,
                                contentColor = SkyDeep
                            )
                        ) {
                            Text(
                                text = if (isAr) "إدارة المخزون" else "Manage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Stock Valuation & Quantities
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "إجمالي القيمة التقديرية للبضاعة" else "Estimated Inventory Value",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = LanguageManager.formatMoney(totalStockValue, currency, isAr),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SkyPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isAr) "الأصناف والكميات" else "Total SKUs & Units",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${products.size} ${if (isAr) "أصناف" else "SKUs"} • ${products.sumOf { it.stockQuantity }.toInt()} ${if (isAr) "قطعة" else "units"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stock Health Status Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Low Stock Badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (metrics.lowStockCount > 0) StatusRedBg else StatusGreenBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (metrics.lowStockCount > 0) StatusRed.copy(alpha = 0.3f) else StatusGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (metrics.lowStockCount > 0) "${metrics.lowStockCount} ⚠️" else "0 ✓",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (metrics.lowStockCount > 0) StatusRed else StatusGreen
                                )
                                Text(
                                    text = if (isAr) "أوشك على النفاد" else "Low Stock",
                                    fontSize = 9.sp,
                                    color = Slate600
                                )
                            }
                        }

                        // Out of Stock Badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (outOfStockCount > 0) StatusRedBg.copy(alpha = 0.7f) else Slate100
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (outOfStockCount > 0) StatusRed.copy(alpha = 0.4f) else Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$outOfStockCount",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (outOfStockCount > 0) StatusRed else Slate700
                                )
                                Text(
                                    text = if (isAr) "نفد من المخزن" else "Out of Stock",
                                    fontSize = 9.sp,
                                    color = Slate600
                                )
                            }
                        }

                        // Healthy Stock Badge
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusGreenBg),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$healthyStockCount",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                                Text(
                                    text = if (isAr) "مستوى آمن" else "Adequate",
                                    fontSize = 9.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    // Low Stock Alert Banner (if any)
                    if (lowStockProducts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusRedBg),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setScreen(NavigationScreen.PRODUCTS) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (isAr) "تنبيه: ${lowStockProducts.first().name} (متبقي: ${lowStockProducts.first().stockQuantity})" else "Alert: ${lowStockProducts.first().name} (Qty: ${lowStockProducts.first().stockQuantity})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusRed,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = if (isAr) "طلب توريد ←" else "Restock →",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Actions for Inventory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setScreen(NavigationScreen.PRODUCTS) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            border = BorderStroke(1.dp, Slate300)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate700)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isAr) "إضافة مادة جديدة" else "New Product", fontSize = 11.sp, color = Slate700)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setScreen(NavigationScreen.POS_SALE) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            border = BorderStroke(1.dp, Slate300)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate700)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isAr) "فحص الباركود" else "Scan Barcode", fontSize = 11.sp, color = Slate700)
                        }
                    }
                }
            }
        }

        // 2. Highlight Dark Card for Amperes (Slate 900 Banner with Glowing Indicators & Net Profit)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(NavigationScreen.AMPERES) }
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "إدارة الأمبيرات والمولد ⚡" else "Generator & Amperes ⚡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) "${metrics.totalAmperesSold} أمبير مباع (${LanguageManager.formatMoney(metrics.averageAmpPrice, currency, isAr)})" else "${metrics.totalAmperesSold} A Sold (${LanguageManager.formatMoney(metrics.averageAmpPrice, currency, isAr)})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Green Dot: Delivered / Paid
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .background(StatusGreen, CircleShape)
                                        .border(2.dp, StatusGreen.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isAr) "تم (${metrics.ampDeliveredCount})" else "Delivered (${metrics.ampDeliveredCount})",
                                    fontSize = 9.sp,
                                    color = Slate300
                                )
                            }

                            // Red Dot: Pending / Undelivered
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .background(StatusRed, CircleShape)
                                        .border(2.dp, StatusRed.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isAr) "متبقي (${metrics.ampUndeliveredCount})" else "Pending (${metrics.ampUndeliveredCount})",
                                    fontSize = 9.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate700, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "صرف المولد: ${LanguageManager.formatMoney(metrics.totalGeneratorExpenses, currency, isAr)}" else "Fuel & Oil: ${LanguageManager.formatMoney(metrics.totalGeneratorExpenses, currency, isAr)}",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAr) "صافي الربح: " else "Net Profit: ",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                            Text(
                                text = LanguageManager.formatMoney(metrics.generatorNetProfit, currency, isAr),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.generatorNetProfit >= 0) EmeraldProfit else RoseDebt
                            )
                        }
                    }
                }
            }
        }

        // 3. Customer Ledger & Debt Preview Card (Design HTML Exact Pattern)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Card Sub-header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100.copy(alpha = 0.6f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "سجل العملاء والديون المستحقة" else "Customer Debts & Ledger",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        FilledTonalButton(
                            onClick = { viewModel.setScreen(NavigationScreen.CUSTOMERS) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SkyLight,
                                contentColor = SkyDeep
                            )
                        ) {
                            Text(text = if (isAr) "عرض الكل" else "View All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    // Customer Items
                    val displayCustomers = customers.take(4)
                    if (displayCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAr) "لا يوجد عملاء مسجلين حالياً" else "No customers registered yet",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    } else {
                        displayCustomers.forEachIndexed { index, cust ->
                            val hasDebt = cust.currentDebt > 0
                            val stripColor = if (hasDebt) StatusRed else StatusGreen
                            val rowBg = if (hasDebt) RoseBg.copy(alpha = 0.3f) else Color.White

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .clickable { viewModel.setScreen(NavigationScreen.CUSTOMERS) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(34.dp)
                                            .background(stripColor, RoundedCornerShape(2.dp))
                                    )
                                    Column {
                                        Text(
                                            text = cust.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate800
                                        )
                                        Text(
                                            text = if (cust.phone.isNotEmpty()) cust.phone else if (isAr) "عميل نظام" else "Standard Customer",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = LanguageManager.formatMoney(cust.currentDebt, currency, isAr),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (hasDebt) RoseDebt else EmeraldProfit
                                        )
                                        Text(
                                            text = if (hasDebt) (if (isAr) "مستحق وغير مسدد" else "Pending Debt") else (if (isAr) "مدفوع بالكامل ✓" else "Fully Paid ✓"),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (hasDebt) StatusRed else StatusGreen
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(if (hasDebt) StatusRedBg else StatusGreenBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (hasDebt) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (hasDebt) StatusRed else StatusGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (index < displayCustomers.size - 1) {
                                HorizontalDivider(color = Slate100, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Four-Button Quick Action Grid (Barcode, Inventory, Print, Cloud Backup)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Barcode POS
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = if (isAr) "باركود" else "Barcode",
                    onClick = { viewModel.setScreen(NavigationScreen.POS_SALE) },
                    modifier = Modifier.weight(1f)
                )

                // Button 2: Stock
                QuickActionButton(
                    icon = Icons.Default.Inventory2,
                    label = if (isAr) "المخزون" else "Stock",
                    onClick = { viewModel.setScreen(NavigationScreen.PRODUCTS) },
                    modifier = Modifier.weight(1f)
                )

                // Button 3: Print / Reports
                QuickActionButton(
                    icon = Icons.Default.Print,
                    label = if (isAr) "طباعة" else "Print",
                    onClick = { viewModel.setScreen(NavigationScreen.REPORTS) },
                    modifier = Modifier.weight(1f)
                )

                // Button 4: Cloud Backup
                QuickActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = if (isAr) "نسخ" else "Backup",
                    onClick = { viewModel.setScreen(NavigationScreen.BACKUP) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Operational Summary & Secondary Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Sales
                MetricCard(
                    title = if (isAr) "إجمالي المبيعات" else "Total Sales",
                    value = LanguageManager.formatMoney(metrics.totalSales, currency, isAr),
                    icon = Icons.Default.PointOfSale,
                    accentColor = SkyPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Supplier Debts
                MetricCard(
                    title = if (isAr) "ديون الموردين" else "Supplier Debts",
                    value = LanguageManager.formatMoney(metrics.totalSupplierDebt, currency, isAr),
                    icon = Icons.Default.LocalShipping,
                    accentColor = Slate600,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6. Navigation Modules Hub
        item {
            Text(
                text = if (isAr) "الأقسام الرئيسية والعمليات" else "Management Modules",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionModuleCard(
                        title = if (isAr) "نقطة البيع وفاتورة سريعة" else "POS & Fast Invoice",
                        subtitle = if (isAr) "باركود، واتساب، طباعة" else "Barcode, WhatsApp, Print",
                        icon = Icons.Default.PointOfSale,
                        badgeColor = SkyPrimary,
                        onClick = { viewModel.setScreen(NavigationScreen.POS_SALE) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionModuleCard(
                        title = if (isAr) "العملاء والديون والوصولات" else "Customers & Debts",
                        subtitle = if (isAr) "سجل الحسابات، تذكير الديون" else "Ledger, WhatsApp Reminder",
                        icon = Icons.Default.People,
                        badgeColor = RoseDebt,
                        onClick = { viewModel.setScreen(NavigationScreen.CUSTOMERS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionModuleCard(
                        title = if (isAr) "بيع الأمبيرات والمولد ⚡" else "Amperes & Generators ⚡",
                        subtitle = if (isAr) "فواتير 🟢 و 🔴 وحساب الديون" else "Green 🟢 & Red 🔴 Delivery",
                        icon = Icons.Default.ElectricBolt,
                        badgeColor = AmberAccent,
                        onClick = { viewModel.setScreen(NavigationScreen.AMPERES) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionModuleCard(
                        title = if (isAr) "المخزون والمنتجات" else "Inventory & Barcode",
                        subtitle = if (isAr) "${metrics.lowStockCount} مواد أوشكت على النفاد" else "${metrics.lowStockCount} Low Stock Alert",
                        icon = Icons.Default.Inventory,
                        badgeColor = if (metrics.lowStockCount > 0) RoseDebt else StatusGreen,
                        onClick = { viewModel.setScreen(NavigationScreen.PRODUCTS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionModuleCard(
                        title = if (isAr) "التقارير المالية والرسوم" else "Financial Reports",
                        subtitle = if (isAr) "أرباح، مصروفات، تصدير إكسل" else "Charts, Taxes, Excel Export",
                        icon = Icons.Default.BarChart,
                        badgeColor = SkyDeep,
                        onClick = { viewModel.setScreen(NavigationScreen.REPORTS) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionModuleCard(
                        title = if (isAr) "النسخ الاحتياطي والاستعادة" else "Backup & Data Restore",
                        subtitle = if (isAr) "تصدير واستيراد JSON، نسخ سحابي" else "JSON Export/Import, Cloud & Support",
                        icon = Icons.Default.Save,
                        badgeColor = Slate700,
                        onClick = { viewModel.setScreen(NavigationScreen.BACKUP) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionModuleCard(
                        title = if (isAr) "إدارة الموردين والمشتريات" else "Suppliers & Purchases",
                        subtitle = if (isAr) "فواتير التوريد، الديون للموردين" else "Supplier Ledger & Owed",
                        icon = Icons.Default.LocalShipping,
                        badgeColor = Slate600,
                        onClick = { viewModel.setScreen(NavigationScreen.SUPPLIERS) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionModuleCard(
                        title = if (isAr) "الصناديق وسجل الديون" else "Accounts & Debts Ledger",
                        subtitle = if (isAr) "رصيد الصناديق: ${LanguageManager.formatMoney(metrics.totalTreasuryBalance, currency, isAr)}" else "Treasury & Debts",
                        icon = Icons.Default.AccountBalanceWallet,
                        badgeColor = EmeraldProfit,
                        onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7. Smart Notifications & Schedules
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "الإشعارات والتنبيهات المجدولة" else "Notifications & Alerts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Text(
                    text = "${notifications.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseDebt,
                    modifier = Modifier
                        .background(RoseDebt.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (notifications.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAr) "لا توجد تنبيهات عاجلة حالياً. جميع المواعيد والمستحقات منتظمة ✓" else "No urgent notifications. Everything is up to date ✓",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notifications.forEach { notif ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (notif.relatedType == "LOW_STOCK") StatusRedBg else AmberBg,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (notif.relatedType == "LOW_STOCK") Icons.Default.Warning else Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (notif.relatedType == "LOW_STOCK") StatusRed else AmberAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                                    Text(text = notif.description, fontSize = 11.sp, color = Slate500)
                                }
                                IconButton(onClick = { viewModel.deleteNotification(notif) }) {
                                    Icon(Icons.Default.Check, contentDescription = "تم", tint = StatusGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SkyAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate800)
        }
    }
}

@Composable
fun ActionModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 10.sp, color = Slate500, maxLines = 1)
            }
        }
    }
}

@Composable
fun DashboardAccountSearchResultItem(
    account: Account,
    currency: AppCurrency,
    isAr: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("account_result_item_${account.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(EmeraldBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = EmeraldProfit,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    if (account.code.isNotBlank()) {
                        Text(
                            text = "[${account.code}]",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500,
                            modifier = Modifier
                                .background(Slate100, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val typeName = when (account.type) {
                    "CASH" -> if (isAr) "صندوق نقدي" else "Cash Treasury"
                    "BANK" -> if (isAr) "حساب بنكي" else "Bank Account"
                    "WALLET" -> if (isAr) "محفظة إلكترونية" else "Digital Wallet"
                    "CAPITAL" -> if (isAr) "رأس المال" else "Capital"
                    "REVENUE" -> if (isAr) "إيرادات عامة" else "General Revenue"
                    else -> account.type
                }
                Text(
                    text = typeName,
                    fontSize = 10.sp,
                    color = Slate500
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = LanguageManager.formatMoney(account.balance, currency, isAr),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (account.balance >= 0) EmeraldProfit else RoseDebt
                )
                Text(
                    text = if (isAr) "الرصيد المتاح" else "Balance",
                    fontSize = 9.sp,
                    color = Slate400
                )
            }
        }
    }
}

@Composable
fun DashboardDebtSearchResultItem(
    debt: DebtRecord,
    currency: AppCurrency,
    isAr: Boolean,
    onOpen: () -> Unit,
    onWhatsAppReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReceivable = debt.debtType == "RECEIVABLE"
    val isPaid = debt.status == "PAID" || debt.remainingAmount <= 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("debt_result_item_${debt.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (isReceivable) RoseBg else AmberBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isReceivable) Icons.Default.ReceiptLong else Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (isReceivable) RoseDebt else AmberAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = debt.debtorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    Text(
                        text = if (isReceivable) (if (isAr) "لنا (عميل)" else "Receivable") else (if (isAr) "علينا (مورد)" else "Payable"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceivable) RoseDebt else AmberAccent,
                        modifier = Modifier
                            .background(
                                (if (isReceivable) RoseDebt else AmberAccent).copy(alpha = 0.12f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (debt.debtorPhone.isNotBlank()) debt.debtorPhone else (if (isAr) "سجل ديون معتمد" else "Registered debt"),
                    fontSize = 10.sp,
                    color = Slate500
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = LanguageManager.formatMoney(debt.remainingAmount, currency, isAr),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaid) StatusGreen else (if (isReceivable) RoseDebt else AmberAccent)
                )
                Text(
                    text = if (isPaid) (if (isAr) "مسدد بالكامل ✓" else "Fully Paid ✓") else (if (isAr) "المبلغ المتبقي" else "Remaining"),
                    fontSize = 9.sp,
                    color = if (isPaid) StatusGreen else Slate400,
                    fontWeight = FontWeight.Medium
                )
            }

            if (debt.debtorPhone.isNotBlank() && !isPaid) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onWhatsAppReminder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = StatusGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardInventorySearchResultItem(
    product: Product,
    currency: AppCurrency,
    isAr: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = product.stockQuantity <= product.minStockAlert

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isLowStock) StatusRed.copy(alpha = 0.4f) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("product_result_item_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (isLowStock) StatusRedBg else SkyLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = if (isLowStock) StatusRed else SkyPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    if (isLowStock) {
                        Text(
                            text = if (isAr) "منخفض ⚠️" else "Low ⚠️",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusRed,
                            modifier = Modifier
                                .background(StatusRedBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.category,
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "• ${product.barcode}",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.stockQuantity} ${product.unit}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLowStock) StatusRed else Slate800
                )
                Text(
                    text = LanguageManager.formatMoney(product.sellPrice, currency, isAr),
                    fontSize = 10.sp,
                    color = SkyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

