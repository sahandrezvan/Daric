package com.example.data.repository

import com.example.core.model.DebtType
import com.example.core.model.InstallmentStatus
import com.example.core.model.TransactionType
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.FinanceDao
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.DebtEntity
import com.example.data.local.entities.InstallmentEntity
import com.example.data.local.entities.SavingGoalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class FinanceRepository(private val db: AppDatabase) {
    private val dao: FinanceDao = db.financeDao()

    val accounts: Flow<List<AccountEntity>> = dao.getActiveAccounts()
    val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecentTransactions(20)
    val goals: Flow<List<SavingGoalEntity>> = dao.getAllGoals()
    val installments: Flow<List<InstallmentEntity>> = dao.getAllInstallments()
    val debts: Flow<List<DebtEntity>> = dao.getAllDebts()
    val userSettings: Flow<UserSettingsEntity> = dao.getUserSettings().map { it ?: UserSettingsEntity() }

    fun getBudgets(monthYear: String): Flow<List<BudgetEntity>> = dao.getBudgetsForMonth(monthYear)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>> = dao.searchTransactions(query)

    // --- Transaction Execution with Balance Updating ---
    suspend fun addTransaction(tx: TransactionEntity): Long = withContext(Dispatchers.IO) {
        require(tx.amount > 0) { "Transaction amount must be positive" }
        db.withTransaction {
            val id = dao.insertTransaction(tx)
            when (tx.type) {
                TransactionType.EXPENSE -> dao.adjustAccountBalance(tx.accountId, -tx.amount)
                TransactionType.INCOME -> dao.adjustAccountBalance(tx.accountId, tx.amount)
                TransactionType.TRANSFER -> {
                    val destinationId = requireNotNull(tx.destinationAccountId) {
                        "Transfer requires a destination account"
                    }
                    require(destinationId != tx.accountId) { "Source and destination accounts must differ" }
                    dao.adjustAccountBalance(tx.accountId, -tx.amount)
                    dao.adjustAccountBalance(destinationId, tx.amount)
                }
            }
            id
        }
    }

    suspend fun softDeleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val tx = dao.getTransactionById(id) ?: return@withTransaction
            if (tx.isSoftDeleted) return@withTransaction
            when (tx.type) {
                TransactionType.EXPENSE -> dao.adjustAccountBalance(tx.accountId, tx.amount)
                TransactionType.INCOME -> dao.adjustAccountBalance(tx.accountId, -tx.amount)
                TransactionType.TRANSFER -> {
                    dao.adjustAccountBalance(tx.accountId, tx.amount)
                    tx.destinationAccountId?.let { dao.adjustAccountBalance(it, -tx.amount) }
                }
            }
            dao.softDeleteTransaction(id)
        }
    }

    suspend fun undoDeleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val tx = dao.getTransactionById(id) ?: return@withTransaction
            if (!tx.isSoftDeleted) return@withTransaction
            when (tx.type) {
                TransactionType.EXPENSE -> dao.adjustAccountBalance(tx.accountId, -tx.amount)
                TransactionType.INCOME -> dao.adjustAccountBalance(tx.accountId, tx.amount)
                TransactionType.TRANSFER -> {
                    val destinationId = requireNotNull(tx.destinationAccountId) {
                        "Transfer requires a destination account"
                    }
                    dao.adjustAccountBalance(tx.accountId, -tx.amount)
                    dao.adjustAccountBalance(destinationId, tx.amount)
                }
            }
            dao.restoreTransaction(id)
        }
    }

    // --- Accounts ---
    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        dao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        dao.updateAccount(account)
    }

    suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteAccount(id)
    }

    // --- Categories ---
    suspend fun addCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteCategory(id)
    }

    // --- Budgets ---
    suspend fun setBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        dao.insertBudget(budget)
    }

    suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBudget(id)
    }

    // --- Goals ---
    suspend fun addGoal(goal: SavingGoalEntity) = withContext(Dispatchers.IO) {
        dao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingGoalEntity) = withContext(Dispatchers.IO) {
        dao.updateGoal(goal)
    }

    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteGoal(id)
    }

    suspend fun depositToGoal(goalId: Long, amount: Long, accountId: Long) = withContext(Dispatchers.IO) {
        val goalsList = dao.getAllGoalsSnapshot()
        val goal = goalsList.find { it.id == goalId } ?: return@withContext
        val newAmount = goal.currentAmount + amount
        dao.updateGoal(goal.copy(currentAmount = newAmount, isCompleted = newAmount >= goal.targetAmount))
        dao.adjustAccountBalance(accountId, -amount)
    }

    // --- Installments ---
    suspend fun addInstallment(inst: InstallmentEntity) = withContext(Dispatchers.IO) {
        dao.insertInstallment(inst)
    }

    suspend fun payNextInstallment(installmentId: Long) = withContext(Dispatchers.IO) {
        val list = dao.getAllInstallmentsSnapshot()
        val inst = list.find { it.id == installmentId } ?: return@withContext
        val nextItem = com.example.core.util.InstallmentScheduleHelper.nextUnpaid(inst)
        if (nextItem != null) {
            val updated = com.example.core.util.InstallmentScheduleHelper.toggleItem(
                inst = inst,
                itemIndex = nextItem.index,
                isPaid = true,
                paidTimestamp = System.currentTimeMillis()
            )
            dao.updateInstallment(updated)
            val paymentAmount = com.example.core.util.InstallmentScheduleHelper.amountFor(inst, nextItem.index)
            dao.adjustAccountBalance(inst.accountId, -paymentAmount)

            val categories = dao.getAllCategoriesSnapshot()
            val billsCategory = categories.find { it.nameFa == "قبوض" || it.name == "Bills" }
            val catId = billsCategory?.id ?: categories.firstOrNull()?.id ?: 1L
            dao.insertTransaction(
                TransactionEntity(
                    accountId = inst.accountId,
                    categoryId = catId,
                    type = TransactionType.EXPENSE,
                    amount = paymentAmount,
                    timestamp = System.currentTimeMillis(),
                    description = "پرداخت قسط: ${inst.title} (${nextItem.index}/${inst.totalInstallments})",
                    note = inst.note
                )
            )
        }
    }

    suspend fun toggleInstallmentItem(
        installmentId: Long,
        itemIndex: Int,
        isPaid: Boolean,
        deductFromAccount: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val list = dao.getAllInstallmentsSnapshot()
        val inst = list.find { it.id == installmentId } ?: return@withContext
        val updated = com.example.core.util.InstallmentScheduleHelper.toggleItem(
            inst = inst,
            itemIndex = itemIndex,
            isPaid = isPaid,
            paidTimestamp = System.currentTimeMillis()
        )
        dao.updateInstallment(updated)

        if (deductFromAccount) {
            if (isPaid) {
                val paymentAmount = com.example.core.util.InstallmentScheduleHelper.amountFor(inst, itemIndex)
                dao.adjustAccountBalance(inst.accountId, -paymentAmount)
                val categories = dao.getAllCategoriesSnapshot()
                val billsCategory = categories.find { it.nameFa == "قبوض" || it.name == "Bills" }
                val catId = billsCategory?.id ?: categories.firstOrNull()?.id ?: 1L
                dao.insertTransaction(
                    TransactionEntity(
                        accountId = inst.accountId,
                        categoryId = catId,
                        type = TransactionType.EXPENSE,
                        amount = paymentAmount,
                        timestamp = System.currentTimeMillis(),
                        description = "پرداخت قسط: ${inst.title} ($itemIndex/${inst.totalInstallments})",
                        note = inst.note
                    )
                )
            } else {
                val paymentAmount = com.example.core.util.InstallmentScheduleHelper.amountFor(inst, itemIndex)
                dao.adjustAccountBalance(inst.accountId, paymentAmount)
            }
        }
    }

    suspend fun resetAllDataToZero() = withContext(Dispatchers.IO) {
        dao.clearAllTransactions()
        dao.clearAllDebts()
        dao.clearAllInstallments()
        dao.clearAllGoals()
        dao.clearAllBudgets()
        dao.resetAllAccountBalancesToZero()
    }

    suspend fun deleteInstallment(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteInstallment(id)
    }

    // --- Debts ---
    suspend fun addDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        dao.insertDebt(debt)
    }

    suspend fun recordDebtPayment(debtId: Long, paymentAmount: Long, accountId: Long?) = withContext(Dispatchers.IO) {
        val list = dao.getAllDebtsSnapshot()
        val debt = list.find { it.id == debtId } ?: return@withContext
        val newPaid = debt.paidAmount + paymentAmount
        val settled = newPaid >= debt.amount
        dao.updateDebt(debt.copy(paidAmount = newPaid, isSettled = settled))
        if (accountId != null) {
            when (debt.type) {
                com.example.core.model.DebtType.CREDITOR -> {
                    // Somebody paid us -> increase our account
                    dao.adjustAccountBalance(accountId, paymentAmount)
                }
                com.example.core.model.DebtType.DEBTOR -> {
                    // We paid them -> decrease our account
                    dao.adjustAccountBalance(accountId, -paymentAmount)
                }
            }
        }
    }

    suspend fun deleteDebt(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteDebt(id)
    }

    // --- Settings ---
    suspend fun updateSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        dao.updateUserSettings(settings)
    }

    // --- Export / Backup to JSON ---
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        val accountsArr = JSONArray()
        dao.getAllAccountsSnapshot().forEach { acc ->
            val o = JSONObject()
            o.put("id", acc.id)
            o.put("name", acc.name)
            o.put("type", acc.type.name)
            o.put("balance", acc.currentBalance)
            o.put("color", acc.colorHex)
            accountsArr.put(o)
        }
        root.put("accounts", accountsArr)

        val categoriesArr = JSONArray()
        dao.getAllCategoriesSnapshot().forEach { category ->
            val o = JSONObject()
            o.put("id", category.id)
            o.put("name", category.name)
            o.put("nameFa", category.nameFa)
            o.put("type", category.type.name)
            o.put("iconName", category.iconName)
            o.put("colorHex", category.colorHex)
            o.put("isDefault", category.isDefault)
            categoriesArr.put(o)
        }
        root.put("categories", categoriesArr)

        val txArr = JSONArray()
        dao.getAllTransactionsSnapshot().forEach { tx ->
            val o = JSONObject()
            o.put("id", tx.id)
            o.put("type", tx.type.name)
            o.put("amount", tx.amount)
            o.put("accountId", tx.accountId)
            o.put("destinationAccountId", tx.destinationAccountId ?: JSONObject.NULL)
            o.put("categoryId", tx.categoryId)
            o.put("description", tx.description)
            o.put("note", tx.note)
            o.put("timestamp", tx.timestamp)
            o.put("tags", tx.tags)
            txArr.put(o)
        }
        root.put("transactions", txArr)

        val goalsArr = JSONArray()
        dao.getAllGoalsSnapshot().forEach { g ->
            val o = JSONObject()
            o.put("id", g.id)
            o.put("title", g.title)
            o.put("targetAmount", g.targetAmount)
            o.put("currentAmount", g.currentAmount)
            o.put("targetDate", g.targetDate)
            o.put("colorHex", g.colorHex)
            o.put("iconName", g.iconName)
            o.put("isCompleted", g.isCompleted)
            goalsArr.put(o)
        }
        root.put("goals", goalsArr)

        val debtsArr = JSONArray()
        dao.getAllDebtsSnapshot().forEach { d ->
            val o = JSONObject()
            o.put("id", d.id)
            o.put("personName", d.personName)
            o.put("type", d.type.name)
            o.put("amount", d.amount)
            o.put("paidAmount", d.paidAmount)
            o.put("dueDate", d.dueDate ?: JSONObject.NULL)
            o.put("note", d.note)
            o.put("isSettled", d.isSettled)
            o.put("createdAt", d.createdAt)
            debtsArr.put(o)
        }
        root.put("debts", debtsArr)

        val instArr = JSONArray()
        dao.getAllInstallmentsSnapshot().forEach { inst ->
            val o = JSONObject()
            o.put("id", inst.id)
            o.put("title", inst.title)
            o.put("totalAmount", inst.totalAmount)
            o.put("totalInstallments", inst.totalInstallments)
            o.put("paidInstallments", inst.paidInstallments)
            o.put("installmentAmount", inst.installmentAmount)
            o.put("firstDueDate", inst.firstDueDate)
            o.put("scheduleStartDate", inst.scheduleStartDate)
            o.put("accountId", inst.accountId)
            o.put("status", inst.status.name)
            o.put("note", inst.note)
            o.put("paidIndicesWithDates", inst.paidIndicesWithDates)
            instArr.put(o)
        }
        root.put("installments", instArr)

        val budgetArr = JSONArray()
        dao.getAllBudgetsSnapshot().forEach { b ->
            val o = JSONObject()
            o.put("id", b.id)
            o.put("categoryId", b.categoryId)
            o.put("monthlyLimit", b.monthlyLimit)
            o.put("monthYear", b.monthYear)
            o.put("alertThreshold", b.alertThreshold)
            budgetArr.put(o)
        }
        root.put("budgets", budgetArr)

        root.toString(2)
    }

    // --- Export to CSV ---
    suspend fun exportTransactionsCsv(): String = withContext(Dispatchers.IO) {
        val list = dao.getAllTransactionsSnapshot()
        val accounts = dao.getAllAccountsSnapshot().associateBy { it.id }
        val categories = dao.getAllCategoriesSnapshot().associateBy { it.id }
        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Account,Category,Description,Note,Timestamp,Tags\n")
        list.forEach { tx ->
            val accName = accounts[tx.accountId]?.name ?: tx.accountId.toString()
            val catName = categories[tx.categoryId]?.nameFa ?: tx.categoryId.toString()
            sb.append("${tx.id},${tx.type.name},${tx.amount},\"$accName\",\"$catName\",\"${tx.description}\",\"${tx.note}\",${tx.timestamp},\"${tx.tags}\"\n")
        }
        sb.toString()
    }

    // --- Restore / Import JSON ---
    suspend fun restoreDataFromJson(jsonStr: String, replaceAll: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            require(version in 1..2) { "Unsupported backup version: $version" }

            db.withTransaction {
                val existingCategories = dao.getAllCategoriesSnapshot()

                if (replaceAll) {
                    dao.clearAllTransactions()
                    dao.clearAllBudgets()
                    dao.clearAllGoals()
                    dao.clearAllInstallments()
                    dao.clearAllDebts()
                    dao.clearAllAccounts()
                    if (root.has("categories")) {
                        dao.clearAllCategories()
                    }
                }

                val accountIdMap = mutableMapOf<Long, Long>()
                if (root.has("accounts")) {
                    val accountsArr = root.getJSONArray("accounts")
                    for (i in 0 until accountsArr.length()) {
                        val o = accountsArr.getJSONObject(i)
                        val oldId = o.optLong("id", 0L)
                        val balance = o.optLong("balance", o.optLong("currentBalance", 0L))
                        val newId = dao.insertAccount(
                            AccountEntity(
                                name = o.optString("name", "حساب جدید"),
                                type = runCatching {
                                    com.example.core.model.AccountType.valueOf(o.optString("type"))
                                }.getOrDefault(com.example.core.model.AccountType.BANK),
                                initialBalance = o.optLong("initialBalance", balance),
                                currentBalance = balance,
                                colorHex = o.optLong("color", o.optLong("colorHex", 0xFF00A86B)),
                                iconName = o.optString("iconName", "account_balance"),
                                note = o.optString("note", ""),
                                isArchived = o.optBoolean("isArchived", false),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                        if (oldId > 0) accountIdMap[oldId] = newId
                    }
                }

                val categoryIdMap = mutableMapOf<Long, Long>()
                if (root.has("categories")) {
                    val categoriesArr = root.getJSONArray("categories")
                    for (i in 0 until categoriesArr.length()) {
                        val o = categoriesArr.getJSONObject(i)
                        val oldId = o.optLong("id", 0L)
                        val newId = dao.insertCategory(
                            CategoryEntity(
                                name = o.optString("name", "Other"),
                                nameFa = o.optString("nameFa", "سایر"),
                                type = runCatching {
                                    TransactionType.valueOf(o.optString("type"))
                                }.getOrDefault(TransactionType.EXPENSE),
                                iconName = o.optString("iconName", "more_horiz"),
                                colorHex = o.optLong("colorHex", 0xFF9E9E9E),
                                isDefault = o.optBoolean("isDefault", false)
                            )
                        )
                        if (oldId > 0) categoryIdMap[oldId] = newId
                    }
                } else {
                    existingCategories.forEach { categoryIdMap[it.id] = it.id }
                }

                suspend fun mappedAccount(oldId: Long): Long =
                    accountIdMap[oldId] ?: dao.getAllAccountsSnapshot().firstOrNull()?.id
                    ?: error("Backup contains financial records but no account")

                suspend fun mappedCategory(oldId: Long, type: TransactionType): Long =
                    categoryIdMap[oldId]
                        ?: dao.getAllCategoriesSnapshot().firstOrNull { it.type == type }?.id
                        ?: dao.getAllCategoriesSnapshot().firstOrNull()?.id
                        ?: error("No category available for restored transaction")

                if (root.has("transactions")) {
                    val txArr = root.getJSONArray("transactions")
                    for (i in 0 until txArr.length()) {
                        val o = txArr.getJSONObject(i)
                        val type = runCatching {
                            TransactionType.valueOf(o.optString("type"))
                        }.getOrDefault(TransactionType.EXPENSE)
                        val amount = o.optLong("amount", 0L)
                        if (amount <= 0L) continue
                        val oldAccountId = o.optLong("accountId", 0L)
                        val oldDestinationId =
                            if (o.isNull("destinationAccountId")) null else o.optLong("destinationAccountId")

                        dao.insertTransaction(
                            TransactionEntity(
                                type = type,
                                amount = amount,
                                accountId = mappedAccount(oldAccountId),
                                destinationAccountId = oldDestinationId?.let { accountIdMap[it] },
                                categoryId = mappedCategory(o.optLong("categoryId", 0L), type),
                                description = o.optString("description", ""),
                                note = o.optString("note", ""),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                                tags = o.optString("tags", ""),
                                attachmentPath = null,
                                isRecurring = o.optBoolean("isRecurring", false),
                                recurringInterval = runCatching {
                                    com.example.core.model.RecurringInterval.valueOf(
                                        o.optString("recurringInterval", "NONE")
                                    )
                                }.getOrDefault(com.example.core.model.RecurringInterval.NONE),
                                isSoftDeleted = false,
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                if (root.has("goals")) {
                    val goalsArr = root.getJSONArray("goals")
                    for (i in 0 until goalsArr.length()) {
                        val o = goalsArr.getJSONObject(i)
                        dao.insertGoal(
                            SavingGoalEntity(
                                title = o.optString("title", ""),
                                targetAmount = o.optLong("targetAmount", 0L),
                                currentAmount = o.optLong("currentAmount", 0L),
                                targetDate = o.optLong("targetDate", System.currentTimeMillis()),
                                colorHex = o.optLong("colorHex", 0xFF00A86B),
                                iconName = o.optString("iconName", "flag"),
                                isCompleted = o.optBoolean("isCompleted", false)
                            )
                        )
                    }
                }

                if (root.has("debts")) {
                    val debtsArr = root.getJSONArray("debts")
                    for (i in 0 until debtsArr.length()) {
                        val o = debtsArr.getJSONObject(i)
                        dao.insertDebt(
                            DebtEntity(
                                personName = o.optString("personName", ""),
                                type = runCatching { DebtType.valueOf(o.optString("type")) }
                                    .getOrDefault(DebtType.CREDITOR),
                                amount = o.optLong("amount", 0L),
                                paidAmount = o.optLong("paidAmount", 0L),
                                dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
                                note = o.optString("note", ""),
                                isSettled = o.optBoolean("isSettled", false),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                if (root.has("installments")) {
                    val instArr = root.getJSONArray("installments")
                    for (i in 0 until instArr.length()) {
                        val o = instArr.getJSONObject(i)
                        dao.insertInstallment(
                            InstallmentEntity(
                                title = o.optString("title", ""),
                                totalAmount = o.optLong("totalAmount", 0L),
                                totalInstallments = o.optInt("totalInstallments", 1).coerceAtLeast(1),
                                paidInstallments = o.optInt("paidInstallments", 0).coerceAtLeast(0),
                                installmentAmount = o.optLong("installmentAmount", 0L),
                                firstDueDate = o.optLong("firstDueDate", System.currentTimeMillis()),
                                scheduleStartDate = o.optLong(
                                    "scheduleStartDate",
                                    o.optLong("firstDueDate", System.currentTimeMillis())
                                ),
                                accountId = mappedAccount(o.optLong("accountId", 0L)),
                                status = runCatching {
                                    InstallmentStatus.valueOf(o.optString("status"))
                                }.getOrDefault(InstallmentStatus.PENDING),
                                note = o.optString("note", ""),
                                paidIndicesWithDates = o.optString("paidIndicesWithDates", "")
                            )
                        )
                    }
                }

                if (root.has("budgets")) {
                    val budgetArr = root.getJSONArray("budgets")
                    for (i in 0 until budgetArr.length()) {
                        val o = budgetArr.getJSONObject(i)
                        dao.insertBudget(
                            BudgetEntity(
                                categoryId = mappedCategory(
                                    o.optLong("categoryId", 0L),
                                    TransactionType.EXPENSE
                                ),
                                monthlyLimit = o.optLong("monthlyLimit", 0L),
                                monthYear = o.optString("monthYear", ""),
                                alertThreshold = o.optInt("alertThreshold", 80).coerceIn(1, 100)
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
