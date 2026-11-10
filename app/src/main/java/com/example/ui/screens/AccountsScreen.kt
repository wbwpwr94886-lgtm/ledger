package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.AccountTransaction
import com.example.data.model.DebtRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AmountInputField
import com.example.util.AppCurrency
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.PdfInvoiceExportHelper
import com.example.util.ValidationHelper
import com.example.util.ValidationResult
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val accounts by viewModel.accounts.collectAsState()
    val accountTransactions by viewModel.accountTransactions.collectAsState()
    val debts by viewModel.debts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: الصناديق والحسابات, 1: سجل الديون, 2: كشف القيود
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showDepositDialogFor by remember { mutableStateOf<Account?>(null) }
    var showWithdrawDialogFor by remember { mutableStateOf<Account?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtRecord?>(null) }

    val totalTreasury = accounts.sumOf { it.balance }
    val totalCustomerDebt = debts.filter { it.debtType == "RECEIVABLE" && it.status == "ACTIVE" }.sumOf { it.remainingAmount }
    val totalSupplierDebt = debts.filter { it.debtType == "PAYABLE" && it.status == "ACTIVE" }.sumOf { it.remainingAmount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddAccountDialog = true
                    else if (selectedTab == 1) showAddDebtDialog = true
                    else showTransferDialog = true
                },
                containerColor = SkyPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.AccountBalanceWallet else if (selectedTab == 1) Icons.Default.PostAdd else Icons.Default.SwapHoriz,
                    contentDescription = "إضافة"
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
        ) {
            // Header: Total Financial Liquidity & Debts
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) "إجمالي السيولة النقدية (الصناديق والبنوك)" else "Total Treasury Balance",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate400
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = LanguageManager.formatMoney(totalTreasury, currency, isAr),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        val pdfFile = PdfInvoiceExportHelper.exportAllAccountsSummaryPdf(
                                            context = context,
                                            accounts = accounts,
                                            totalBalance = totalTreasury,
                                            currencySymbol = currency.code,
                                            isArabic = isAr
                                        )
                                        if (pdfFile != null) {
                                            PdfInvoiceExportHelper.sharePdf(context, pdfFile, if (isAr) "تقرير الأرصدة الشامل" else "All Accounts Summary")
                                        } else {
                                            Toast.makeText(context, if (isAr) "فشل إنشاء تقرير PDF" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_export_all_balances_pdf")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (isAr) "تقرير PDF" else "PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = { showTransferDialog = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color(0xFFFDE047)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "تحويل مالي" else "Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Slate700, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = if (isAr) "ديون مستحقة لنا (عملاء)" else "Receivables (Clients)", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = LanguageManager.formatMoney(totalCustomerDebt, currency, isAr),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldProfit
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = if (isAr) "ديون مستحقة علينا (موردون)" else "Payables (Suppliers)", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = LanguageManager.formatMoney(totalSupplierDebt, currency, isAr),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDebt
                                )
                            }
                        }
                    }
                }
            }

            // Tabs Selector
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
                        text = { Text(if (isAr) "الحسابات والصناديق (${accounts.size})" else "Accounts (${accounts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (isAr) "سجل الديون (${debts.count { it.status == "ACTIVE" }})" else "Debts (${debts.count { it.status == "ACTIVE" }})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (isAr) "سجل القيود" else "Journal", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            // Tab Content 0: Accounts list
            if (selectedTab == 0) {
                if (accounts.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(if (isAr) "لا توجد حسابات مسجلة حالياً" else "No accounts recorded", color = Slate400)
                            }
                        }
                    }
                } else {
                    items(accounts) { acc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (acc.isDefault) SkyPrimary.copy(alpha = 0.5f) else Slate200),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                                .background(
                                                    when (acc.type) {
                                                        "CASH" -> EmeraldBg
                                                        "BANK" -> SkyLight
                                                        "WALLET" -> AmberBg
                                                        else -> Slate100
                                                    },
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (acc.type) {
                                                    "CASH" -> Icons.Default.Payments
                                                    "BANK" -> Icons.Default.AccountBalance
                                                    "WALLET" -> Icons.Default.PhoneAndroid
                                                    else -> Icons.Default.AccountBalanceWallet
                                                },
                                                contentDescription = null,
                                                tint = when (acc.type) {
                                                    "CASH" -> EmeraldProfit
                                                    "BANK" -> SkyPrimary
                                                    "WALLET" -> AmberAccent
                                                    else -> Slate600
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(text = acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                                if (acc.isDefault) {
                                                    Surface(
                                                        color = SkyLight,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(text = if (isAr) "افتراضي" else "Default", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SkyDeep, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (acc.description.isNotBlank()) acc.description else when (acc.type) {
                                                    "CASH" -> "خزينة نقدية"
                                                    "BANK" -> "حساب مصرفي"
                                                    "WALLET" -> "محفظة دفع رقمي"
                                                    else -> "حساب مالي"
                                                },
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        }
                                    }

                                    val accCurrency = if (acc.currency == "USD") AppCurrency.USD else AppCurrency.IQD
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = LanguageManager.formatMoney(acc.balance, accCurrency, isAr),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (acc.balance >= 0) EmeraldProfit else RoseDebt
                                        )
                                        Text(text = acc.currency, fontSize = 10.sp, color = Slate400)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Slate100, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val txs = accountTransactions.filter { it.accountId == acc.id }
                                            val pdfFile = PdfInvoiceExportHelper.exportAccountSummaryPdf(
                                                context = context,
                                                account = acc,
                                                transactions = txs,
                                                currencySymbol = currency.code,
                                                isArabic = isAr
                                            )
                                            if (pdfFile != null) {
                                                PdfInvoiceExportHelper.sharePdf(context, pdfFile, if (isAr) "كشف حساب: ${acc.name}" else "Account Statement: ${acc.name}")
                                            } else {
                                                Toast.makeText(context, if (isAr) "فشل إنشاء كشف الحساب" else "Failed to generate statement", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = SkyLight,
                                            contentColor = SkyDeep
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp).testTag("btn_export_account_pdf_${acc.id}")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(if (isAr) "كشف PDF" else "Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    OutlinedButton(
                                        onClick = { showWithdrawDialogFor = acc },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(13.dp), tint = RoseDebt)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(if (isAr) "سحب" else "Withdraw", fontSize = 11.sp, color = RoseDebt)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = { showDepositDialogFor = acc },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = EmeraldBg,
                                            contentColor = EmeraldProfit
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(if (isAr) "إيداع" else "Deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Content 1: Debts Ledger (سجل الديون والذمم)
            if (selectedTab == 1) {
                if (debts.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(if (isAr) "لا توجد سجلات ديون مسجلة" else "No debts recorded", color = Slate400)
                            }
                        }
                    }
                } else {
                    items(debts) { debt ->
                        val isReceivable = debt.debtType == "RECEIVABLE"
                        val isPaid = debt.status == "PAID" || debt.remainingAmount <= 0

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isPaid) StatusGreen.copy(alpha = 0.3f) else if (isReceivable) RoseDebt.copy(alpha = 0.35f) else AmberAccent.copy(alpha = 0.35f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                                .size(38.dp)
                                                .background(
                                                    if (isPaid) StatusGreenBg else if (isReceivable) StatusRedBg else AmberBg,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPaid) Icons.Default.CheckCircle else if (isReceivable) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = if (isPaid) StatusGreen else if (isReceivable) StatusRed else AmberAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(text = debt.debtorName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                                            Text(
                                                text = if (isReceivable) (if (isAr) "دين على عميل (لنا)" else "Receivable (From Client)") else (if (isAr) "مستحق لمورد (علينا)" else "Payable (To Supplier)"),
                                                fontSize = 10.sp,
                                                color = if (isReceivable) StatusRed else AmberAccent,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = LanguageManager.formatMoney(debt.remainingAmount, currency, isAr),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isPaid) StatusGreen else if (isReceivable) RoseDebt else AmberAccent
                                        )
                                        Text(
                                            text = if (isPaid) (if (isAr) "مسدد بالكامل ✓" else "Fully Paid ✓") else (if (isAr) "متبقي" else "Remaining"),
                                            fontSize = 9.sp,
                                            color = if (isPaid) StatusGreen else Slate400
                                        )
                                    }
                                }

                                if (debt.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "ملاحظات: ${debt.notes}", fontSize = 11.sp, color = Slate500)
                                }

                                if (!isPaid) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المبلغ الأصلي: ${LanguageManager.formatMoney(debt.initialAmount, currency, isAr)}",
                                            fontSize = 10.sp,
                                            color = Slate400
                                        )
                                        FilledTonalButton(
                                            onClick = { selectedDebtForPayment = debt },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = StatusGreenBg,
                                                contentColor = StatusGreen
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAr) "تسديد دفعة" else "Pay Installment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Content 2: Account Transactions (سجل القيود اليومية)
            if (selectedTab == 2) {
                if (accountTransactions.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(if (isAr) "لا توجد قيود مسجلة" else "No transactions recorded", color = Slate400)
                            }
                        }
                    }
                } else {
                    items(accountTransactions) { tx ->
                        val isDeposit = tx.type == "DEPOSIT" || tx.type == "TRANSFER_IN"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(if (isDeposit) EmeraldBg else RoseBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isDeposit) EmeraldProfit else RoseDebt,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(text = tx.accountName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                        Text(text = "${tx.category} • ${tx.notes}", fontSize = 10.sp, color = Slate500)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isDeposit) "+" else "-"}${LanguageManager.formatMoney(tx.amount, currency, isAr)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDeposit) EmeraldProfit else RoseDebt
                                        )
                                        Text(
                                            text = "الرصيد بعدها: ${LanguageManager.formatMoney(tx.balanceAfter, currency, isAr)}",
                                            fontSize = 9.sp,
                                            color = Slate400
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val pdfFile = PdfInvoiceExportHelper.exportTransactionVoucherPdf(
                                                context = context,
                                                transaction = tx,
                                                currencySymbol = currency.code,
                                                isArabic = isAr
                                            )
                                            if (pdfFile != null) {
                                                PdfInvoiceExportHelper.sharePdf(context, pdfFile, if (isAr) "سند قيد مالي #${tx.id}" else "Voucher #${tx.id}")
                                            } else {
                                                Toast.makeText(context, if (isAr) "فشل إنشاء السند" else "Failed to generate voucher", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("btn_export_voucher_pdf_${tx.id}")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير سند PDF", tint = SkyPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("CASH") }
        var accountCurrency by remember { mutableStateOf(currency.code) }
        var initialBalance by remember { mutableStateOf("0") }
        var description by remember { mutableStateOf("") }
        var isDefault by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text(if (isAr) "إضافة صندوق / حساب مالي" else "Add Financial Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isAr) "اسم الحساب (مثال: صندوق فرع 2 / زين كاش)" else "Account Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Account Type Selector
                    Text(text = if (isAr) "نوع الحساب:" else "Account Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH" to "نقدي / كاش", "BANK" to "مصرفي", "WALLET" to "محفظة رقمية").forEach { (typeKey, label) ->
                            FilterChip(
                                selected = type == typeKey,
                                onClick = { type = typeKey },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Multi-Currency Selection for Account
                    Text(text = if (isAr) "عملة الحساب:" else "Account Currency:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "IQD" to if (isAr) "دينار عراقي (IQD)" else "Iraqi Dinar (IQD)",
                            "USD" to if (isAr) "دولار أمريكي (USD)" else "US Dollar ($)"
                        ).forEach { (currCode, label) ->
                            FilterChip(
                                selected = accountCurrency == currCode,
                                onClick = { accountCurrency = currCode },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    AmountInputField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = if (isAr) "الرصيد الافتتاحي" else "Initial Balance",
                        allowZero = true,
                        isAr = isAr,
                        currencySymbol = accountCurrency
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (isAr) "وصف الحساب أو الغرض" else "Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAr) "تعيين كحساب افتراضي للمبيعات" else "Set as default sales account", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم الحساب", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val bal = initialBalance.toDoubleOrNull() ?: 0.0
                        viewModel.addAccount(
                            name = name,
                            code = "10${accounts.size + 1}",
                            type = type,
                            balance = bal,
                            currency = accountCurrency,
                            isDefault = isDefault,
                            description = description
                        )
                        showAddAccountDialog = false
                        Toast.makeText(context, "تم حفظ الحساب المالي بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Deposit Dialog
    if (showDepositDialogFor != null) {
        val acc = showDepositDialogFor!!
        var amountInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDepositDialogFor = null },
            title = { Text(if (isAr) "إيداع نقدي في: ${acc.name}" else "Deposit Funds") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "الرصيد الحالي: ${acc.balance} ${acc.currency}", fontSize = 12.sp, color = Slate500)
                    AmountInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = if (isAr) "مبلغ الإيداع" else "Deposit Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = acc.currency
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(if (isAr) "بيان أو سبب الإيداع" else "Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = ValidationHelper.validateAmount(amountInput, allowZero = false, isAr = isAr)
                        if (validation is ValidationResult.Invalid) {
                            Toast.makeText(context, validation.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amount = (validation as ValidationResult.Valid).value
                        viewModel.depositToAccount(acc.id, amount, notesInput.ifBlank { "إيداع نقدي" })
                        showDepositDialogFor = null
                        Toast.makeText(context, "تم الإيداع بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "تأكيد الإيداع" else "Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialogFor = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Withdraw Dialog
    if (showWithdrawDialogFor != null) {
        val acc = showWithdrawDialogFor!!
        var amountInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWithdrawDialogFor = null },
            title = { Text(if (isAr) "سحب نقدي من: ${acc.name}" else "Withdraw Funds") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "الرصيد المتاح: ${acc.balance} ${acc.currency}", fontSize = 12.sp, color = Slate500)
                    AmountInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = if (isAr) "مبلغ السحب" else "Withdraw Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = acc.currency
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(if (isAr) "بيان أو سبب السحب" else "Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = ValidationHelper.validateAmount(amountInput, allowZero = false, isAr = isAr)
                        if (validation is ValidationResult.Invalid) {
                            Toast.makeText(context, validation.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amount = (validation as ValidationResult.Valid).value
                        viewModel.withdrawFromAccount(acc.id, amount, notesInput.ifBlank { "سحب نقدي" })
                        showWithdrawDialogFor = null
                        Toast.makeText(context, "تم السحب بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "تأكيد السحب" else "Withdraw")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialogFor = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Transfer Between Accounts Dialog
    if (showTransferDialog) {
        var fromAccountIndex by remember { mutableIntStateOf(0) }
        var toAccountIndex by remember { mutableIntStateOf(if (accounts.size > 1) 1 else 0) }
        var amountInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text(if (isAr) "تحويل مالي بين الحسابات" else "Transfer Funds") },
            text = {
                if (accounts.size < 2) {
                    Text(if (isAr) "يجب توفر حسابين ماليين على الأقل لإجراء التحويل." else "At least two accounts needed to transfer.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "من حساب: ${accounts.getOrNull(fromAccountIndex)?.name ?: ""}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            accounts.forEachIndexed { idx, acc ->
                                FilterChip(
                                    selected = fromAccountIndex == idx,
                                    onClick = { fromAccountIndex = idx },
                                    label = { Text(acc.name.take(12), fontSize = 10.sp) }
                                )
                            }
                        }

                        Text(text = "إلى حساب: ${accounts.getOrNull(toAccountIndex)?.name ?: ""}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            accounts.forEachIndexed { idx, acc ->
                                FilterChip(
                                    selected = toAccountIndex == idx,
                                    onClick = { toAccountIndex = idx },
                                    label = { Text(acc.name.take(12), fontSize = 10.sp) }
                                )
                            }
                        }

                        AmountInputField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = if (isAr) "مبلغ التحويل" else "Transfer Amount",
                            allowZero = false,
                            isAr = isAr,
                            currencySymbol = currency.code
                        )
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text(if (isAr) "ملاحظات التحويل" else "Notes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (accounts.size >= 2) {
                    Button(
                        onClick = {
                            if (fromAccountIndex == toAccountIndex) {
                                Toast.makeText(context, "لا يمكن التحويل لنفس الحساب", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val validation = ValidationHelper.validateAmount(amountInput, allowZero = false, isAr = isAr)
                            if (validation is ValidationResult.Invalid) {
                                Toast.makeText(context, validation.message, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val amount = (validation as ValidationResult.Valid).value
                            val fromId = accounts[fromAccountIndex].id
                            val toId = accounts[toAccountIndex].id
                            viewModel.transferBetweenAccounts(fromId, toId, amount, notesInput.ifBlank { "تحويل مالي بين الصناديق" })
                            showTransferDialog = false
                            Toast.makeText(context, "تم التحويل بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(if (isAr) "تحويل" else "Transfer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Add Debt Dialog
    if (showAddDebtDialog) {
        var debtorName by remember { mutableStateOf("") }
        var debtorPhone by remember { mutableStateOf("") }
        var debtType by remember { mutableStateOf("RECEIVABLE") }
        var amountInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text(if (isAr) "تسجيل قيد دين جديد" else "Record New Debt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = debtType == "RECEIVABLE",
                            onClick = { debtType = "RECEIVABLE" },
                            label = { Text(if (isAr) "دين لنا على عميل" else "Client Debt (Owed to Us)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = debtType == "PAYABLE",
                            onClick = { debtType = "PAYABLE" },
                            label = { Text(if (isAr) "دين علينا لمورد" else "Supplier Debt (We Owe)", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = debtorName,
                        onValueChange = { debtorName = it },
                        label = { Text(if (isAr) "الاسم الكامل للمدين / الدائن" else "Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = debtorPhone,
                        onValueChange = { debtorPhone = it },
                        label = { Text(if (isAr) "رقم الهاتف" else "Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AmountInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = if (isAr) "مبلغ الدين" else "Debt Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(if (isAr) "تفاصيل أو سبب الدين" else "Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (debtorName.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة الاسم", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val validation = ValidationHelper.validateAmount(amountInput, allowZero = false, isAr = isAr)
                        if (validation is ValidationResult.Invalid) {
                            Toast.makeText(context, validation.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amount = (validation as ValidationResult.Valid).value
                        viewModel.addDebt(
                            debtorName = debtorName,
                            debtorPhone = debtorPhone,
                            debtType = debtType,
                            amount = amount,
                            dueDateMillis = System.currentTimeMillis() + 86400000L * 7,
                            notes = notesInput
                        )
                        showAddDebtDialog = false
                        Toast.makeText(context, "تم حفظ قيد الدين بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "حفظ الدين" else "Save Debt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Debt Payment Dialog
    if (selectedDebtForPayment != null) {
        val debt = selectedDebtForPayment!!
        var paymentAmountInput by remember { mutableStateOf(debt.remainingAmount.toInt().toString()) }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedDebtForPayment = null },
            title = { Text(if (isAr) "تسجيل دفعة سداد دين: ${debt.debtorName}" else "Record Debt Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "المبلغ المتبقي: ${debt.remainingAmount} ${debt.currency}", fontWeight = FontWeight.Bold, color = RoseDebt)
                    AmountInputField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = if (isAr) "المبلغ المسدد" else "Payment Amount",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = debt.currency
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(if (isAr) "طريقة السداد أو ملاحظات" else "Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        viewModel.recordDebtPayment(debt.id, amount, notesInput.ifBlank { "سداد دفعة دين" })
                        selectedDebtForPayment = null
                        Toast.makeText(context, "تم تسجيل السداد وتحديث الرصيد", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isAr) "تأكيد السداد" else "Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtForPayment = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
