package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DigitFormat
import com.example.core.util.CurrencyFormatter
import com.example.core.util.JalaliCalendar
import com.example.core.util.InstallmentScheduleHelper
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.InstallmentEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.UserSettingsEntity
import com.example.ui.components.AccountCard
import com.example.ui.components.FinancialMetricsGrid
import com.example.ui.components.MonthlySummaryCard
import com.example.ui.components.NetWorthCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.screens.installments.DueStatus
import com.example.ui.screens.installments.calculateDueStatus
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@Composable
fun DashboardScreen(
    userSettings: UserSettingsEntity,
    accounts: List<AccountEntity>,
    recentTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    installments: List<InstallmentEntity> = emptyList(),
    netWorth: Long,
    monthlyIncome: Long,
    monthlyExpense: Long,
    onAccountClick: (AccountEntity) -> Unit,
    onAddAccountClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onViewAllTransactions: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onInstallmentsClick: () -> Unit = {},
    onMarketRatesClick: () -> Unit = {},
    onGoalsClick: () -> Unit = {},
    onPayInstallment: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val monthlySavings = monthlyIncome - monthlyExpense
    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }
    val isShamsi = userSettings.calendarType == com.example.core.model.CalendarType.SHAMSI
    val todayFormatted = JalaliCalendar.formatDate(System.currentTimeMillis(), isShamsi = isShamsi)
    val compact = userSettings.isCompactMode
    val sectionGap = if (compact) 10.dp else 20.dp
    val contentPad = if (compact) {
        PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    } else {
        PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    }

    // Filter installments that are due today, overdue, or due within 3 days
    val activeInstallments = installments.filter { it.paidInstallments < it.totalInstallments }
    val totalInstallmentMonthly = activeInstallments.sumOf { it.installmentAmount }
    val dueInstallments = activeInstallments.filter {
        val (status, _) = calculateDueStatus(it)
        status == DueStatus.DUE_TODAY || status == DueStatus.OVERDUE || status == DueStatus.DUE_SOON
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPad,
        verticalArrangement = Arrangement.spacedBy(sectionGap)
    ) {
        // 1. Top Bar: Greeting & Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (compact) 2.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (compact) userSettings.userName else "سلام، ${userSettings.userName}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!compact) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(if (compact) 36.dp else 40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .testTag("dashboard_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onCalendarClick,
                        modifier = Modifier
                            .size(if (compact) 36.dp else 40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .testTag("dashboard_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "تقویم",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Net Worth Card
        item {
            NetWorthCard(
                netWorth = netWorth,
                monthlyChangePercent = 0.0,
                currency = userSettings.currency,
                digitFormat = userSettings.digitFormat
            )
        }

        // 3. Only the nearest actionable installment is shown to keep Home calm.
        if (dueInstallments.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سررسید اقساط (نیاز به پرداخت)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }

                        TextButton(onClick = onInstallmentsClick) {
                            Text(
                                text = "همه اقساط",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    dueInstallments.sortedBy {
                        InstallmentScheduleHelper.nextUnpaid(it)?.scheduledDueDate ?: Long.MAX_VALUE
                    }.take(1).forEach { inst ->
                        val (status, diffDays) = calculateDueStatus(inst)
                        val nextItem = InstallmentScheduleHelper.nextUnpaid(inst)
                        val dueDateStr = JalaliCalendar.formatDate(nextItem?.scheduledDueDate ?: inst.firstDueDate, isShamsi = isShamsi)
                        val badgeText = when (status) {
                            DueStatus.OVERDUE -> "⚠️ سررسید گذشته ($diffDays روز)"
                            DueStatus.DUE_TODAY -> "🔔 سررسید امروز!"
                            DueStatus.DUE_SOON -> "⚡ $diffDays روز مانده"
                            else -> dueDateStr
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ExpenseRed.copy(alpha = 0.08f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = inst.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "مبلغ قسط: ${CurrencyFormatter.format(nextItem?.let { InstallmentScheduleHelper.amountFor(inst, it.index) } ?: inst.installmentAmount, userSettings.currency, userSettings.digitFormat)} (${nextItem?.index ?: inst.paidInstallments + 1}/${inst.totalInstallments})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ExpenseRed.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ExpenseRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onPayInstallment(inst.id) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "پرداخت این قسط", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeInstallments.isNotEmpty()) {
            // Upcoming installments quick card
            val nearest = activeInstallments.minByOrNull {
                InstallmentScheduleHelper.nextUnpaid(it)?.scheduledDueDate ?: Long.MAX_VALUE
            }
            if (nearest != null) {
                item {
                    val nearestItem = InstallmentScheduleHelper.nextUnpaid(nearest)
                    val formattedNextDate = JalaliCalendar.formatDate(nearestItem?.scheduledDueDate ?: nearest.firstDueDate, isShamsi = isShamsi)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInstallmentsClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "نزدیک‌ترین قسط: ${nearest.title}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تاریخ سررسید: $formattedNextDate | ${CurrencyFormatter.format(nearestItem?.let { InstallmentScheduleHelper.amountFor(nearest, it.index) } ?: nearest.installmentAmount, userSettings.currency, userSettings.digitFormat)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Financial Metrics Grid (2x2) — compact uses single summary row
        item {
            if (compact) {
                MonthlySummaryCard(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    savings = monthlySavings,
                    currency = userSettings.currency,
                    digitFormat = userSettings.digitFormat
                )
            } else {
                FinancialMetricsGrid(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    savings = monthlySavings,
                    installmentsCommitment = totalInstallmentMonthly,
                    currency = userSettings.currency,
                    digitFormat = userSettings.digitFormat
                )
            }
        }

        // 5. Accounts Header & Carousel
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حساب‌ها و کارت‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onAddAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حساب جدید",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (accounts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddAccountClick() }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "حسابی تعریف نشده است",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "برای وارد کردن حساب واقعی خود اینجا را لمس کنید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            AccountCard(
                                account = acc,
                                currency = userSettings.currency,
                                digitFormat = userSettings.digitFormat,
                                onClick = { onAccountClick(acc) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Recent Transactions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخرین تراکنش‌ها",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onViewAllTransactions) {
                    Text(
                        text = "مشاهده همه",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هنوز تراکنشی ثبت نشده است",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای ثبت درآمد، هزینه یا انتقال واقعی روی دکمه + در پایین بزنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(8), key = { it.id }) { tx ->
                val category = categoryMap[tx.categoryId]
                val account = accountMap[tx.accountId]
                val destAccount = tx.destinationAccountId?.let { accountMap[it] }

                TransactionRowItem(
                    transaction = tx,
                    category = category,
                    accountName = account?.name ?: "حساب",
                    destAccountName = destAccount?.name,
                    currency = userSettings.currency,
                    digitFormat = userSettings.digitFormat,
                    isShamsi = isShamsi,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }

        // Bottom spacer for FAB & Navigation bar clearance
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
