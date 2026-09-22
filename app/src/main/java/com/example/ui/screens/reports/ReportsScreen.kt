package com.example.ui.screens.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.DigitFormat
import com.example.core.model.TransactionType
import com.example.core.util.CurrencyFormatter
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.TransactionEntity
import com.example.ui.components.ChartSlice
import com.example.ui.components.DonutChart
import com.example.ui.components.MonthlyBarComparison
import java.util.Calendar

@Composable
fun ReportsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: String,
    digitFormat: DigitFormat,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rangeFilter by remember { mutableIntStateOf(0) } // 0: This month, 1: Last 3 months, 2: All time

    val now = System.currentTimeMillis()
    val filterStartTime = when (rangeFilter) {
        0 -> {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.timeInMillis
        }
        1 -> now - 90L * 24 * 3600 * 1000
        else -> 0L
    }

    val filteredTransactions = transactions.filter { it.timestamp >= filterStartTime }
    val categoryMap = categories.associateBy { it.id }

    val totalIncome = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense

    val expenseSlices = filteredTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .map { (catId, txs) ->
            val cat = categoryMap[catId]
            val catTotal = txs.sumOf { it.amount }
            val catColor = cat?.let { Color(it.colorHex) } ?: Color.Gray
            val label = cat?.nameFa ?: "سایر"
            ChartSlice(label = label, value = catTotal, color = catColor)
        }
        .sortedByDescending { it.value }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "گزارش و نمودارها",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خروجی اکسل (CSV)")
                }
            }
        }

        // Time Range Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = rangeFilter == 0,
                    onClick = { rangeFilter = 0 },
                    label = { Text("این ماه") }
                )
                FilterChip(
                    selected = rangeFilter == 1,
                    onClick = { rangeFilter = 1 },
                    label = { Text("۳ ماه اخیر") }
                )
                FilterChip(
                    selected = rangeFilter == 2,
                    onClick = { rangeFilter = 2 },
                    label = { Text("همه زمان‌ها") }
                )
            }
        }

        // Income vs Expense Comparison Bar
        item {
            MonthlyBarComparison(
                income = totalIncome,
                expense = totalExpense,
                currency = currency,
                digitFormat = digitFormat
            )
        }

        // Donut Chart of Category Expenses
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفکیک هزینه‌ها بر اساس دسته‌بندی",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DonutChart(
                        slices = expenseSlices,
                        totalAmount = totalExpense,
                        currency = currency,
                        digitFormat = digitFormat
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
