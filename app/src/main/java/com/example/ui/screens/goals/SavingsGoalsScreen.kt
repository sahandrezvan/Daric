package com.example.ui.screens.goals

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
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.core.model.DigitFormat
import com.example.core.util.CurrencyFormatter
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.SavingGoalEntity
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    goals: List<SavingGoalEntity>,
    accounts: List<AccountEntity>,
    currency: String,
    digitFormat: DigitFormat,
    onAddGoal: (title: String, targetAmount: Long, currentAmount: Long, targetDate: Long, colorHex: Long) -> Unit,
    onDepositGoal: (goalId: Long, amount: Long, accountId: Long) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var depositGoalTarget by remember { mutableStateOf<SavingGoalEntity?>(null) }

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
                    text = "اهداف پس‌انداز",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن هدف")
            }
        }
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = "هنوز هدفی برای پس‌انداز تعریف نشده است",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "برای خریدهای بزرگ، مسافرت، خودرو یا صندوق اضطراری هدف تعریف کنید تا روند رشد پس‌انداز را دنبال کنید.",
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
                items(goals, key = { it.id }) { g ->
                    val progress = if (g.targetAmount > 0) (g.currentAmount.toFloat() / g.targetAmount.toFloat()).coerceIn(0f, 1f) else 0f
                    val isDone = g.isCompleted || progress >= 1f

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
                                            .background(if (isDone) IncomeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = if (isDone) IncomeGreen else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = g.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isDone) "تکمیل شد!" else "${CurrencyFormatter.formatPercentage((progress * 100).toDouble(), digitFormat)} تکمیل شده",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteGoal(g.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف هدف",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Progress numbers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${CurrencyFormatter.format(g.currentAmount, currency, digitFormat, false)} از ${CurrencyFormatter.format(g.targetAmount, currency, digitFormat)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val remaining = (g.targetAmount - g.currentAmount).coerceAtLeast(0L)
                                Text(
                                    text = if (remaining > 0) "مانده: ${CurrencyFormatter.format(remaining, currency, digitFormat)}" else "تکمیل شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isDone) IncomeGreen else MaterialTheme.colorScheme.primary)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Deposit button
                            if (!isDone) {
                                OutlinedButton(
                                    onClick = { depositGoalTarget = g },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("واریز به این پس‌انداز")
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

    // Add Goal Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var targetAmountInput by remember { mutableStateOf("") }
        var initialAmountInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("تعریف هدف پس‌انداز") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان هدف") },
                        placeholder = { Text("مثلاً خرید لپ‌تاپ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetAmountInput,
                        onValueChange = { targetAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ هدف ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialAmountInput,
                        onValueChange = { initialAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("موجودی اولیه پس‌انداز (اختیاری)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetAmountInput.toLongOrNull() ?: 0L
                        val initial = initialAmountInput.toLongOrNull() ?: 0L
                        if (title.isNotBlank() && target > 0L) {
                            onAddGoal(
                                title,
                                target,
                                initial,
                                System.currentTimeMillis() + 90L * 24 * 3600 * 1000,
                                0xFF00A86B
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = title.isNotBlank() && (targetAmountInput.toLongOrNull() ?: 0L) > 0L
                ) {
                    Text("ایجاد هدف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Deposit to Goal Dialog
    depositGoalTarget?.let { targetGoal ->
        var depositAmountInput by remember { mutableStateOf("") }
        var selectedAccId by remember {
            mutableLongStateOf(accounts.firstOrNull()?.id ?: 1L)
        }
        var accDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { depositGoalTarget = null },
            title = { Text("واریز به «${targetGoal.title}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = depositAmountInput,
                        onValueChange = { depositAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ واریزی ($currency)") },
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
                            label = { Text("برداشت از حساب") },
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
                                    text = { Text("${a.name} (${CurrencyFormatter.format(a.currentBalance, currency, digitFormat)})") },
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
                        val amount = depositAmountInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            onDepositGoal(targetGoal.id, amount, selectedAccId)
                            depositGoalTarget = null
                        }
                    },
                    enabled = (depositAmountInput.toLongOrNull() ?: 0L) > 0L
                ) {
                    Text("واریز")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositGoalTarget = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}
