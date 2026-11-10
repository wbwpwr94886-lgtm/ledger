package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SystemUser
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.AppLanguage
import com.example.util.BackupExportHelper
import com.example.util.JsonBackupManager
import com.example.util.ParsedBackupPayload
import com.example.util.WhatsAppHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndCloudScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val activeUser by viewModel.activeUser.collectAsState()
    val systemUsers by viewModel.systemUsers.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val products by viewModel.products.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val customers by viewModel.customers.collectAsState()

    val isExporting by viewModel.isExportingBackup.collectAsState()
    val isRestoring by viewModel.isRestoringBackup.collectAsState()
    val localBackups by viewModel.localBackupsList.collectAsState()

    var destinationGmail by remember { mutableStateOf("") }
    var autoCloudBackupEnabled by remember { mutableStateOf(true) }
    var isCloudSyncing by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf("اليوم، منذ قليل") }
    var showAddUserDialog by remember { mutableStateOf(false) }

    var restorePreviewPayload by remember { mutableStateOf<ParsedBackupPayload?>(null) }
    var latestExportedFile by remember { mutableStateOf<File?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalBackups(context)
    }

    // File picker launcher for JSON backup import
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseBackupFromUri(
                context = context,
                uri = uri,
                onParsed = { payload ->
                    restorePreviewPayload = payload
                },
                onError = { errorMsg ->
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Developer & Technical Support Card (Featured prominently)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.company_logo),
                            contentDescription = "شعار النظام",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isAr) "الدعم الفني والبرمجي المعتمد" else "Authorized Technical Support",
                                fontSize = 12.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "أحمد فرحان السلماني",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "هاتف الدعم المباشر: 07811996908",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { WhatsAppHelper.contactDeveloperSupport(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "واتساب المطور" else "WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:07811996908"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White, contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "اتصال هاتفي" else "Direct Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Local Room Database JSON Backup & Export Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NavyPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = NavyPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAr) "النسخ الاحتياطي المحلي لقاعدة البيانات (JSON)" else "Local Room Database Backup (JSON)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAr) "تصدير كافة الحسابات والديون والفواتير والمخزون كملف مشفر وآمن" else "Export all accounts, debts, invoices & inventory as JSON",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Database contents count summary chips
                    Text(
                        text = if (isAr) "محتويات قاعدة البيانات الحالية الجاهزة للتصدير:" else "Current Database Records Ready for Export:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CountChip(
                            label = if (isAr) "صناديق وحسابات" else "Accounts",
                            count = accounts.size.toString(),
                            color = NavyPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        CountChip(
                            label = if (isAr) "سجلات ديون" else "Debts",
                            count = debts.size.toString(),
                            color = RoseDebt,
                            modifier = Modifier.weight(1f)
                        )
                        CountChip(
                            label = if (isAr) "فواتير بيع/شراء" else "Invoices",
                            count = invoices.size.toString(),
                            color = EmeraldProfit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CountChip(
                            label = if (isAr) "مواد ومخزون" else "Products",
                            count = products.size.toString(),
                            color = AmberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        CountChip(
                            label = if (isAr) "موردون" else "Suppliers",
                            count = suppliers.size.toString(),
                            color = Slate600,
                            modifier = Modifier.weight(1f)
                        )
                        CountChip(
                            label = if (isAr) "عملاء ومستفيدون" else "Customers",
                            count = customers.size.toString(),
                            color = SkyPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Export Button
                    Button(
                        onClick = {
                            viewModel.exportDatabaseToJsonFile(context) { success, file, msg ->
                                if (success && file != null) {
                                    latestExportedFile = file
                                    showExportSuccessDialog = true
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "جارِ تجميع البيانات وتصدير ملف JSON..." else "Exporting JSON Backup...")
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "تصدير نسخة احتياطية محلية الآن (ملف JSON)" else "Export Local Backup Now (.JSON)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Local JSON Backup Restore Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(GreenSuccess.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = GreenSuccess)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAr) "استعادة نسخة احتياطية (Import JSON)" else "Restore Database from JSON",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAr) "استيراد البيانات وإرجاعها بأمان لقاعدة بيانات Room" else "Import & restore data safely into Room database",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = AmberAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr)
                                    "يمكنك اختيار أي ملف نسخة احتياطية بصيغة JSON من جهازك أو من Google Drive واستعراض محتوياته وتأكيد الاستعادة قبل التطبيق."
                                else
                                    "Select any JSON backup file from your device or Google Drive. You can inspect all contents before confirming.",
                                fontSize = 12.sp,
                                color = Slate700
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            jsonFilePickerLauncher.launch("application/json")
                        },
                        enabled = !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, GreenSuccess),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(color = GreenSuccess, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "جارِ تطبيق الاستعادة في قاعدة البيانات..." else "Restoring to Room...")
                        } else {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "اختيار ملف JSON واستعادة البيانات" else "Pick JSON File & Restore Data",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Saved Local Backups List on this Device
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = Slate700, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "سجل النسخ الاحتياطية في ذاكرة التطبيق" else "Saved Local Backups on Device",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(onClick = { viewModel.refreshLocalBackups(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = NavyPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (localBackups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isAr) "لا توجد نسخ احتياطية محلية محفوظة حتى الآن" else "No local backups created yet",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    } else {
                        localBackups.forEach { file ->
                            val fileSizeKb = String.format(Locale.US, "%.1f KB", file.length() / 1024.0)
                            val modifiedDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate100),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Slate800,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "$modifiedDate • $fileSizeKb",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Quick restore
                                        IconButton(
                                            onClick = {
                                                viewModel.parseBackupFromFile(
                                                    file = file,
                                                    onParsed = { payload -> restorePreviewPayload = payload },
                                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = "استعادة", tint = GreenSuccess, modifier = Modifier.size(18.dp))
                                        }

                                        // Share file
                                        IconButton(
                                            onClick = { JsonBackupManager.shareBackupFile(context, file) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                        }

                                        // Delete file
                                        IconButton(
                                            onClick = { fileToDelete = file },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseDebt, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Cloud Backup & Transfer to Second Gmail
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = BlueInfo, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "النسخ الاحتياطي السحابي" else "Cloud Backup & Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Switch(
                            checked = autoCloudBackupEnabled,
                            onCheckedChange = {
                                autoCloudBackupEnabled = it
                                val msg = if (it) "تم تفعيل النسخ السحابي التلقائي اليومي" else "تم إيقاف النسخ التلقائي"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr) "آخر مزامنة سحابية: $lastSyncTime" else "Last Cloud Sync: $lastSyncTime",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isCloudSyncing = true
                            BackupExportHelper.performCloudBackup(context) { success: Boolean, msg: String ->
                                isCloudSyncing = false
                                lastSyncTime = "الآن بنجاح ✓"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueInfo),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isCloudSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isAr) "مزامنة ونسخ سحابي فوري الآن" else "Sync to Cloud Now")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Transfer to Another Gmail Account
                    Text(
                        text = if (isAr) "تحويل نسخة البيانات إلى حساب جيميل (Gmail) آخر:" else "Transfer Backup to another Gmail Account:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = destinationGmail,
                        onValueChange = { destinationGmail = it },
                        placeholder = { Text("example@gmail.com") },
                        label = { Text(if (isAr) "أدخل البريد الإلكتروني المستلم" else "Recipient Gmail") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (destinationGmail.isBlank() || !destinationGmail.contains("@")) {
                                Toast.makeText(context, "يرجى كتابة بريد Gmail صالح", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            BackupExportHelper.transferToAnotherGmail(context, destinationGmail.trim())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAr) "إرسال البيانات فورًا إلى الجيميل" else "Send to Gmail Now")
                    }
                }
            }
        }

        // 6. Multi-user & Role Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "تعدد المستخدمين وتحديد الصلاحيات" else "Multi-User & Permission Roles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(onClick = { showAddUserDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "إضافة موظف", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "المستخدم النشط حالياً: ${activeUser.displayName} (${activeUser.role})" else "Active User: ${activeUser.displayName}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    systemUsers.forEach { user ->
                        val isCurrent = user.id == activeUser.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "الدور: ${user.role} | صلاحيات: ${if (user.canViewReports) "تقارير✓ " else ""}${if (user.canManageDebts) "ديون✓ " else ""}${if (user.canEditProducts) "مخزون✓" else ""}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (isCurrent) {
                                    Surface(color = GreenSuccess, shape = RoundedCornerShape(6.dp)) {
                                        Text("نشط ✓", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.switchUser(user)
                                            Toast.makeText(context, "تم التحويل إلى: ${user.displayName}", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تبديل", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Dialog 1: Export Success Dialog with Share / Save options
    // =========================================================================
    if (showExportSuccessDialog && latestExportedFile != null) {
        val file = latestExportedFile!!
        val fileSize = String.format(Locale.US, "%.1f KB", file.length() / 1024.0)

        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(36.dp)) },
            title = { Text(if (isAr) "تم إنشاء النسخة الاحتياطية بنجاح!" else "Backup Created Successfully!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAr) "تم تصدير وحفظ كامل بيانات النظام في ملف JSON محلي آمن داخل الجهاز." else "Complete system data has been exported into a local JSON backup file.",
                        fontSize = 13.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "اسم الملف: ${file.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                            Text(text = "الحجم: $fileSize", fontSize = 11.sp, color = Slate600)
                            Text(text = "المسار: ${file.parent}", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        JsonBackupManager.shareBackupFile(context, file)
                        showExportSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAr) "مشاركة / حفظ في الهاتف" else "Share / Save File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportSuccessDialog = false }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            }
        )
    }

    // =========================================================================
    // Dialog 2: Restore Preview & Confirmation Dialog
    // =========================================================================
    restorePreviewPayload?.let { payload ->
        val summary = payload.summary

        AlertDialog(
            onDismissRequest = { restorePreviewPayload = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (isAr) "معاينة وتأكيد استعادة النسخة الاحتياطية" else "Confirm Database Restore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isAr)
                            "تاريخ إنشاء النسخة: ${summary.exportDateFormatted}"
                        else
                            "Backup Date: ${summary.exportDateFormatted}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )

                    Text(
                        text = if (isAr) "محتويات ملف النسخة الجاهزة للاستعادة:" else "Records in Backup File:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )

                    // Breakdown of records in this backup
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            RestoreDetailRow(if (isAr) "💳 الحسابات والصناديق:" else "Accounts:", "${payload.accounts.size}")
                            RestoreDetailRow(if (isAr) "⚖️ سجل الديون والذمم:" else "Debt Records:", "${payload.debts.size}")
                            RestoreDetailRow(if (isAr) "🧾 فواتير البيع:" else "Invoices:", "${payload.invoices.size}")
                            RestoreDetailRow(if (isAr) "📦 المواد والمنتجات:" else "Products:", "${payload.products.size}")
                            RestoreDetailRow(if (isAr) "🏭 الموردون:" else "Suppliers:", "${payload.suppliers.size}")
                            RestoreDetailRow(if (isAr) "👥 العملاء:" else "Customers:", "${payload.customers.size}")
                            RestoreDetailRow(if (isAr) "⚡ صرفيات المولد:" else "Generator Expenses:", "${payload.generatorExpenses.size}")
                        }
                    }

                    Surface(
                        color = RoseDebt.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RoseDebt, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr)
                                    "تنبيه: سيتم استبدال البيانات الحالية في قاعدة بيانات Room بالبيانات المستوردة بأمان."
                                else
                                    "Warning: Existing Room database records will be replaced with imported records safely in a transaction.",
                                fontSize = 11.sp,
                                color = RoseDebt,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentPayload = payload
                        restorePreviewPayload = null
                        viewModel.restoreBackupPayload(currentPayload, context) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAr) "تأكيد واستعادة البيانات الآن" else "Confirm & Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { restorePreviewPayload = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // =========================================================================
    // Dialog 3: Delete Local Backup File Confirmation
    // =========================================================================
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseDebt) },
            title = { Text(if (isAr) "حذف النسخة الاحتياطية" else "Delete Backup File") },
            text = {
                Text(
                    text = if (isAr)
                        "هل أنت متأكد من رغبتك في حذف ملف النسخة الاحتياطية (${file.name})؟"
                    else
                        "Are you sure you want to delete backup file (${file.name})?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLocalBackup(context, file)
                        fileToDelete = null
                        Toast.makeText(context, "تم حذف الملف بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDebt)
                ) {
                    Text(if (isAr) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // =========================================================================
    // Dialog 4: Add Employee Dialog
    // =========================================================================
    if (showAddUserDialog) {
        var name by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("CASHIER") }
        var canReports by remember { mutableStateOf(false) }
        var canDebts by remember { mutableStateOf(true) }
        var canProducts by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text(if (isAr) "إضافة موظف / مستخدم جديد" else "Add Employee") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الكامل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم الدخول (Username)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("الصلاحيات الممنوحة للموظف:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canDebts, onCheckedChange = { canDebts = it })
                        Text("إدارة الديون وسندات القبض", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canProducts, onCheckedChange = { canProducts = it })
                        Text("تعديل وإضافة منتجات المخزون", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canReports, onCheckedChange = { canReports = it })
                        Text("الاطلاع على الأرباح والتقارير المالية", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || username.isBlank()) {
                            Toast.makeText(context, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addUser(
                            SystemUser(
                                username = username,
                                displayName = name,
                                role = role,
                                canViewReports = canReports,
                                canEditProducts = canProducts,
                                canManageDebts = canDebts,
                                canExportBackup = false,
                                canManageUsers = false
                            )
                        )
                        showAddUserDialog = false
                        Toast.makeText(context, "تمت إضافة الموظف بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun CountChip(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            Text(text = label, fontSize = 10.sp, color = Slate600, maxLines = 1)
        }
    }
}

@Composable
private fun RestoreDetailRow(label: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Slate700)
        Text(text = count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }
}
