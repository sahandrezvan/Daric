package com.example.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.DigitFormat
import com.example.core.model.TransactionType
import com.example.core.util.CurrencyFormatter
import com.example.core.util.InstallmentScheduleHelper
import com.example.core.util.JalaliCalendar
import com.example.data.local.entities.*
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private data class FinancialMonth(val title: String, val days: List<Long?>)
private data class DueItem(val installment: InstallmentEntity, val index: Int, val dueDate: Long, val isPaid: Boolean)

@Composable
fun CalendarViewScreen(
    transactions: List<TransactionEntity>,
    installments: List<InstallmentEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currency: String,
    digitFormat: DigitFormat,
    isShamsi: Boolean,
    onTransactionClick: (TransactionEntity) -> Unit,
    onInstallmentsClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = System.currentTimeMillis()
    var monthOffset by remember { mutableIntStateOf(0) }
    val month = remember(monthOffset, isShamsi) { buildMonth(today, monthOffset, isShamsi) }
    val initialSelected = remember(monthOffset, isShamsi) {
        month.days.firstOrNull { it != null && sameDay(it, today, isShamsi) }
            ?: month.days.firstNotNullOfOrNull { it }
            ?: today
    }
    var selectedDay by remember(monthOffset, isShamsi) { mutableLongStateOf(initialSelected) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val txByDay = remember(transactions, isShamsi) { transactions.groupBy { dayKey(it.timestamp, isShamsi) } }
    val dueByDay = remember(installments, isShamsi) {
        installments.flatMap { inst ->
            InstallmentScheduleHelper.generateSchedule(inst).map { item ->
                DueItem(inst, item.index, item.scheduledDueDate, item.isPaid)
            }
        }.groupBy { dayKey(it.dueDate, isShamsi) }
    }

    val selectedKey = dayKey(selectedDay, isShamsi)
    val selectedTransactions = txByDay[selectedKey].orEmpty()
    val selectedDues = dueByDay[selectedKey].orEmpty()
    val dayExpense = selectedTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val dayIncome = selectedTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("تقویم مالی", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("سررسید وام‌ها و تراکنش‌های روزانه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { monthOffset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل") }
                            Text(month.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            IconButton(onClick = { monthOffset++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد") }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { name ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        month.days.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                week.forEach { timestamp ->
                                    if (timestamp == null) {
                                        Spacer(Modifier.weight(1f).height(48.dp))
                                    } else {
                                        val key = dayKey(timestamp, isShamsi)
                                        val hasDue = dueByDay[key].orEmpty().any { !it.isPaid }
                                        val hasTx = txByDay[key].orEmpty().isNotEmpty()
                                        val selected = sameDay(timestamp, selectedDay, isShamsi)
                                        val isToday = sameDay(timestamp, today, isShamsi)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when {
                                                selected -> MaterialTheme.colorScheme.primary
                                                hasDue -> ExpenseRed.copy(alpha = 0.10f)
                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            border = if (hasDue && !selected) BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f)) else null,
                                            modifier = Modifier.weight(1f).height(48.dp).clickable { selectedDay = timestamp }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                Text(dayNumber(timestamp, isShamsi).toString(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    if (hasDue) Box(Modifier.size(5.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.onPrimary else ExpenseRed))
                                                    if (hasTx) Box(Modifier.size(5.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.onPrimary else IncomeGreen))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(JalaliCalendar.formatDate(selectedDay, isShamsi), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("${selectedDues.count { !it.isPaid }} قسط پرداخت‌نشده • ${selectedTransactions.size} تراکنش", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (selectedDues.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("اقساط این روز", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = onInstallmentsClick) { Text("مدیریت اقساط") }
                    }
                }
                items(selectedDues, key = { "${it.installment.id}-${it.index}" }) { due ->
                    val accent = if (due.isPaid) IncomeGreen else ExpenseRed
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onInstallmentsClick)
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = accent)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(due.installment.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("قسط ${due.index} از ${due.installment.totalInstallments}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(CurrencyFormatter.format(InstallmentScheduleHelper.amountFor(due.installment, due.index), currency, digitFormat), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(if (due.isPaid) "پرداخت شده" else "موعد پرداخت", color = accent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (selectedTransactions.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تراکنش‌های این روز", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("−${CurrencyFormatter.format(dayExpense, currency, digitFormat)}  +${CurrencyFormatter.format(dayIncome, currency, digitFormat)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(selectedTransactions, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        category = categoryMap[tx.categoryId],
                        accountName = accountMap[tx.accountId]?.name ?: "حساب",
                        destAccountName = tx.destinationAccountId?.let { accountMap[it]?.name },
                        currency = currency,
                        digitFormat = digitFormat,
                        isShamsi = isShamsi,
                        onClick = { onTransactionClick(tx) }
                    )
                }
            } else if (selectedDues.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                        Text("در این روز پرداخت یا تراکنشی ثبت نشده است.", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

private fun buildMonth(now: Long, offset: Int, isShamsi: Boolean): FinancialMonth {
    val days = mutableListOf<Long?>()
    if (isShamsi) {
        val current = JalaliCalendar.fromTimestamp(now)
        val total = current.year * 12 + current.month - 1 + offset
        val year = Math.floorDiv(total, 12)
        val month = Math.floorMod(total, 12) + 1
        val first = JalaliCalendar.toTimestamp(year, month, 1)
        val count = when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            else -> if (JalaliCalendar.fromTimestamp(JalaliCalendar.toTimestamp(year, 12, 30)).month == 12) 30 else 29
        }
        val firstWeekday = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply { timeInMillis = first }.get(Calendar.DAY_OF_WEEK) % 7
        repeat(firstWeekday) { days.add(null) }
        for (day in 1..count) days.add(JalaliCalendar.toTimestamp(year, month, day))
        while (days.size % 7 != 0) days.add(null)
        return FinancialMonth("${JalaliCalendar.getMonthName(month)} $year", days)
    }
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.MONTH, offset)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val first = cal.timeInMillis
    repeat(cal.get(Calendar.DAY_OF_WEEK) % 7) { days.add(null) }
    for (day in 1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
        days.add(Calendar.getInstance().apply { timeInMillis = first; set(Calendar.DAY_OF_MONTH, day) }.timeInMillis)
    }
    while (days.size % 7 != 0) days.add(null)
    return FinancialMonth(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time), days)
}

private fun dayKey(timestamp: Long, isShamsi: Boolean): Int {
    if (isShamsi) {
        val j = JalaliCalendar.fromTimestamp(timestamp)
        return j.year * 10000 + j.month * 100 + j.day
    }
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
}

private fun sameDay(a: Long, b: Long, isShamsi: Boolean) = dayKey(a, isShamsi) == dayKey(b, isShamsi)
private fun dayNumber(timestamp: Long, isShamsi: Boolean) = if (isShamsi) JalaliCalendar.fromTimestamp(timestamp).day else Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
