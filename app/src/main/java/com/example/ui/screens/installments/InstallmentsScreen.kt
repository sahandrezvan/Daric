package com.example.ui.screens.installments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DigitFormat
import com.example.core.model.InstallmentStatus
import com.example.core.util.CurrencyFormatter
import com.example.core.util.InstallmentScheduleHelper
import com.example.core.util.InstallmentTaskItem
import com.example.core.util.JalaliCalendar
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.InstallmentEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

enum class DueStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_SOON,
    FUTURE,
    COMPLETED
}

fun calculateDueStatus(inst: InstallmentEntity): Pair<DueStatus, Int> {
    if (inst.paidInstallments >= inst.totalInstallments || inst.status == InstallmentStatus.PAID) {
        return Pair(DueStatus.COMPLETED, 0)
    }

    val now = System.currentTimeMillis()
    val diffMillis = inst.firstDueDate - now
    val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

    return when {
        diffMillis < -24 * 3600 * 1000 -> Pair(DueStatus.OVERDUE, diffDays)
        diffDays == 0 || (diffMillis in 0..(24 * 3600 * 1000)) -> Pair(DueStatus.DUE_TODAY, 0)
        diffDays in 1..3 -> Pair(DueStatus.DUE_SOON, diffDays)
        else -> Pair(DueStatus.FUTURE, diffDays)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(
    installments: List<InstallmentEntity>,
    accounts: List<AccountEntity>,
    currency: String,
    digitFormat: DigitFormat,
    onAddInstallment: (title: String, totalAmount: Long, totalInstallments: Int, paidInstallments: Int, installmentAmount: Long, firstDueDate: Long, accountId: Long, note: String) -> Unit,
    onPayInstallment: (Long) -> Unit,
    onToggleInstallmentItem: (installmentId: Long, itemIndex: Int, isPaid: Boolean, deduct: Boolean) -> Unit = { _, _, _, _ -> },
    onDeleteInstallment: (Long) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedInstallmentId by remember { mutableLongStateOf(-1L) }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: همه, 1: در حال پرداخت, 2: تسویه شده

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val activeCount = installments.count { it.status != InstallmentStatus.PAID && it.paidInstallments < it.totalInstallments }
    val completedCount = installments.size - activeCount
    val totalMonthlyCommitment = installments
        .filter { it.status != InstallmentStatus.PAID && it.paidInstallments < it.totalInstallments }
        .sumOf { it.installmentAmount }

    val dueTodayCount = installments.count {
        val (st, _) = calculateDueStatus(it)
        st == DueStatus.DUE_TODAY || st == DueStatus.OVERDUE
    }

    val filteredInstallments = remember(installments, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> installments.filter { it.status != InstallmentStatus.PAID && it.paidInstallments < it.totalInstallments }
            2 -> installments.filter { it.status == InstallmentStatus.PAID || it.paidInstallments >= it.totalInstallments }
            else -> installments
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اقساط و وام‌ها",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "ثبت قسط جدید",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "ثبت قسط")
            }
        }
    ) { innerPadding ->
        if (installments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "هنوز هیچ قسط یا وامی ثبت نکرده‌اید",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مثلاً وام ۲۴ ماهه بانک ملت را اضافه کنید تا چک‌لیست ۲۴ قسط همراه با موعد سررسید برایتان ساخته شود و با هر پرداخت بتوانید تیک بزنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت اولین قسط یا وام")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Summary Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "تعهد کل ماهانه اقساط",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(totalMonthlyCommitment, currency, digitFormat),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$activeCount وام فعال در حال بازپرداخت",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (dueTodayCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ExpenseRed.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$dueTodayCount سررسید امروز!",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "همه (${installments.size})",
                            1 to "در حال پرداخت ($activeCount)",
                            2 to "تسویه شده ($completedCount)"
                        ).forEach { (tabIndex, title) ->
                            val isSelected = selectedFilterTab == tabIndex
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFilterTab = tabIndex }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Installments List
                items(filteredInstallments, key = { it.id }) { inst ->
                    val isExpanded = expandedInstallmentId == inst.id
                    val (dueStatus, diffDays) = calculateDueStatus(inst)
                    val isDone = dueStatus == DueStatus.COMPLETED
                    val acc = accountMap[inst.accountId]
                    val progress = if (inst.totalInstallments > 0) (inst.paidInstallments.toFloat() / inst.totalInstallments.toFloat()) else 0f
                    val formattedDueDate = JalaliCalendar.formatDate(inst.firstDueDate, isShamsi = true)

                    val statusColor = when (dueStatus) {
                        DueStatus.OVERDUE -> ExpenseRed
                        DueStatus.DUE_TODAY -> ExpenseRed
                        DueStatus.DUE_SOON -> Color(0xFFFFA000)
                        DueStatus.FUTURE -> MaterialTheme.colorScheme.primary
                        DueStatus.COMPLETED -> IncomeGreen
                    }

                    val statusText = when (dueStatus) {
                        DueStatus.OVERDUE -> "⚠️ سررسید گذشته (تأخیر در پرداخت)"
                        DueStatus.DUE_TODAY -> "🔔 موعد سررسید امروز فرا رسیده است"
                        DueStatus.DUE_SOON -> "⚡ موعد سررسید نزدیک است ($diffDays روز مانده)"
                        DueStatus.FUTURE -> "📅 $diffDays روز تا سررسید"
                        DueStatus.COMPLETED -> "✓ تمام اقساط تسویه شد"
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            width = if (dueStatus == DueStatus.DUE_TODAY || dueStatus == DueStatus.OVERDUE) 1.5.dp else 1.dp,
                            color = if (dueStatus == DueStatus.DUE_TODAY || dueStatus == DueStatus.OVERDUE) ExpenseRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Title, Due Status Icon, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(statusColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else if (dueStatus == DueStatus.DUE_TODAY || dueStatus == DueStatus.OVERDUE) Icons.Default.NotificationsActive else Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = inst.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "حساب کسر: ${acc?.name ?: "حساب اصلی"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteInstallment(inst.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف قسط",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Due Status Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusColor.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Today,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (!isDone) "موعد قسط بعدی: $formattedDueDate" else "تسویه کامل",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Key Figures
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "مبلغ هر قسط",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(inst.installmentAmount, currency, digitFormat),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "وضعیت اقساط",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${inst.paidInstallments} از ${inst.totalInstallments} قسط (${inst.totalInstallments - inst.paidInstallments} قسط مانده)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isDone) IncomeGreen else MaterialTheme.colorScheme.primary)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Toggle Checklist Button ("۲۴ تیک مثل تسک")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedInstallmentId = if (isExpanded) -1L else inst.id
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "چک‌لیست ${inst.totalInstallments} قسط (تیک پرداخت)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${inst.paidInstallments}/${inst.totalInstallments} تیک",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Expanded Task Checklist
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                InstallmentTasksChecklist(
                                    installment = inst,
                                    currency = currency,
                                    digitFormat = digitFormat,
                                    onToggleItem = { itemIndex, checked ->
                                        onToggleInstallmentItem(inst.id, itemIndex, checked, true)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add Installment Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var installmentAmountInput by remember { mutableStateOf("") }
        var totalCountInput by remember { mutableStateOf("24") } // Default to 24-month loan as requested
        var paidCountInput by remember { mutableStateOf("0") }
        var selectedDueDaysAhead by remember { mutableIntStateOf(0) }
        var selectedAccId by remember {
            mutableLongStateOf(accounts.firstOrNull()?.id ?: 1L)
        }
        var accDropdownExpanded by remember { mutableStateOf(false) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "ثبت وام و اقساط",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // موضوع وام و قسط
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("موضوع وام یا قسط") },
                        placeholder = { Text("مثلاً وام بانک ملت، قسط خرید خودرو") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // مبلغ هر قسط
                    OutlinedTextField(
                        value = installmentAmountInput,
                        onValueChange = { installmentAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ هر قسط ($currency)") },
                        placeholder = { Text("مثلاً ۲۵۰۰۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // تعداد کل اقساط و دکمه‌های سریع
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = totalCountInput,
                                onValueChange = { totalCountInput = it.filter { c -> c.isDigit() } },
                                label = { Text("تعداد کل اقساط") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = paidCountInput,
                                onValueChange = { paidCountInput = it.filter { c -> c.isDigit() } },
                                label = { Text("پرداخت شده") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Month Chips (6, 12, 24, 36)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("6", "12", "24", "36").forEach { monthStr ->
                                val isSelected = totalCountInput == monthStr
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { totalCountInput = monthStr }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$monthStr ماهه",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // تاریخ سررسید قسط نخست
                    Text(
                        text = "موعد سررسید قسط نخست:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to "امروز",
                            5 to "۵ روز بعد",
                            15 to "۱۵ روز بعد",
                            30 to "۱ ماه بعد"
                        ).forEach { (days, label) ->
                            val isSelected = selectedDueDaysAhead == days
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDueDaysAhead = days }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // حساب کسر اقساط
                    ExposedDropdownMenuBox(
                        expanded = accDropdownExpanded,
                        onExpandedChange = { accDropdownExpanded = !accDropdownExpanded }
                    ) {
                        val accName = accounts.find { it.id == selectedAccId }?.name ?: "انتخاب حساب"
                        OutlinedTextField(
                            value = accName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("حساب کسر قسط") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accDropdownExpanded,
                            onDismissRequest = { accDropdownExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        selectedAccId = acc.id
                                        accDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // یادداشت
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("یادداشت و شماره قرارداد (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val instAmt = installmentAmountInput.toLongOrNull() ?: 0L
                        val totalInst = totalCountInput.toIntOrNull() ?: 12
                        val paidInst = paidCountInput.toIntOrNull() ?: 0
                        val totalAmt = instAmt * totalInst
                        val dueDate = System.currentTimeMillis() + (selectedDueDaysAhead * 24L * 3600 * 1000)

                        if (title.isNotBlank() && instAmt > 0 && totalInst > 0) {
                            onAddInstallment(
                                title,
                                totalAmt,
                                totalInst,
                                paidInst,
                                instAmt,
                                dueDate,
                                selectedAccId,
                                note
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = title.isNotBlank() && (installmentAmountInput.toLongOrNull() ?: 0L) > 0
                ) {
                    Text("ایجاد قسط و ساخت تسک‌ها")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

/**
 * Task-like checklist showing all N installments with individual checkboxes and payment dates
 */
@Composable
private fun InstallmentTasksChecklist(
    installment: InstallmentEntity,
    currency: String,
    digitFormat: DigitFormat,
    onToggleItem: (itemIndex: Int, checked: Boolean) -> Unit
) {
    var taskFilter by remember { mutableIntStateOf(0) } // 0: همه, 1: پرداخت نشده, 2: پرداخت شده

    val scheduleItems = remember(installment) {
        InstallmentScheduleHelper.generateSchedule(installment)
    }

    val paidCount = scheduleItems.count { it.isPaid }
    val unpaidCount = scheduleItems.size - paidCount

    val filteredItems = remember(scheduleItems, taskFilter) {
        when (taskFilter) {
            1 -> scheduleItems.filter { !it.isPaid }
            2 -> scheduleItems.filter { it.isPaid }
            else -> scheduleItems
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Filter row inside checklist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0 to "همه (${scheduleItems.size})",
                1 to "پرداخت نشده ($unpaidCount)",
                2 to "پرداخت شده ($paidCount)"
            ).forEach { (fIndex, title) ->
                val isSelected = taskFilter == fIndex
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { taskFilter = fIndex }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tasks list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredItems.forEach { item ->
                val isOverdue = !item.isPaid && (item.scheduledDueDate < System.currentTimeMillis() - 24 * 3600 * 1000)
                val isToday = !item.isPaid && JalaliCalendar.isToday(item.scheduledDueDate)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isToday || isOverdue) 1.2.dp else 1.dp,
                        color = when {
                            item.isPaid -> IncomeGreen.copy(alpha = 0.35f)
                            isOverdue -> ExpenseRed.copy(alpha = 0.5f)
                            isToday -> ExpenseRed.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleItem(item.index, !item.isPaid) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox ("تیک مثل تسک")
                        Checkbox(
                            checked = item.isPaid,
                            onCheckedChange = { isChecked ->
                                onToggleItem(item.index, isChecked)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IncomeGreen,
                                uncheckedColor = if (isToday || isOverdue) ExpenseRed else MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Task Details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "قسط شماره ${item.index} از ${installment.totalInstallments}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (item.isPaid) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (item.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
                                )

                                if (isToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ExpenseRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "امروز",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ExpenseRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isOverdue) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ExpenseRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "معوق",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ExpenseRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Scheduled Date
                            Text(
                                text = "📅 موعد سررسید: ${JalaliCalendar.formatDate(item.scheduledDueDate, isShamsi = true)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Paid Date if completed
                            if (item.isPaid) {
                                val paidDateStr = if (item.paidDate != null && item.paidDate > 0) {
                                    JalaliCalendar.formatDate(item.paidDate, isShamsi = true)
                                } else {
                                    "ثبت شده"
                                }
                                Text(
                                    text = "✓ پرداخت شده در $paidDateStr",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = IncomeGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Amount & Status Badge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.format(installment.installmentAmount, currency, digitFormat),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (item.isPaid) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (item.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isPaid) IncomeGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (item.isPaid) "پرداخت شده" else "در انتظار",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (item.isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
