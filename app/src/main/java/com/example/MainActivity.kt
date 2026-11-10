package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.NavigationScreen
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AccountingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val language by viewModel.currentLanguage.collectAsState()
            val currency by viewModel.currentCurrency.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val activeUser by viewModel.activeUser.collectAsState()
            val metrics by viewModel.financialMetrics.collectAsState()
            val isAr = language == AppLanguage.ARABIC
            val layoutDirection = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr
            val context = LocalContext.current
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            // Request Notification Permission on Android 13+ (API 33+)
            val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    com.example.worker.InventoryCheckWorker.enqueuePeriodicInventoryCheck(context)
                }
            }

            LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    com.example.worker.InventoryCheckWorker.enqueuePeriodicInventoryCheck(context)
                }
            }

            // Back handler: Close drawer if open, or return to Dashboard from any secondary screen
            BackHandler(enabled = drawerState.isOpen || currentScreen != NavigationScreen.DASHBOARD) {
                if (drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                } else {
                    viewModel.setScreen(NavigationScreen.DASHBOARD)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color.White,
                                modifier = Modifier
                                    .widthIn(max = 320.dp)
                                    .fillMaxHeight()
                                    .testTag("navigation_drawer_sheet")
                            ) {
                                // Drawer Header with Ledger Branding & User Info
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate900)
                                        .padding(20.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_ledger_brand),
                                                contentDescription = "Ledger Logo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp))
                                            )
                                            Column {
                                                Text(
                                                    text = "LEDGER • ليدجر",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 17.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = if (isAr) "النظام المحاسبي الذكي المتكامل" else "Smart Accounting System",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFD4AF37)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = Slate700, thickness = 0.8.dp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(StatusGreen, CircleShape)
                                            )
                                            Text(
                                                text = "${activeUser.displayName} (${activeUser.role})",
                                                fontSize = 11.sp,
                                                color = Slate300,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Drawer Navigation Items
                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "لوحة التحكم الرئيسية" else "Dashboard") },
                                    selected = currentScreen == NavigationScreen.DASHBOARD,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.DASHBOARD)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_dashboard")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "الحسابات والصناديق والأرصدة" else "Accounts & Balances") },
                                    selected = currentScreen == NavigationScreen.ACCOUNTS,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.ACCOUNTS)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                    badge = {
                                        Text(
                                            text = LanguageManager.formatMoney(metrics.totalTreasuryBalance, currency, isAr),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldProfit
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_accounts")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "العملاء وسجل الديون" else "Customers & Debts") },
                                    selected = currentScreen == NavigationScreen.CUSTOMERS,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.CUSTOMERS)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                                    badge = {
                                        Text(
                                            text = LanguageManager.formatMoney(metrics.totalCustomerDebt, currency, isAr),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseDebt
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_customers")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "المخزون والمواد والباركود" else "Inventory & Stock") },
                                    selected = currentScreen == NavigationScreen.PRODUCTS,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.PRODUCTS)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                    badge = {
                                        if (metrics.lowStockCount > 0) {
                                            Text(
                                                text = "${metrics.lowStockCount} ⚠️",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoseDebt
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_products")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "نقطة البيع وفاتورة سريعة" else "POS & Sales") },
                                    selected = currentScreen == NavigationScreen.POS_SALE,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.POS_SALE)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_pos")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "إدارة المولد والأمبيرات ⚡" else "Amperes & Generators ⚡") },
                                    selected = currentScreen == NavigationScreen.AMPERES,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.AMPERES)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.ElectricBolt, contentDescription = null) },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_amperes")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "الموردين والمشتريات" else "Suppliers & Purchasing") },
                                    selected = currentScreen == NavigationScreen.SUPPLIERS,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.SUPPLIERS)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_suppliers")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "التقارير المالية والرسوم" else "Financial Reports") },
                                    selected = currentScreen == NavigationScreen.REPORTS,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.REPORTS)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_reports")
                                )

                                NavigationDrawerItem(
                                    label = { Text(if (isAr) "النسخ الاحتياطي والاستعادة" else "Backup & Cloud") },
                                    selected = currentScreen == NavigationScreen.BACKUP,
                                    onClick = {
                                        viewModel.setScreen(NavigationScreen.BACKUP)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_item_backup")
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Slate200, thickness = 1.dp)

                                // Quick support in drawer footer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { WhatsAppHelper.contactDeveloperSupport(context) }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = SkyPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isAr) "الدعم الفني المباشر" else "Direct Support",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800
                                        )
                                        Text(
                                            text = "07811996908 (WhatsApp)",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_ledger_brand),
                                                contentDescription = "Ledger Logo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.setScreen(NavigationScreen.DASHBOARD) }
                                            )
                                            Column {
                                                Text(
                                                    text = if (currentScreen == NavigationScreen.DASHBOARD) {
                                                        "LEDGER • ليدجر"
                                                    } else {
                                                        if (isAr) currentScreen.titleAr else currentScreen.titleEn
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = if (currentScreen == NavigationScreen.DASHBOARD) {
                                                        if (isAr) "النظام المحاسبي الذكي والمتكامل" else "Smart Accounting System"
                                                    } else {
                                                        if (isAr) "ليدجر • إدارة مالية ومخزنية" else "Ledger • Financial & Stock"
                                                    },
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFD4AF37).copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    },
                                    navigationIcon = {
                                        if (currentScreen != NavigationScreen.DASHBOARD) {
                                            IconButton(
                                                onClick = { viewModel.setScreen(NavigationScreen.DASHBOARD) },
                                                modifier = Modifier.testTag("top_app_bar_back_button")
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = if (isAr) "رجوع" else "Back",
                                                    tint = Color.White
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { scope.launch { drawerState.open() } },
                                                modifier = Modifier.testTag("top_app_bar_drawer_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.Menu,
                                                    contentDescription = if (isAr) "القائمة الرئيسية" else "Main Menu",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        // Quick Currency Switcher
                                        FilledTonalButton(
                                            onClick = { viewModel.toggleCurrency() },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(30.dp)
                                                .testTag("currency_toggle_button"),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color.White.copy(alpha = 0.15f),
                                                contentColor = SkyAccent
                                            )
                                        ) {
                                            Text(currency.code, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Quick Language Switcher
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.toggleLanguage() },
                                                modifier = Modifier.testTag("language_toggle_button")
                                            ) {
                                                Text(
                                                    if (isAr) "EN" else "عربي",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Direct WhatsApp Support Quick Call
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = { WhatsAppHelper.contactDeveloperSupport(context) },
                                                modifier = Modifier.testTag("support_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.SupportAgent,
                                                    contentDescription = "الدعم الفني",
                                                    tint = SkyAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Slate900,
                                        titleContentColor = Color.White,
                                        navigationIconContentColor = Color.White,
                                        actionIconContentColor = Color.White
                                    )
                                )
                            },
                            bottomBar = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .navigationBarsPadding()
                                ) {
                                    NavigationBar(
                                        containerColor = Color.White,
                                        tonalElevation = 2.dp,
                                        modifier = Modifier
                                            .height(64.dp)
                                            .testTag("main_bottom_navigation")
                                    ) {
                                        val itemColors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = SkyPrimary,
                                            selectedTextColor = SkyPrimary,
                                            indicatorColor = SkyLight,
                                            unselectedIconColor = Slate400,
                                            unselectedTextColor = Slate400
                                        )

                                        // 1. Dashboard
                                        NavigationBarItem(
                                            selected = currentScreen == NavigationScreen.DASHBOARD,
                                            onClick = { viewModel.setScreen(NavigationScreen.DASHBOARD) },
                                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                            label = {
                                                Text(
                                                    if (isAr) "الرئيسية" else "Home",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentScreen == NavigationScreen.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = itemColors,
                                            modifier = Modifier.testTag("nav_dashboard")
                                        )

                                        // 2. Current Balances & Accounts
                                        NavigationBarItem(
                                            selected = currentScreen == NavigationScreen.ACCOUNTS,
                                            onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) },
                                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                            label = {
                                                Text(
                                                    if (isAr) "الحسابات" else "Balances",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentScreen == NavigationScreen.ACCOUNTS) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = itemColors,
                                            modifier = Modifier.testTag("nav_accounts")
                                        )

                                        // 3. Debts & Receivables
                                        NavigationBarItem(
                                            selected = currentScreen == NavigationScreen.CUSTOMERS,
                                            onClick = { viewModel.setScreen(NavigationScreen.CUSTOMERS) },
                                            icon = {
                                                if (metrics.totalCustomerDebt > 0) {
                                                    BadgedBox(badge = {
                                                        Box(modifier = Modifier.size(7.dp).background(RoseDebt, CircleShape))
                                                    }) {
                                                        Icon(Icons.Default.People, contentDescription = null)
                                                    }
                                                } else {
                                                    Icon(Icons.Default.People, contentDescription = null)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    if (isAr) "الديون" else "Debts",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentScreen == NavigationScreen.CUSTOMERS) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = itemColors,
                                            modifier = Modifier.testTag("nav_debts")
                                        )

                                        // 4. Inventory & Stock Status
                                        NavigationBarItem(
                                            selected = currentScreen == NavigationScreen.PRODUCTS,
                                            onClick = { viewModel.setScreen(NavigationScreen.PRODUCTS) },
                                            icon = {
                                                if (metrics.lowStockCount > 0) {
                                                    BadgedBox(badge = {
                                                        Box(modifier = Modifier.size(7.dp).background(StatusRed, CircleShape))
                                                    }) {
                                                        Icon(Icons.Default.Inventory2, contentDescription = null)
                                                    }
                                                } else {
                                                    Icon(Icons.Default.Inventory2, contentDescription = null)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    if (isAr) "المخزون" else "Stock",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentScreen == NavigationScreen.PRODUCTS) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = itemColors,
                                            modifier = Modifier.testTag("nav_inventory")
                                        )

                                        // 5. POS & Fast Invoice
                                        NavigationBarItem(
                                            selected = currentScreen == NavigationScreen.POS_SALE,
                                            onClick = { viewModel.setScreen(NavigationScreen.POS_SALE) },
                                            icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                                            label = {
                                                Text(
                                                    if (isAr) "نقطة البيع" else "POS",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentScreen == NavigationScreen.POS_SALE) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = itemColors,
                                            modifier = Modifier.testTag("nav_pos")
                                        )
                                    }

                                    // Live Sync & Support Ribbon
                                    HorizontalDivider(color = Slate200, thickness = 1.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Slate100)
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(StatusGreen, CircleShape)
                                            )
                                            Text(
                                                text = if (isAr) "متصل ومزامن • قاعدة بيانات Room" else "Online & Synced • Local DB",
                                                fontSize = 10.sp,
                                                color = Slate500,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.clickable { scope.launch { drawerState.open() } }
                                        ) {
                                            Icon(Icons.Default.Menu, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(13.dp))
                                            Text(
                                                text = if (isAr) "كافة الأقسام (9)" else "All Sections (9)",
                                                fontSize = 10.sp,
                                                color = SkyPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentScreen) {
                                    NavigationScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                    NavigationScreen.POS_SALE -> PosInvoiceScreen(viewModel = viewModel)
                                    NavigationScreen.CUSTOMERS -> CustomersDebtScreen(viewModel = viewModel)
                                    NavigationScreen.PRODUCTS -> ProductsInventoryScreen(viewModel = viewModel)
                                    NavigationScreen.AMPERES -> AmperesScreen(viewModel = viewModel)
                                    NavigationScreen.SUPPLIERS -> SuppliersScreen(viewModel = viewModel)
                                    NavigationScreen.ACCOUNTS -> AccountsScreen(viewModel = viewModel)
                                    NavigationScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                                    NavigationScreen.BACKUP -> BackupAndCloudScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
