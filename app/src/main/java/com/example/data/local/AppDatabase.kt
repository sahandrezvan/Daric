package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.model.AccountType
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingGoalEntity::class,
        InstallmentEntity::class,
        DebtEntity::class,
        UserSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daric_finance_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.financeDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: FinanceDao) {
            // Default User Settings
            dao.insertUserSettings(
                UserSettingsEntity(
                    id = 1,
                    userName = "کاربر داریک",
                    isOnboardingCompleted = false
                )
            )

            // Default Expense Categories
            val expenseCategories = listOf(
                CategoryEntity(name = "Food", nameFa = "خوراک", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = 0xFFFF7043),
                CategoryEntity(name = "Shopping", nameFa = "خرید", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = 0xFF42A5F5),
                CategoryEntity(name = "Home", nameFa = "خانه", type = TransactionType.EXPENSE, iconName = "home", colorHex = 0xFFAB47BC),
                CategoryEntity(name = "Rent", nameFa = "اجاره", type = TransactionType.EXPENSE, iconName = "apartment", colorHex = 0xFF7E57C2),
                CategoryEntity(name = "Bills", nameFa = "قبوض", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = 0xFFFFA726),
                CategoryEntity(name = "Internet", nameFa = "اینترنت", type = TransactionType.EXPENSE, iconName = "wifi", colorHex = 0xFF26A69A),
                CategoryEntity(name = "Transportation", nameFa = "حمل و نقل", type = TransactionType.EXPENSE, iconName = "directions_bus", colorHex = 0xFF29B6F6),
                CategoryEntity(name = "Car", nameFa = "خودرو", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = 0xFF5C6BC0),
                CategoryEntity(name = "Health", nameFa = "سلامت", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = 0xFFEF5350),
                CategoryEntity(name = "Clothing", nameFa = "پوشاک", type = TransactionType.EXPENSE, iconName = "checkroom", colorHex = 0xFFEC407A),
                CategoryEntity(name = "Entertainment", nameFa = "تفریح", type = TransactionType.EXPENSE, iconName = "sports_esports", colorHex = 0xFFFFCA28),
                CategoryEntity(name = "Travel", nameFa = "سفر", type = TransactionType.EXPENSE, iconName = "flight", colorHex = 0xFF26C6DA),
                CategoryEntity(name = "Education", nameFa = "آموزش", type = TransactionType.EXPENSE, iconName = "school", colorHex = 0xFF66BB6A),
                CategoryEntity(name = "Gift", nameFa = "هدیه", type = TransactionType.EXPENSE, iconName = "card_giftcard", colorHex = 0xFFF06292),
                CategoryEntity(name = "Subscriptions", nameFa = "اشتراک‌ها", type = TransactionType.EXPENSE, iconName = "subscriptions", colorHex = 0xFF8D6E63),
                CategoryEntity(name = "Insurance", nameFa = "بیمه", type = TransactionType.EXPENSE, iconName = "security", colorHex = 0xFF78909C),
                CategoryEntity(name = "Tax", nameFa = "مالیات", type = TransactionType.EXPENSE, iconName = "account_balance", colorHex = 0xFFB0BEC5),
                CategoryEntity(name = "Other", nameFa = "دیگر", type = TransactionType.EXPENSE, iconName = "more_horiz", colorHex = 0xFF9E9E9E)
            )
            dao.insertAllCategories(expenseCategories)

            // Default Income Categories
            val incomeCategories = listOf(
                CategoryEntity(name = "Salary", nameFa = "حقوق", type = TransactionType.INCOME, iconName = "payments", colorHex = 0xFF00C853),
                CategoryEntity(name = "Sales", nameFa = "فروش", type = TransactionType.INCOME, iconName = "storefront", colorHex = 0xFF00B0FF),
                CategoryEntity(name = "Investment", nameFa = "سرمایه‌گذاری", type = TransactionType.INCOME, iconName = "trending_up", colorHex = 0xFF00E676),
                CategoryEntity(name = "Gift", nameFa = "هدیه", type = TransactionType.INCOME, iconName = "redeem", colorHex = 0xFFFF4081),
                CategoryEntity(name = "Profit", nameFa = "سود", type = TransactionType.INCOME, iconName = "savings", colorHex = 0xFFFFD700),
                CategoryEntity(name = "Side Income", nameFa = "درآمد جانبی", type = TransactionType.INCOME, iconName = "work", colorHex = 0xFF7C4DFF),
                CategoryEntity(name = "Refund", nameFa = "بازپرداخت", type = TransactionType.INCOME, iconName = "replay", colorHex = 0xFF1DE9B6),
                CategoryEntity(name = "Other Income", nameFa = "سایر", type = TransactionType.INCOME, iconName = "attach_money", colorHex = 0xFFB2FF59)
            )
            dao.insertAllCategories(incomeCategories)

            // Initial Clean Starter Accounts with 0 balance for real user input
            val initialAccounts = listOf(
                AccountEntity(name = "حساب بانکی اصلی", type = AccountType.BANK, initialBalance = 0L, currentBalance = 0L, colorHex = 0xFF1565C0, iconName = "account_balance"),
                AccountEntity(name = "کیف پول نقدی", type = AccountType.CASH, initialBalance = 0L, currentBalance = 0L, colorHex = 0xFF2E7D32, iconName = "payments")
            )
            for (acc in initialAccounts) {
                dao.insertAccount(acc)
            }
        }
    }
}
