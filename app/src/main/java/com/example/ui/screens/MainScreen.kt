package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.CalendarType
import com.example.ui.screens.accounts.AccountDetailScreen
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.backup.BackupRestoreScreen
import com.example.ui.screens.budgets.BudgetsScreen
import com.example.ui.screens.calendar.CalendarViewScreen
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.debts.DebtsScreen
import com.example.ui.screens.goals.SavingsGoalsScreen
import com.example.ui.screens.installments.InstallmentsScreen
import com.example.ui.screens.lock.LockScreen
import com.example.ui.screens.market.MarketRatesScreen
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.more.PrivacyScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.search.GlobalSearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.transactions.AddTransactionSheet
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.viewmodel.DaricViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: DaricViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val installments by viewModel.installments.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    val marketItems by viewModel.marketItems.collectAsState()
    val isMarketRefreshing by viewModel.isMarketRefreshing.collectAsState()
    val lastMarketRefreshTime by viewModel.lastMarketRefreshTime.collectAsState()
    val weather by viewModel.weather.collectAsState()

    val netWorth by viewModel.netWorth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()

    val activeTab by viewModel.activeTab.collectAsState()
    val subScreen by viewModel.currentSubScreen.collectAsState()
    val selectedAccountForDetail by viewModel.selectedAccountForDetail.collectAsState()
    val isAddTransactionOpen by viewModel.isAddTransactionOpen.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()

    val isShamsi = settings.calendarType == CalendarType.SHAMSI

    // Listen for snackbar undo events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { (message, deletedTxId) ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = if (deletedTxId != null) "بازگردانی (Undo)" else null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed && deletedTxId != null) {
                    viewModel.undoDelete(deletedTxId)
                }
            }
        }
    }

    // 1. Onboarding check
    if (!settings.isOnboardingCompleted) {
        OnboardingScreen(
            onComplete = { pin ->
                viewModel.completeOnboarding(pin)
            }
        )
        return
    }

    // 2. Lock screen check
    if (!isUnlocked && ((settings.isPinEnabled && settings.pinCode.isNotBlank()) || settings.isBiometricEnabled)) {
        LockScreen(
            onUnlockWithPin = { pin -> viewModel.unlockWithPin(pin) },
            onUnlockWithBiometric = { viewModel.unlockWithBiometric() },
            hasBiometrics = settings.isBiometricEnabled
        )
        return
    }

    // 3. SubScreen Navigation
    if (subScreen != null) {
        when (subScreen) {
            "transactions" -> TransactionsScreen(
                transactions = transactions,
                accounts = accounts,
                categories = categories,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                isShamsi = isShamsi,
                onTransactionClick = {},
                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "accounts" -> AccountsScreen(
                accounts = accounts,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                onAccountClick = { acc -> viewModel.selectAccountDetail(acc) },
                onAddAccount = { name, type, balance, color, icon, note ->
                    viewModel.addAccount(name, type, balance, color, icon, note)
                },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "account_detail" -> {
                selectedAccountForDetail?.let { acc ->
                    AccountDetailScreen(
                        account = acc,
                        transactions = transactions,
                        categories = categories,
                        currency = settings.currency,
                        digitFormat = settings.digitFormat,
                        isShamsi = isShamsi,
                        onBack = { viewModel.navigateToSubScreen("accounts") },
                        onDeleteAccount = { viewModel.deleteAccount(acc.id) },
                        onTransactionClick = {}
                    )
                } ?: run {
                    viewModel.navigateToSubScreen(null)
                }
            }
            "budgets" -> BudgetsScreen(
                budgets = budgets,
                categories = categories,
                transactions = transactions,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                onSetBudget = { catId, limit -> viewModel.setBudget(catId, limit) },
                onDeleteBudget = { viewModel.deleteBudget(it) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "goals" -> SavingsGoalsScreen(
                goals = goals,
                accounts = accounts,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                onAddGoal = { title, target, current, date, col ->
                    viewModel.addGoal(title, target, current, date, col)
                },
                onDepositGoal = { gId, amt, accId ->
                    viewModel.depositGoal(gId, amt, accId)
                },
                onDeleteGoal = { viewModel.deleteGoal(it) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "installments" -> InstallmentsScreen(
                installments = installments,
                accounts = accounts,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                isShamsi = isShamsi,
                onAddInstallment = { title, total, count, paidCount, instAmt, due, accId, note ->
                    viewModel.addInstallment(title, total, count, paidCount, instAmt, due, accId, note)
                },
                onPayInstallment = { viewModel.payInstallment(it) },
                onToggleInstallmentItem = { instId, itemIndex, isPaid, deduct ->
                    viewModel.toggleInstallmentItem(instId, itemIndex, isPaid, deduct)
                },
                onDeleteInstallment = { viewModel.deleteInstallment(it) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "debts" -> DebtsScreen(
                debts = debts,
                accounts = accounts,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                onAddDebt = { name, type, amt, due, note ->
                    viewModel.addDebt(name, type, amt, due, note)
                },
                onRecordPayment = { dId, amt, accId ->
                    viewModel.recordDebtPayment(dId, amt, accId)
                },
                onDeleteDebt = { viewModel.deleteDebt(it) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "calendar" -> CalendarViewScreen(
                transactions = transactions,
                installments = installments,
                accounts = accounts,
                categories = categories,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                isShamsi = isShamsi,
                onTransactionClick = {},
                onInstallmentsClick = { viewModel.setTab(1) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "search" -> GlobalSearchScreen(
                transactions = transactions,
                accounts = accounts,
                categories = categories,
                currency = settings.currency,
                digitFormat = settings.digitFormat,
                isShamsi = isShamsi,
                onTransactionClick = {},
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "backup" -> BackupRestoreScreen(
                lastBackupTime = settings.lastBackupTimestamp,
                onExportJson = { viewModel.exportJson() },
                onRestoreJson = { json, replaceAll -> viewModel.restoreBackup(json, replaceAll) },
                onResetAllData = { viewModel.resetAllDataToZero() },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "settings" -> SettingsScreen(
                settings = settings,
                onUpdateTheme = { viewModel.updateTheme(it) },
                onUpdateAccent = { viewModel.updateAccent(it) },
                onUpdateLanguage = { viewModel.updateLanguage(it) },
                onUpdateCalendar = { viewModel.updateCalendarType(it) },
                onUpdateDigitFormat = { viewModel.updateDigitFormat(it) },
                onUpdateCurrency = { viewModel.updateCurrency(it) },
                onUpdateUserName = { viewModel.updateUserName(it) },
                onUpdatePin = { pin, en -> viewModel.updatePin(pin, en) },
                onUpdateBiometric = { viewModel.updateBiometric(it) },
                onUpdateCompactMode = { viewModel.updateCompactMode(it) },
                onResetAllData = { viewModel.resetAllDataToZero() },
                onNavigateToBackup = { viewModel.navigateToSubScreen("backup") },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "categories" -> CategoriesScreen(
                categories = categories,
                onAddCategory = { n, nf, t, ic, col -> viewModel.addCategory(n, nf, t, ic, col) },
                onDeleteCategory = { viewModel.deleteCategory(it) },
                onBack = { viewModel.navigateToSubScreen(null) }
            )
            "privacy" -> PrivacyScreen(
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        return
    }

    // 4. Main App Scaffold with Tabs & FAB
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "خانه"
                        )
                    },
                    label = { Text("خانه") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                            contentDescription = "اقساط"
                        )
                    },
                    label = { Text("اقساط") },
                    modifier = Modifier.testTag("nav_installments")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Filled.CurrencyExchange else Icons.Outlined.CurrencyExchange,
                            contentDescription = "قیمت‌ها"
                        )
                    },
                    label = { Text("قیمت‌ها") },
                    modifier = Modifier.testTag("nav_market")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 3) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                            contentDescription = "گزارش‌ها"
                        )
                    },
                    label = { Text("گزارش‌ها") },
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { viewModel.setTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 4) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                            contentDescription = "بیشتر"
                        )
                    },
                    label = { Text("بیشتر") },
                    modifier = Modifier.testTag("nav_more")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTransaction(true) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("global_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "ثبت تراکنش",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = activeTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        userSettings = settings,
                        accounts = accounts,
                        recentTransactions = recentTransactions,
                        categories = categories,
                        installments = installments,
                        netWorth = netWorth,
                        monthlyIncome = monthlyIncome,
                        monthlyExpense = monthlyExpense,
                        weather = weather,
                        onAccountClick = { acc -> viewModel.selectAccountDetail(acc) },
                        onAddAccountClick = { viewModel.navigateToSubScreen("accounts") },
                        onTransactionClick = {},
                        onViewAllTransactions = { viewModel.navigateToSubScreen("transactions") },
                        onSearchClick = { viewModel.navigateToSubScreen("search") },
                        onCalendarClick = { viewModel.navigateToSubScreen("calendar") },
                        onInstallmentsClick = { viewModel.setTab(1) },
                        onMarketRatesClick = { viewModel.setTab(2) },
                        onGoalsClick = { viewModel.navigateToSubScreen("goals") },
                        onPayInstallment = { viewModel.payInstallment(it) }
                    )
                    1 -> InstallmentsScreen(
                        installments = installments,
                        accounts = accounts,
                        currency = settings.currency,
                        digitFormat = settings.digitFormat,
                        isShamsi = isShamsi,
                        onAddInstallment = { title, total, count, paidCount, instAmt, due, accId, note ->
                            viewModel.addInstallment(title, total, count, paidCount, instAmt, due, accId, note)
                        },
                        onPayInstallment = { viewModel.payInstallment(it) },
                        onToggleInstallmentItem = { instId, itemIndex, isPaid, deduct ->
                            viewModel.toggleInstallmentItem(instId, itemIndex, isPaid, deduct)
                        },
                        onDeleteInstallment = { viewModel.deleteInstallment(it) },
                        onBack = null
                    )
                    2 -> MarketRatesScreen(
                        marketItems = marketItems,
                        isRefreshing = isMarketRefreshing,
                        lastRefreshTime = lastMarketRefreshTime,
                        currency = settings.currency,
                        digitFormat = settings.digitFormat,
                        isShamsi = isShamsi,
                        onRefresh = { viewModel.refreshMarketRates() }
                    )
                    3 -> ReportsScreen(
                        transactions = transactions,
                        categories = categories,
                        currency = settings.currency,
                        digitFormat = settings.digitFormat,
                        onExportCsv = {
                            scope.launch {
                                val csv = viewModel.exportCsv()
                                Toast.makeText(context, "فایل اکسل آماده شد (${csv.lines().size} ردیف)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    4 -> MoreScreen(
                        onNavigate = { route -> viewModel.navigateToSubScreen(route) }
                    )
                }
            }
        }
    }

    // Add Transaction Bottom Sheet Modal
    if (isAddTransactionOpen) {
        AddTransactionSheet(
            accounts = accounts,
            categories = categories,
            currency = settings.currency,
            onDismiss = { viewModel.openAddTransaction(false) },
            onSave = { type, amount, accId, destId, catId, desc, note, isRec, recInt ->
                viewModel.addTransaction(
                    type = type,
                    amount = amount,
                    accountId = accId,
                    destinationAccountId = destId,
                    categoryId = catId,
                    description = desc,
                    note = note,
                    isRecurring = isRec,
                    recurringInterval = recInt
                )
            }
        )
    }
}
