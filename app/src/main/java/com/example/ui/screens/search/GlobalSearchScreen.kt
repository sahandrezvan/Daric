package com.example.ui.screens.search

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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.DigitFormat
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.TransactionEntity
import com.example.core.model.TransactionType
import com.example.ui.components.TransactionRowItem

@Composable
fun GlobalSearchScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currency: String,
    digitFormat: DigitFormat,
    isShamsi: Boolean,
    onTransactionClick: (TransactionEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var dayRange by remember { mutableStateOf<Int?>(null) }
    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }

    val hasFilter = typeFilter != null || dayRange != null
    val results = if (query.isBlank() && !hasFilter) {
        emptyList()
    } else {
        val q = query.trim().lowercase()
        transactions.filter { tx ->
            val textMatches = q.isBlank() || tx.description.lowercase().contains(q) ||
                    tx.note.lowercase().contains(q) ||
                    tx.tags.lowercase().contains(q) ||
                    (categoryMap[tx.categoryId]?.nameFa?.contains(q) == true) ||
                    (accountMap[tx.accountId]?.name?.lowercase()?.contains(q) == true) ||
                    tx.amount.toString().contains(q)
            val typeMatches = typeFilter == null || tx.type == typeFilter
            val dateMatches = dayRange == null || tx.timestamp >= System.currentTimeMillis() - dayRange!! * 86_400_000L
            textMatches && typeMatches && dateMatches
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
                Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجو در کل اطلاعات...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = typeFilter == null, onClick = { typeFilter = null }, label = { Text("همه") })
                    TransactionType.entries.forEach { type ->
                        FilterChip(selected = typeFilter == type, onClick = { typeFilter = type }, label = { Text(type.titleFa) })
                    }
                    FilterChip(selected = dayRange == 7, onClick = { dayRange = if (dayRange == 7) null else 7 }, label = { Text("۷ روز") })
                    FilterChip(selected = dayRange == 30, onClick = { dayRange = if (dayRange == 30) null else 30 }, label = { Text("۳۰ روز") })
                }
                }
            }
        }
    ) { innerPadding ->
        if (query.isBlank() && !hasFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "نام هزینه، شخص، دسته‌بندی، تگ یا مبلغ را تایپ کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ نتیجه‌ای مطابق «$query» یافت نشد",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${results.size} نتیجه پیدا شد",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(results, key = { it.id }) { tx ->
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
            }
        }
    }
}
