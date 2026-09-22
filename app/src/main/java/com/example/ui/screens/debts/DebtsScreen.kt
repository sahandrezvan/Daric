package com.example.ui.screens.debts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.model.DebtType
import com.example.core.model.DigitFormat
import com.example.core.util.CurrencyFormatter
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.DebtEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    debts: List<DebtEntity>,
    accounts: List<AccountEntity>,
    currency: String,
    digitFormat: DigitFormat,
    onAddDebt: (personName: String, type: DebtType, amount: Long, dueDate: Long?, note: String) -> Unit,
    onRecordPayment: (debtId: Long, amount: Long, accountId: Long?) -> Unit,
    onDeleteDebt: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Creditor (طلب‌های من), 1: Debtor (بدهی‌های من)
    var showAddDialog by remember { mutableStateOf(false) }
    var payDebtTarget by remember { mutableStateOf<DebtEntity?>(null) }

    val activeType = if (selectedTab == 0) DebtType.CREDITOR else DebtType.DEBTOR
    val filteredDebts = debts.filter { it.type == activeType }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
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
                        text = "طلب و بدهی",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("طلب‌های من (بستانکار)") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("بدهی‌های من (بدهکار)") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن قرض")
            }
        }
    ) { innerPadding ->
        if (filteredDebts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = if (selectedTab == 0) "طلب ثبت‌شده‌ای ندارید" else "بدهی ثبت‌شده‌ای ندارید",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "برای ثبت طلب یا بدهی به اشخاص و دوستان روی دکمه + بزنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredDebts, key = { it.id }) { d ->
                    val isDone = d.isSettled || d.paidAmount >= d.amount
                    val remaining = (d.amount - d.paidAmount).coerceAtLeast(0L)
                    val themeColor = if (d.type == DebtType.CREDITOR) IncomeGreen else ExpenseRed

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = d.personName,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isDone) "تسویه شد" else "مانده: ${CurrencyFormatter.format(remaining, currency, digitFormat)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteDebt(d.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "مبلغ کل: ${CurrencyFormatter.format(d.amount, currency, digitFormat)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "پرداخت شده: ${CurrencyFormatter.format(d.paidAmount, currency, digitFormat, false)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isDone) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { payDebtTarget = d },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (d.type == DebtType.CREDITOR) "ثبت دریافت طلب" else "ثبت پرداخت بدهی")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Add Debt Dialog
    if (showAddDialog) {
        var personName by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (selectedTab == 0) "ثبت طلب جدید" else "ثبت بدهی جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("نام طرف حساب / شخص") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("توضیحات یا بابت (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toLongOrNull() ?: 0L
                        if (personName.isNotBlank() && amount > 0L) {
                            onAddDebt(
                                personName,
                                activeType,
                                amount,
                                null,
                                note
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = personName.isNotBlank() && (amountInput.toLongOrNull() ?: 0L) > 0L
                ) {
                    Text("ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Pay Debt Dialog
    payDebtTarget?.let { target ->
        var payAmountInput by remember { mutableStateOf("") }
        var selectedAccId by remember {
            mutableLongStateOf(accounts.firstOrNull()?.id ?: 1L)
        }
        var accDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { payDebtTarget = null },
            title = { Text("ثبت تسویه با ${target.personName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = payAmountInput,
                        onValueChange = { payAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ پرداختی / دریافتی ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = accDropdownExpanded,
                        onExpandedChange = { accDropdownExpanded = !accDropdownExpanded }
                    ) {
                        val accName = accounts.find { it.id == selectedAccId }?.name ?: "انتخاب حساب"
                        OutlinedTextField(
                            value = accName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (target.type == DebtType.CREDITOR) "واریز به حساب" else "برداشت از حساب") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = accDropdownExpanded,
                            onDismissRequest = { accDropdownExpanded = false }
                        ) {
                            accounts.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a.name) },
                                    onClick = {
                                        selectedAccId = a.id
                                        accDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmountInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            onRecordPayment(target.id, amount, selectedAccId)
                            payDebtTarget = null
                        }
                    },
                    enabled = (payAmountInput.toLongOrNull() ?: 0L) > 0L
                ) {
                    Text("ثبت تسویه")
                }
            },
            dismissButton = {
                TextButton(onClick = { payDebtTarget = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}
