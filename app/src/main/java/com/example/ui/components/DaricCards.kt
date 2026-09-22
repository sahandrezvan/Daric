package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DigitFormat
import com.example.core.model.TransactionType
import com.example.core.util.CurrencyFormatter
import com.example.core.util.JalaliCalendar
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber

@Composable
fun NetWorthCard(
    netWorth: Long,
    monthlyChangePercent: Double,
    currency: String,
    digitFormat: DigitFormat,
    modifier: Modifier = Modifier
) {
    var isHidden by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "دارایی خالص",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { isHidden = !isHidden },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "مخفی‌سازی موجودی",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                val isPositive = monthlyChangePercent >= 0
                val badgeColor = if (isPositive) IncomeGreen else ExpenseRed
                val sign = if (isPositive) "+" else ""

                Text(
                    text = "$sign${CurrencyFormatter.formatPercentage(monthlyChangePercent, digitFormat)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val displayAmount = if (isHidden) {
                "•••••••• $currency"
            } else {
                CurrencyFormatter.format(netWorth, currency, digitFormat, includeCurrency = true)
            }

            Text(
                text = displayAmount,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("net_worth_amount")
            )
        }
    }
}

@Composable
fun MonthlySummaryCard(
    income: Long,
    expense: Long,
    savings: Long,
    currency: String,
    digitFormat: DigitFormat,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryColumn("درآمد", income, currency, digitFormat, IncomeGreen)
            SummaryColumn("هزینه", expense, currency, digitFormat, ExpenseRed)
            SummaryColumn(
                "پس‌انداز",
                savings,
                currency,
                digitFormat,
                if (savings >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: Long,
    currency: String,
    digitFormat: DigitFormat,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = CurrencyFormatter.format(amount, currency, digitFormat, includeCurrency = false),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
        Text(
            text = currency,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FinancialMetricsGrid(
    income: Long,
    expense: Long,
    savings: Long,
    installmentsCommitment: Long,
    currency: String,
    digitFormat: DigitFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricMiniCard(
                title = "درآمد ماه",
                amount = income,
                currency = currency,
                digitFormat = digitFormat,
                color = IncomeGreen,
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier.weight(1f)
            )
            MetricMiniCard(
                title = "هزینه‌های ماه",
                amount = expense,
                currency = currency,
                digitFormat = digitFormat,
                color = ExpenseRed,
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val savingsColor = if (savings >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
            MetricMiniCard(
                title = "پس‌انداز خالص",
                amount = savings,
                currency = currency,
                digitFormat = digitFormat,
                color = savingsColor,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            MetricMiniCard(
                title = "اقساط این ماه",
                amount = installmentsCommitment,
                currency = currency,
                digitFormat = digitFormat,
                color = WarningAmber,
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    amount: Long,
    currency: String,
    digitFormat: DigitFormat,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = CurrencyFormatter.format(amount, currency, digitFormat),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onMarketRatesClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onGoalsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionItem(
            title = "بازار",
            icon = Icons.Default.SwapHoriz,
            onClick = onMarketRatesClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            title = "اقساط",
            icon = Icons.Default.AccountBalanceWallet,
            onClick = onInstallmentsClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            title = "اهداف",
            icon = Icons.Default.CheckCircle,
            onClick = onGoalsClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AccountCard(
    account: AccountEntity,
    currency: String,
    digitFormat: DigitFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountColor = Color(account.colorHex)

    Surface(
        modifier = modifier
            .width(158.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accountColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(account.iconName),
                        contentDescription = null,
                        tint = accountColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = account.type.titleFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.format(account.currentBalance, currency, digitFormat),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    accountName: String,
    destAccountName: String?,
    currency: String,
    digitFormat: DigitFormat,
    isShamsi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val amountColor = when {
        isExpense -> ExpenseRed
        isIncome -> IncomeGreen
        else -> TransferBlue
    }

    val amountPrefix = when {
        isExpense -> "−"
        isIncome -> "+"
        else -> "⇄"
    }

    val iconColor = category?.let { Color(it.colorHex) } ?: when {
        isExpense -> ExpenseRed
        isIncome -> IncomeGreen
        else -> TransferBlue
    }

    val iconVector = if (isTransfer) {
        Icons.Default.SwapHoriz
    } else {
        category?.let { CategoryIconHelper.getIcon(it.iconName) } ?: Icons.Default.AccountBalanceWallet
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val title = if (transaction.description.isNotBlank()) {
                transaction.description
            } else if (isTransfer) {
                "$accountName → ${destAccountName ?: ""}"
            } else {
                category?.nameFa ?: "تراکنش"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isTransfer) "انتقال حساب" else accountName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix ${CurrencyFormatter.format(transaction.amount, currency, digitFormat)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = JalaliCalendar.formatDate(transaction.timestamp, isShamsi = isShamsi),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
