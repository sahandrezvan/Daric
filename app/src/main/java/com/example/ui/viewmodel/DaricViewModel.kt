package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.AccentColorChoice
import com.example.core.model.AccountType
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.model.CalendarType
import com.example.core.model.DebtType
import com.example.core.model.DigitFormat
import com.example.core.model.InstallmentStatus
import com.example.core.model.RecurringInterval
import com.example.core.model.TransactionType
import com.example.core.util.PinSecurity
import com.example.core.util.RecurringSchedule
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.CategoryEntity
import com.example.core.model.MarketItem
import com.example.data.local.entities.DebtEntity
import com.example.data.local.entities.InstallmentEntity
import com.example.data.local.entities.SavingGoalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.UserSettingsEntity
import com.example.data.repository.FinanceRepository
import com.example.data.repository.MarketRepository
import com.example.data.repository.WeatherRepository
import com.example.core.model.WeatherInfo
import com.example.ui.components.ChartSlice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.widget.DaricSummaryWidget

class DaricViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = FinanceRepository(db)
    private val marketRepository = MarketRepository()
    private val weatherRepository = WeatherRepository()

    val marketItems: StateFlow<List<MarketItem>> = marketRepository.marketItems
    val isMarketRefreshing: StateFlow<Boolean> = marketRepository.isRefreshing
    val lastMarketRefreshTime: StateFlow<Long> = marketRepository.lastRefreshTime
    val weather: StateFlow<WeatherInfo> = weatherRepository.weather

    fun refreshMarketRates() {
        viewModelScope.launch {
            marketRepository.refreshRates()
        }
        viewModelScope.launch { weatherRepository.refresh() }
    }

    init {
        viewModelScope.launch {
            marketRepository.refreshRates()
        }
        viewModelScope.launch {
            weatherRepository.refresh()
        }
        viewModelScope.launch { repository.processRecurringTransactions() }
    }

    val settings: StateFlow<UserSettingsEntity> = repository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettingsEntity()
        )

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val goals: StateFlow<List<SavingGoalEntity>> = repository.goals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val installments: StateFlow<List<InstallmentEntity>> = repository.installments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val debts: StateFlow<List<DebtEntity>> = repository.debts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Monthly Budgets
    private val _currentMonthKey = MutableStateFlow(getThisMonthKey())
    val budgets: StateFlow<List<BudgetEntity>> = _currentMonthKey
        .combine(repository.getBudgets(_currentMonthKey.value)) { _, list -> list }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Financial Metrics Calculation
    val netWorth: StateFlow<Long> = accounts.combine(goals) { accs, _ ->
        accs.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyIncome: StateFlow<Long> = transactions.combine(_currentMonthKey) { txs, _ ->
        val startOfMonth = getStartOfMonthMillis()
        txs.filter { it.type == TransactionType.INCOME && it.timestamp >= startOfMonth }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = transactions.combine(_currentMonthKey) { txs, _ ->
        val startOfMonth = getStartOfMonthMillis()
        txs.filter { it.type == TransactionType.EXPENSE && it.timestamp >= startOfMonth }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        viewModelScope.launch {
            combine(netWorth, monthlyIncome, monthlyExpense) { balance, income, expense ->
                Triple(balance, income, expense)
            }.collect { (balance, income, expense) ->
                DaricSummaryWidget.publish(getApplication(), balance, income, expense)
            }
        }
    }

    // UI Navigation & Dialog states
    private val _activeTab = MutableStateFlow(0) // 0: Home, 1: Transactions, 3: Reports, 4: More
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _currentSubScreen = MutableStateFlow<String?>(null)
    val currentSubScreen: StateFlow<String?> = _currentSubScreen.asStateFlow()

    private val _selectedAccountForDetail = MutableStateFlow<AccountEntity?>(null)
    val selectedAccountForDetail: StateFlow<AccountEntity?> = _selectedAccountForDetail.asStateFlow()

    private val _isAddTransactionOpen = MutableStateFlow(false)
    val isAddTransactionOpen: StateFlow<Boolean> = _isAddTransactionOpen.asStateFlow()

    private val _isUnlocked = MutableStateFlow(true)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Undo toast message event
    private val _snackbarEvent = MutableSharedFlow<Pair<String, Long?>>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                if (s.isPinEnabled && s.pinCode.isNotBlank() && s.isOnboardingCompleted) {
                    _isUnlocked.value = false
                    if (!PinSecurity.isEncoded(s.pinCode)) {
                        repository.updateSettings(s.copy(pinCode = PinSecurity.hash(s.pinCode)))
                    }
                }
            }
        }
    }

    fun setTab(index: Int) {
        _activeTab.value = index
        _currentSubScreen.value = null
    }

    fun navigateToSubScreen(route: String?) {
        _currentSubScreen.value = route
    }

    fun selectAccountDetail(account: AccountEntity) {
        _selectedAccountForDetail.value = account
        _currentSubScreen.value = "account_detail"
    }

    fun openAddTransaction(open: Boolean) {
        _isAddTransactionOpen.value = open
    }

    fun unlockWithPin(pin: String): Boolean {
        if (PinSecurity.verify(pin, settings.value.pinCode)) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun unlockWithBiometric() {
        _isUnlocked.value = true
    }

    fun completeOnboarding(pin: String?) {
        viewModelScope.launch {
            val updated = settings.value.copy(
                isOnboardingCompleted = true,
                isPinEnabled = !pin.isNullOrBlank(),
                pinCode = if (pin.isNullOrBlank()) "" else PinSecurity.hash(pin)
            )
            repository.updateSettings(updated)
            _isUnlocked.value = true
        }
    }

    // --- Transactions ---
    fun addTransaction(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        destinationAccountId: Long? = null,
        categoryId: Long,
        description: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        tags: String = "",
        isRecurring: Boolean = false,
        recurringInterval: RecurringInterval = RecurringInterval.NONE
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                type = type,
                amount = amount,
                accountId = accountId,
                destinationAccountId = destinationAccountId,
                categoryId = categoryId,
                description = description,
                note = note,
                timestamp = timestamp,
                tags = tags,
                isRecurring = isRecurring,
                recurringInterval = recurringInterval,
                nextOccurrenceAt = if (isRecurring) RecurringSchedule.next(timestamp, recurringInterval) else null
            )
            repository.addTransaction(tx)
            _isAddTransactionOpen.value = false
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.softDeleteTransaction(id)
            _snackbarEvent.emit(Pair("تراکنش حذف شد", id))
        }
    }

    fun undoDelete(id: Long) {
        viewModelScope.launch {
            repository.undoDeleteTransaction(id)
        }
    }

    // --- Accounts ---
    fun addAccount(
        name: String,
        type: AccountType,
        balance: Long,
        colorHex: Long,
        iconName: String,
        note: String
    ) {
        viewModelScope.launch {
            val acc = AccountEntity(
                name = name,
                type = type,
                initialBalance = balance,
                currentBalance = balance,
                colorHex = colorHex,
                iconName = iconName,
                note = note
            )
            repository.addAccount(acc)
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            if (_selectedAccountForDetail.value?.id == id) {
                _selectedAccountForDetail.value = null
                _currentSubScreen.value = null
            }
        }
    }

    // --- Categories ---
    fun addCategory(name: String, nameFa: String, type: TransactionType, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                nameFa = nameFa,
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false
            )
            repository.addCategory(cat)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // --- Budgets ---
    fun setBudget(categoryId: Long, monthlyLimit: Long) {
        viewModelScope.launch {
            val b = BudgetEntity(
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                monthYear = _currentMonthKey.value
            )
            repository.setBudget(b)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    // --- Savings Goals ---
    fun addGoal(title: String, targetAmount: Long, currentAmount: Long, targetDate: Long, colorHex: Long) {
        viewModelScope.launch {
            val g = SavingGoalEntity(
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                colorHex = colorHex
            )
            repository.addGoal(g)
        }
    }

    fun depositGoal(goalId: Long, amount: Long, accountId: Long) {
        viewModelScope.launch {
            repository.depositToGoal(goalId, amount, accountId)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

    // --- Installments ---
    fun addInstallment(
        title: String,
        totalAmount: Long,
        totalInstallments: Int,
        paidInstallments: Int = 0,
        installmentAmount: Long,
        firstDueDate: Long,
        accountId: Long,
        note: String
    ) {
        viewModelScope.launch {
            val inst = InstallmentEntity(
                title = title,
                totalAmount = totalAmount,
                totalInstallments = totalInstallments,
                paidInstallments = paidInstallments,
                installmentAmount = installmentAmount,
                firstDueDate = firstDueDate,
                scheduleStartDate = firstDueDate,
                accountId = accountId,
                note = note
            )
            repository.addInstallment(inst)
        }
    }

    fun payInstallment(id: Long) {
        viewModelScope.launch {
            repository.payNextInstallment(id)
            _snackbarEvent.emit(Pair("قسط با موفقیت ثبت و پرداخت شد", null))
        }
    }

    fun toggleInstallmentItem(installmentId: Long, itemIndex: Int, isPaid: Boolean, deductFromAccount: Boolean = true) {
        viewModelScope.launch {
            repository.toggleInstallmentItem(installmentId, itemIndex, isPaid, deductFromAccount)
            val msg = if (isPaid) "قسط شماره $itemIndex به عنوان پرداخت شده ثبت شد" else "وضعیت قسط شماره $itemIndex به پرداخت‌نشده تغییر یافت"
            _snackbarEvent.emit(Pair(msg, null))
        }
    }

    fun deleteInstallment(id: Long) {
        viewModelScope.launch {
            repository.deleteInstallment(id)
        }
    }

    fun resetAllDataToZero() {
        viewModelScope.launch {
            repository.resetAllDataToZero()
            _snackbarEvent.emit(Pair("تمامی داده‌های آزمایشی پاک شد و موجودی‌ها صفر شدند", null))
        }
    }

    // --- Debts ---
    fun addDebt(
        personName: String,
        type: DebtType,
        amount: Long,
        dueDate: Long?,
        note: String
    ) {
        viewModelScope.launch {
            val d = DebtEntity(
                personName = personName,
                type = type,
                amount = amount,
                dueDate = dueDate,
                note = note
            )
            repository.addDebt(d)
        }
    }

    fun recordDebtPayment(debtId: Long, amount: Long, accountId: Long?) {
        viewModelScope.launch {
            repository.recordDebtPayment(debtId, amount, accountId)
        }
    }

    fun deleteDebt(id: Long) {
        viewModelScope.launch {
            repository.deleteDebt(id)
        }
    }

    // --- Settings Updates ---
    fun updateTheme(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(themeMode = themeMode))
        }
    }

    fun updateAccent(accent: AccentColorChoice) {
        viewModelScope.launch {
            repository.updateSettings(
                settings.value.copy(accentColor = AccentColorChoice.normalize(accent))
            )
        }
    }

    fun updateCompactMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(isCompactMode = enabled))
        }
    }

    fun updateDashboardSections(sections: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(dashboardSections = sections))
        }
    }

    fun updateInstallmentReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(installmentRemindersEnabled = enabled))
        }
    }

    fun updateLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(language = lang))
        }
    }

    fun updateCalendarType(cal: CalendarType) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(calendarType = cal))
        }
    }

    fun updateDigitFormat(digit: DigitFormat) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(digitFormat = digit))
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(currency = currency))
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(userName = name))
        }
    }

    fun updatePin(pin: String, enabled: Boolean) {
        viewModelScope.launch {
            val storedPin = if (enabled && pin.isNotBlank()) PinSecurity.hash(pin) else ""
            repository.updateSettings(
                settings.value.copy(pinCode = storedPin, isPinEnabled = enabled)
            )
        }
    }

    fun updateBiometric(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(isBiometricEnabled = enabled))
        }
    }

    // --- Backup & Restore ---
    suspend fun exportJson(): String = repository.exportDataAsJson()
    suspend fun exportCsv(): String = repository.exportTransactionsCsv()

    suspend fun restoreBackup(jsonStr: String, replaceAll: Boolean): Boolean {
        val success = repository.restoreDataFromJson(jsonStr, replaceAll)
        if (success) {
            repository.updateSettings(settings.value.copy(lastBackupTimestamp = System.currentTimeMillis()))
        }
        return success
    }

    private fun getThisMonthKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
    }

    private fun getStartOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
