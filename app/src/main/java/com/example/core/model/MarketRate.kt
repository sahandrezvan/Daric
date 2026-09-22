package com.example.core.model

enum class MarketCategory(val titleFa: String, val titleEn: String) {
    GOLD("سکه و طلا", "Gold & Coins"),
    CURRENCY("دلار و ارزها", "Forex & Currencies"),
    CRYPTO("ارز دیجیتال", "Cryptocurrency")
}

data class MarketItem(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val symbol: String,
    val category: MarketCategory,
    val priceToman: Long,
    val priceUsd: Double? = null,
    val change24h: Double = 0.0,
    val highToman: Long = 0L,
    val lowToman: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)
