package com.example.ui.screens.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.util.JalaliCalendar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    lastBackupTime: Long?,
    onExportJson: suspend () -> String,
    onRestoreJson: suspend (json: String, replaceAll: Boolean) -> Boolean,
    onResetAllData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showManualPasteDialog by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }
    var showCleanDataConfirmDialog by remember { mutableStateOf(false) }

    // Launcher for saving directly to phone storage (e.g. Downloads, Documents, or Drive)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isProcessing = true
                    val json = onExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    isProcessing = false
                    Toast.makeText(context, "فایل پشتیبان با موفقیت در حافظه گوشی ذخیره شد", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    isProcessing = false
                    Toast.makeText(context, "خطا در ذخیره فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for opening and restoring a backup JSON file from phone or Google Drive
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isProcessing = true
                    val jsonStr = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    isProcessing = false
                    if (!jsonStr.isNullOrBlank()) {
                        pendingRestoreJson = jsonStr
                        showRestoreConfirmDialog = true
                    } else {
                        Toast.makeText(context, "فایل انتخاب شده خالی است یا نامعتبر می‌باشد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isProcessing = false
                    Toast.makeText(context, "خطا در باز کردن فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function to share file directly to Google Drive (or other cloud/storage targets)
    fun shareBackupToGoogleDrive() {
        scope.launch {
            try {
                isProcessing = true
                val json = onExportJson()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val backupFile = File(context.cacheDir, "daric_backup_$timeStamp.json")
                backupFile.writeText(json, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "نسخه پشتیبان حسابداری داریک")
                    putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان شامل تمام اطلاعات مالی، حساب‌ها، اقساط و تراکنش‌ها")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "ذخیره در گوگل درایو یا اشتراک‌گذاری"))
                isProcessing = false
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "خطا در ایجاد فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ذخیره‌سازی و نسخه پشتیبان",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Local Storage Persistence Info Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "ذخیره دائمی در حافظه گوشی",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تمامی داده‌های شما (تراکنش‌ها، حساب‌ها، چک‌لیست اقساط و طلب‌ها) به صورت خودکار و دائمی در دیتابیس امن گوشی ذخیره شده و بدون اینترنت همیشه در دسترس است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                        if (lastBackupTime != null && lastBackupTime > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "آخرین پشتیبان‌گیری: ${JalaliCalendar.formatDate(lastBackupTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Section 1: Save to Phone Storage
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ذخیره در حافظه گوشی (دانلودها و فایل‌ها)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "یک فایل استاندارد پشتیبان (.json) شامل کلیه تراکنش‌ها، چک‌لیست ۲۴ ماهه اقساط و حساب‌ها در پوشه دلخواه شما (پوشه Downloads، Documents یا کارت حافظه) ذخیره می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            createDocumentLauncher.launch("daric_backup_$timeStamp.json")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ذخیره فایل در حافظه گوشی...")
                    }
                }
            }

            // Section 2: Save to Google Drive
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ذخیره در گوگل درایو (Google Drive)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ارسال و آپلود مستقیم فایل پشتیبان در Google Drive شخصی شما تا حتی با تعویض گوشی، اطلاعات حساب‌های شما همیشه محفوظ بماند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { shareBackupToGoogleDrive() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ارسال و ذخیره در Google Drive")
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "نکته: در منوی باز شده گزینه «ذخیره در درایو (Save to Drive)» را انتخاب کنید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 3: Restore Data from Phone or Google Drive
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "بازیابی اطلاعات از گوشی یا گوگل درایو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "فایل پشتیبان قبلی خود را از حافظه گوشی یا مستقیماً از Google Drive انتخاب کنید تا تمام حساب‌ها، اقساط و تراکنش‌های شما بازیابی شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("انتخاب فایل پشتیبان از حافظه یا Drive")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showManualPasteDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ورود دستی متن پشتیبان (از کلیپ‌بورد)")
                    }
                }
            }

            // Section 4: Clean Demo & Test Data for Official Release
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "پاکسازی داده‌های دمو و تستی برای انتشار",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "برای انتشار رسمی و شروع استفاده با داده‌های واقعی خودتان: تمام تراکنش‌ها، اقساط، چک‌لیست‌ها و داده‌های تستی را پاک کرده و حساب‌ها با موجودی صفر آماده می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCleanDataConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پاکسازی داده‌های تستی و شروع واقعی")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Dialog 1: Clean Data Confirmation
    if (showCleanDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanDataConfirmDialog = false },
            title = { Text("پاکسازی داده‌های تستی و دمو") },
            text = {
                Text(
                    "آیا مطمئن هستید؟ با تأیید، کلیه تراکنش‌ها، اقساط، طلب‌ها، اهداف و بودجه‌های آزمایشی به طور کامل پاک شده و موجودی حساب‌ها صفر خواهد شد تا برنامه آماده انتشار و ثبت داده‌های واقعی شما گردد."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanDataConfirmDialog = false
                        onResetAllData()
                        Toast.makeText(context, "تمام داده‌های آزمایشی پاک شدند. برنامه آماده ثبت اطلاعات واقعی شماست.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأیید و پاکسازی کامل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanDataConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog 2: Restore Confirmation Dialog (Replace vs Merge)
    if (showRestoreConfirmDialog && pendingRestoreJson != null) {
        var replaceAll by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("بازیابی اطلاعات پشتیبان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "فایل پشتیبان با موفقیت شناسایی شد. لطفاً نحوه بازیابی را انتخاب کنید:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = replaceAll,
                            onClick = { replaceAll = true }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جایگزینی کامل (داده‌های فعلی پاک و نسخه جدید اعمال شود)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = !replaceAll,
                            onClick = { replaceAll = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ادغام (افزودن به داده‌های موجود بدون حذف)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreJson
                        showRestoreConfirmDialog = false
                        if (json != null) {
                            scope.launch {
                                isProcessing = true
                                val success = onRestoreJson(json, replaceAll)
                                isProcessing = false
                                if (success) {
                                    Toast.makeText(context, "اطلاعات با موفقیت بازیابی شدند", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "خطا در پردازش فایل پشتیبان", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("بازیابی اطلاعات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog 3: Manual Paste Dialog
    if (showManualPasteDialog) {
        var replaceAll by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualPasteDialog = false },
            title = { Text("ورود دستی متن پشتیبان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "متن JSON پشتیبان را در کادر زیر وارد کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("متن JSON...") },
                        maxLines = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = replaceAll,
                            onCheckedChange = { replaceAll = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "جایگزینی کامل (حذف داده‌های فعلی)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualInputText.isNotBlank()) {
                            showManualPasteDialog = false
                            scope.launch {
                                isProcessing = true
                                val success = onRestoreJson(manualInputText, replaceAll)
                                isProcessing = false
                                if (success) {
                                    Toast.makeText(context, "اطلاعات با موفقیت بازیابی شد", Toast.LENGTH_LONG).show()
                                    manualInputText = ""
                                } else {
                                    Toast.makeText(context, "خطا در بازیابی: ساختار نامعتبر است", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = manualInputText.isNotBlank()
                ) {
                    Text("اعمال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualPasteDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
