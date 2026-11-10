package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.data.model.Supplier
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.AmountInputField
import com.example.util.ValidationHelper
import com.example.util.ValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val suppliers by viewModel.suppliers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPaySupplierDialogFor by remember { mutableStateOf<Supplier?>(null) }
    var payAmountInput by remember { mutableStateOf("") }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.company.contains(searchQuery, ignoreCase = true) }
    }

    val totalSupplierDebt = suppliers.sumOf { it.balanceOwed }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "إضافة مورد جديد")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            Text(if (isAr) "إجمالي مستحقات الموردين بذمتنا" else "Total Balance Owed to Suppliers", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LanguageManager.formatMoney(totalSupplierDebt, currency, isAr),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAlert
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${suppliers.size} مورد",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isAr) "ابحث باسم المورد أو الشركة..." else "Search supplier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredSuppliers.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = if (isAr) "لا يوجد موردين مسجلين" else "No suppliers found", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filteredSuppliers) { supplier ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(NavySecondary.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = NavySecondary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = supplier.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = supplier.company.ifBlank { "شركة توريد" }, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = LanguageManager.formatMoney(supplier.balanceOwed, currency, isAr),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (supplier.balanceOwed > 0) RedAlert else GreenSuccess
                                    )
                                    Text(
                                        text = if (isAr) "مستحق له" else "Owed",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        showPaySupplierDialogFor = supplier
                                        payAmountInput = supplier.balanceOwed.toInt().toString()
                                    },
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "تسديد دفعة للمورد" else "Pay Supplier", fontSize = 11.sp)
                                }

                                Row {
                                    if (supplier.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = BlueInfo)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSupplier(supplier)
                                            Toast.makeText(context, "تم حذف المورد", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(34.dp)
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

    // Add Supplier Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var initialBalance by remember { mutableStateOf("0") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isAr) "إضافة مورد جديد" else "Add New Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isAr) "اسم المندوب أو المورد" else "Supplier Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text(if (isAr) "اسم الشركة أو المستودع" else "Company Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isAr) "رقم الهاتف" else "Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AmountInputField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = if (isAr) "رصيد مستحق له سابق إن وجد" else "Initial Balance Owed",
                        allowZero = true,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isAr) "ملاحظات التوريد" else "Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم المورد", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val valRes = ValidationHelper.validateAmount(initialBalance, allowZero = true, isAr = isAr)
                        if (valRes is ValidationResult.Invalid) {
                            Toast.makeText(context, valRes.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val b = (valRes as ValidationResult.Valid).value
                        viewModel.addSupplier(
                            Supplier(
                                name = name,
                                phone = phone,
                                company = company,
                                balanceOwed = b,
                                notes = notes
                            )
                        )
                        showAddDialog = false
                        Toast.makeText(context, "تمت إضافة المورد بنجاح", Toast.LENGTH_SHORT).show()
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

    // Pay Supplier Dialog
    if (showPaySupplierDialogFor != null) {
        val supp = showPaySupplierDialogFor!!
        AlertDialog(
            onDismissRequest = { showPaySupplierDialogFor = null },
            title = { Text(if (isAr) "تسديد دفعة للمورد: ${supp.name}" else "Pay Supplier") },
            text = {
                Column {
                    Text(text = "المبلغ المتبقي له: ${supp.balanceOwed} ${currency.code}", color = RedAlert, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    AmountInputField(
                        value = payAmountInput,
                        onValueChange = { payAmountInput = it },
                        label = if (isAr) "المبلغ المسدد" else "Amount Paid",
                        allowZero = false,
                        isAr = isAr,
                        currencySymbol = currency.code
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valRes = ValidationHelper.validateAmount(payAmountInput, allowZero = false, isAr = isAr)
                        if (valRes is ValidationResult.Invalid) {
                            Toast.makeText(context, valRes.message, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amount = (valRes as ValidationResult.Valid).value
                        if (amount > 0) {
                            viewModel.updateSupplier(supp.copy(balanceOwed = maxOf(0.0, supp.balanceOwed - amount)))
                            Toast.makeText(context, "تم خصم الدفعة من حساب المورد", Toast.LENGTH_SHORT).show()
                            showPaySupplierDialogFor = null
                        }
                    }
                ) {
                    Text(if (isAr) "تأكيد التسديد" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaySupplierDialogFor = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
