package com.example.core.model

enum class AccountType(val titleFa: String, val titleEn: String) {
    BANK("بانک", "Bank"),
    CASH("نقدی", "Cash"),
    WALLET("کیف پول", "Wallet"),
    CREDIT_CARD("کارت اعتباری", "Credit Card"),
    SAVINGS("پس‌انداز", "Savings"),
    INVESTMENT("سرمایه‌گذاری", "Investment"),
    GOLD("طلا و سکه", "Gold"),
    CRYPTO("ارز دیجیتال", "Crypto"),
    OTHER("سایر دارایی‌ها", "Other Asset")
}

enum class TransactionType(val titleFa: String, val titleEn: String) {
    EXPENSE("هزینه", "Expense"),
    INCOME("درآمد", "Income"),
    TRANSFER("انتقال", "Transfer")
}

enum class RecurringInterval(val titleFa: String, val titleEn: String) {
    NONE("بدون تکرار", "None"),
    DAILY("روزانه", "Daily"),
    WEEKLY("هفتگی", "Weekly"),
    MONTHLY("ماهانه", "Monthly"),
    YEARLY("سالانه", "Yearly")
}

enum class InstallmentStatus(val titleFa: String, val titleEn: String) {
    PENDING("در انتظار پرداخت", "Pending"),
    PAID("تسویه شده", "Paid"),
    OVERDUE("معوق", "Overdue")
}

enum class DebtType(val titleFa: String, val titleEn: String) {
    CREDITOR("طلب (دیگران بدهکارند)", "Receivable"),
    DEBTOR("بدهی (من بدهکارم)", "Debt")
}

enum class AppThemeMode(val titleFa: String, val titleEn: String) {
    SYSTEM("رنگ‌بندی + حالت سیستم", "Palette + System mode"),
    LIGHT("روشن", "Light"),
    DARK("تاریک", "Dark")
}

/**
 * Curated visual themes. Enum names stay stable for Room.
 * Only [primaryChoices] are offered in Settings; legacy values remap.
 */
enum class AccentColorChoice(val titleFa: String, val titleEn: String, val colorHex: Long) {
    EMERALD("اوبسیدین", "Obsidian", 0xFF3DCF9A),
    SAPPHIRE("نیمه‌شب", "Midnight", 0xFF6B9FD4),
    AMBER("شن", "Sand", 0xFF8B7355), // legacy → remaps to Pearl
    RUBY("مروارید", "Pearl", 0xFF2C2C2A),
    VIOLET("نوردیک", "Nordic", 0xFF4A6278), // legacy → remaps to Midnight
    GRAPHITE("گرافیت", "Graphite", 0xFF4DB6A0), // legacy → remaps to Obsidian
    MONO("مونو", "Mono", 0xFF111111);

    companion object {
        /** The four themes shown in Settings. */
        val primaryChoices: List<AccentColorChoice> = listOf(
            EMERALD,  // Obsidian (default)
            RUBY,     // Pearl
            SAPPHIRE, // Midnight
            MONO
        )

        fun normalize(choice: AccentColorChoice): AccentColorChoice = when (choice) {
            AMBER -> RUBY
            VIOLET -> SAPPHIRE
            GRAPHITE -> EMERALD
            else -> choice
        }
    }
}

enum class AppLanguage(val code: String, val titleFa: String, val titleEn: String) {
    FA("fa", "فارسی (RTL)", "Persian"),
    EN("en", "English (LTR)", "English")
}

enum class CalendarType(val titleFa: String, val titleEn: String) {
    SHAMSI("شمسی (هجری خورشیدی)", "Solar Hijri (Jalali)"),
    GREGORIAN("میلادی", "Gregorian")
}

enum class DigitFormat(val titleFa: String, val titleEn: String) {
    PERSIAN("اعداد فارسی (۱۲۳)", "Persian Digits (۱۲۳)"),
    ENGLISH("اعداد لاتین (123)", "English Digits (123)")
}
