package com.example.data.repository

import com.example.core.model.DebtType
import com.example.core.model.InstallmentStatus
import com.example.core.model.TransactionType
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

class FinanceRepository(private val dao: FinanceDao) {

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
        val id = dao.insertTransaction(tx)
        when (tx.type) {
            TransactionType.EXPENSE -> {
                dao.adjustAccountBalance(tx.accountId, -tx.amount)
            }
            TransactionType.INCOME -> {
                dao.adjustAccountBalance(tx.accountId, tx.amount)
            }
            TransactionType.TRANSFER -> {
                dao.adjustAccountBalance(tx.accountId, -tx.amount)
                tx.destinationAccountId?.let { destId ->
                    dao.adjustAccountBalance(destId, tx.amount)
                }
            }
        }
        id
    }

    suspend fun softDeleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        val tx = dao.getTransactionById(id) ?: return@withContext
        // Reverse balance effect
        when (tx.type) {
            TransactionType.EXPENSE -> dao.adjustAccountBalance(tx.accountId, tx.amount)
            TransactionType.INCOME -> dao.adjustAccountBalance(tx.accountId, -tx.amount)
            TransactionType.TRANSFER -> {
                dao.adjustAccountBalance(tx.accountId, tx.amount)
                tx.destinationAccountId?.let { destId ->
                    dao.adjustAccountBalance(destId, -tx.amount)
                }
            }
        }
        dao.softDeleteTransaction(id)
    }

    suspend fun undoDeleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        val tx = dao.getTransactionById(id) ?: return@withContext
        // Re-apply balance effect
        when (tx.type) {
            TransactionType.EXPENSE -> dao.adjustAccountBalance(tx.accountId, -tx.amount)
            TransactionType.INCOME -> dao.adjustAccountBalance(tx.accountId, tx.amount)
            TransactionType.TRANSFER -> {
                dao.adjustAccountBalance(tx.accountId, -tx.amount)
                tx.destinationAccountId?.let { destId ->
                    dao.adjustAccountBalance(destId, tx.amount)
                }
            }
        }
        dao.restoreTransaction(id)
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
        if (inst.paidInstallments < inst.totalInstallments) {
            val updatedPaid = inst.paidInstallments + 1
            val isFinished = updatedPaid >= inst.totalInstallments
            val nextDueDate = if (!isFinished) inst.firstDueDate + (30L * 24 * 3600 * 1000) else inst.firstDueDate
            dao.updateInstallment(
                inst.copy(
                    paidInstallments = updatedPaid,
                    firstDueDate = nextDueDate,
                    status = if (isFinished) InstallmentStatus.PAID else InstallmentStatus.PENDING
                )
            )
            dao.adjustAccountBalance(inst.accountId, -inst.installmentAmount)

            val categories = dao.getAllCategoriesSnapshot()
            val billsCategory = categories.find { it.nameFa == "قبوض" || it.name == "Bills" }
            val catId = billsCategory?.id ?: categories.firstOrNull()?.id ?: 1L
            dao.insertTransaction(
                TransactionEntity(
                    accountId = inst.accountId,
                    categoryId = catId,
                    type = TransactionType.EXPENSE,
                    amount = inst.installmentAmount,
                    timestamp = System.currentTimeMillis(),
                    description = "پرداخت قسط: ${inst.title} ($updatedPaid/${inst.totalInstallments})",
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
                dao.adjustAccountBalance(inst.accountId, -inst.installmentAmount)
                val categories = dao.getAllCategoriesSnapshot()
                val billsCategory = categories.find { it.nameFa == "قبوض" || it.name == "Bills" }
                val catId = billsCategory?.id ?: categories.firstOrNull()?.id ?: 1L
                dao.insertTransaction(
                    TransactionEntity(
                        accountId = inst.accountId,
                        categoryId = catId,
                        type = TransactionType.EXPENSE,
                        amount = inst.installmentAmount,
                        timestamp = System.currentTimeMillis(),
                        description = "پرداخت قسط: ${inst.title} ($itemIndex/${inst.totalInstallments})",
                        note = inst.note
                    )
                )
            } else {
                dao.adjustAccountBalance(inst.accountId, inst.installmentAmount)
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
        root.put("version", 1)
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
            if (replaceAll) {
                dao.clearAllTransactions()
                dao.clearAllAccounts()
                dao.clearAllBudgets()
                dao.clearAllGoals()
                dao.clearAllInstallments()
                dao.clearAllDebts()
            }

            if (root.has("accounts")) {
                val accountsArr = root.getJSONArray("accounts")
                for (i in 0 until accountsArr.length()) {
                    val o = accountsArr.getJSONObject(i)
                    val acc = AccountEntity(
                        name = o.optString("name", "حساب جدید"),
                        type = try { com.example.core.model.AccountType.valueOf(o.optString("type")) } catch (e: Exception) { com.example.core.model.AccountType.BANK },
                        initialBalance = o.optLong("balance", 0L),
                        currentBalance = o.optLong("balance", 0L),
                        colorHex = o.optLong("color", 0xFF00A86B)
                    )
                    dao.insertAccount(acc)
                }
            }

            if (root.has("transactions")) {
                val txArr = root.getJSONArray("transactions")
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    val tx = TransactionEntity(
                        type = try { TransactionType.valueOf(o.optString("type")) } catch (e: Exception) { TransactionType.EXPENSE },
                        amount = o.optLong("amount", 0L),
                        accountId = o.optLong("accountId", 1L),
                        destinationAccountId = if (o.isNull("destinationAccountId")) null else o.optLong("destinationAccountId"),
                        categoryId = o.optLong("categoryId", 1L),
                        description = o.optString("description", ""),
                        note = o.optString("note", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        tags = o.optString("tags", "")
                    )
                    dao.insertTransaction(tx)
                }
            }

            if (root.has("goals")) {
                val goalsArr = root.getJSONArray("goals")
                for (i in 0 until goalsArr.length()) {
                    val o = goalsArr.getJSONObject(i)
                    val goal = SavingGoalEntity(
                        title = o.optString("title", ""),
                        targetAmount = o.optLong("targetAmount", 0L),
                        currentAmount = o.optLong("currentAmount", 0L),
                        targetDate = o.optLong("targetDate", System.currentTimeMillis()),
                        colorHex = o.optLong("colorHex", 0xFF00A86B),
                        iconName = o.optString("iconName", "flag"),
                        isCompleted = o.optBoolean("isCompleted", false)
                    )
                    dao.insertGoal(goal)
                }
            }

            if (root.has("debts")) {
                val debtsArr = root.getJSONArray("debts")
                for (i in 0 until debtsArr.length()) {
                    val o = debtsArr.getJSONObject(i)
                    val debt = DebtEntity(
                        personName = o.optString("personName", ""),
                        type = try { DebtType.valueOf(o.optString("type")) } catch (e: Exception) { DebtType.CREDITOR },
                        amount = o.optLong("amount", 0L),
                        paidAmount = o.optLong("paidAmount", 0L),
                        dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
                        note = o.optString("note", ""),
                        isSettled = o.optBoolean("isSettled", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                    dao.insertDebt(debt)
                }
            }

            if (root.has("installments")) {
                val instArr = root.getJSONArray("installments")
                for (i in 0 until instArr.length()) {
                    val o = instArr.getJSONObject(i)
                    val inst = InstallmentEntity(
                        title = o.optString("title", ""),
                        totalAmount = o.optLong("totalAmount", 0L),
                        totalInstallments = o.optInt("totalInstallments", 1),
                        paidInstallments = o.optInt("paidInstallments", 0),
                        installmentAmount = o.optLong("installmentAmount", 0L),
                        firstDueDate = o.optLong("firstDueDate", System.currentTimeMillis()),
                        accountId = o.optLong("accountId", 1L),
                        status = try { InstallmentStatus.valueOf(o.optString("status")) } catch (e: Exception) { InstallmentStatus.PENDING },
                        note = o.optString("note", ""),
                        paidIndicesWithDates = o.optString("paidIndicesWithDates", "")
                    )
                    dao.insertInstallment(inst)
                }
            }

            if (root.has("budgets")) {
                val budgetArr = root.getJSONArray("budgets")
                for (i in 0 until budgetArr.length()) {
                    val o = budgetArr.getJSONObject(i)
                    val b = BudgetEntity(
                        categoryId = o.optLong("categoryId", 1L),
                        monthlyLimit = o.optLong("monthlyLimit", 0L),
                        monthYear = o.optString("monthYear", ""),
                        alertThreshold = o.optInt("alertThreshold", 80)
                    )
                    dao.insertBudget(b)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
