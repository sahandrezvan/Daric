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
    SYSTEM("پیرو سیستم", "System"),
    LIGHT("روشن", "Light"),
    DARK("تاریک (AMOLED)", "Dark")
}

enum class AccentColorChoice(val titleFa: String, val titleEn: String, val colorHex: Long) {
    EMERALD("سبز زمردی مینیمال", "Emerald Green", 0xFF00A86B),
    SAPPHIRE("آبی لاجوردی مینیمال", "Royal Sapphire", 0xFF2563EB),
    AMBER("کهربایی گرم مدرن", "Warm Amber", 0xFFF59E0B),
    RUBY("یاقوتی سرخ مدرن", "Modern Ruby", 0xFFE11D48),
    VIOLET("بنفش نئوکلاسیک", "Neo Violet", 0xFF8B5CF6)
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
