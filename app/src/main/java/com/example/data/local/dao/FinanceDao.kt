package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.model.TransactionType
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.DebtEntity
import com.example.data.local.entities.InstallmentEntity
import com.example.data.local.entities.SavingGoalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Accounts ---
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY currentBalance DESC")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, id DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("UPDATE accounts SET currentBalance = currentBalance + :delta WHERE id = :id")
    suspend fun adjustAccountBalance(id: Long, delta: Long)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    // --- Transactions ---
    @Query("SELECT * FROM transactions WHERE isSoftDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isSoftDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isSoftDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime AND isSoftDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE isSoftDeleted = 0 
        AND (description LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isSoftDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long)

    @Query("UPDATE transactions SET isSoftDeleted = 0 WHERE id = :id")
    suspend fun restoreTransaction(id: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionPermanently(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isRecurring = 1 AND recurringParentId IS NULL AND nextOccurrenceAt IS NOT NULL AND nextOccurrenceAt <= :now AND isSoftDeleted = 0")
    suspend fun getDueRecurringTemplates(now: Long): List<TransactionEntity>

    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetForCategoryMonth(categoryId: Long, monthYear: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    // --- Savings Goals ---
    @Query("SELECT * FROM saving_goals ORDER BY isCompleted ASC, targetDate ASC")
    fun getAllGoals(): Flow<List<SavingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingGoalEntity)

    @Query("DELETE FROM saving_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    // --- Installments ---
    @Query("SELECT * FROM installments ORDER BY status ASC, firstDueDate ASC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteInstallment(id: Long)

    // --- Debts ---
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, dueDate ASC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: Long)

    // --- User Settings ---
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getUserSettingsSnapshot(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)

    @Update
    suspend fun updateUserSettings(settings: UserSettingsEntity)

    // --- Backup & Bulk Operations ---
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSnapshot(): List<AccountEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity>

    @Query("SELECT * FROM transactions WHERE isSoftDeleted = 0")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSnapshot(): List<BudgetEntity>

    @Query("SELECT * FROM saving_goals")
    suspend fun getAllGoalsSnapshot(): List<SavingGoalEntity>

    @Query("SELECT * FROM installments")
    suspend fun getAllInstallmentsSnapshot(): List<InstallmentEntity>

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsSnapshot(): List<DebtEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    @Query("DELETE FROM saving_goals")
    suspend fun clearAllGoals()

    @Query("DELETE FROM installments")
    suspend fun clearAllInstallments()

    @Query("DELETE FROM debts")
    suspend fun clearAllDebts()

    @Query("UPDATE accounts SET currentBalance = 0, initialBalance = 0")
    suspend fun resetAllAccountBalancesToZero()
}
