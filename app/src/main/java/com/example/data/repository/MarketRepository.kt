package com.example.data.repository

import com.example.core.model.MarketCategory
import com.example.core.model.MarketItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MarketRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // Never seed the UI with invented prices. Items appear only after a live response.
    private val _marketItems = MutableStateFlow<List<MarketItem>>(emptyList())
    val marketItems: StateFlow<List<MarketItem>> = _marketItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow(0L)
    val lastRefreshTime: StateFlow<Long> = _lastRefreshTime.asStateFlow()

    suspend fun refreshRates(): Result<Unit> = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        try {
            val currentList = _marketItems.value.toMutableList()
            val usdRate = 0L

            // 1. Fetch live cryptocurrency prices from CoinGecko free public API
            try {
                val url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,tether,solana,binancecoin,ripple,the-open-network,cardano,dogecoin,tron&vs_currencies=usd&include_24hr_change=true"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            val json = JSONObject(bodyString)
                            val mapping = mapOf(
                                "bitcoin" to Triple("btc", "بیت‌کوین", "Bitcoin"),
                                "ethereum" to Triple("eth", "اتریوم", "Ethereum"),
                                "tether" to Triple("usdt", "تتر", "Tether"),
                                "solana" to Triple("sol", "سولانا", "Solana"),
                                "binancecoin" to Triple("bnb", "بایننس‌کوین", "BNB"),
                                "ripple" to Triple("xrp", "ریپل", "XRP"),
                                "the-open-network" to Triple("ton", "تون‌کوین", "Toncoin"),
                                "cardano" to Triple("ada", "کاردانو", "Cardano"),
                                "dogecoin" to Triple("doge", "دوج‌کوین", "Dogecoin"),
                                "tron" to Triple("trx", "ترون", "TRON")
                            )

                            mapping.forEach { (geckoId, meta) ->
                                if (json.has(geckoId)) {
                                    val coinObj = json.getJSONObject(geckoId)
                                    val priceUsd = coinObj.optDouble("usd", 0.0)
                                    val change = coinObj.optDouble("usd_24h_change", 0.0)
                                    val priceToman = if (usdRate > 0) (priceUsd * usdRate).toLong() else 0L

                                    val index = currentList.indexOfFirst { it.id == meta.first }
                                    if (index >= 0) {
                                        val existing = currentList[index]
                                        currentList[index] = existing.copy(
                                            priceToman = priceToman,
                                            priceUsd = priceUsd,
                                            change24h = Math.round(change * 100.0) / 100.0,
                                            lastUpdated = System.currentTimeMillis(),
                                            source = "CoinGecko"
                                        )
                                    } else {
                                        currentList += MarketItem(
                                            id = meta.first,
                                            nameFa = meta.second,
                                            nameEn = meta.third,
                                            symbol = meta.first.uppercase(),
                                            category = MarketCategory.CRYPTO,
                                            priceToman = priceToman,
                                            priceUsd = priceUsd,
                                            change24h = Math.round(change * 100.0) / 100.0,
                                            lastUpdated = System.currentTimeMillis(),
                                            source = "CoinGecko"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Network unavailable or blocked, retain cache
            }

            _marketItems.value = currentList
            if (currentList.isNotEmpty()) _lastRefreshTime.value = System.currentTimeMillis()
            _isRefreshing.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isRefreshing.value = false
            Result.failure(e)
        }
    }

    private fun initialMarketData(): List<MarketItem> {
        val now = System.currentTimeMillis()
        val usdRate = 63200L

        return listOf(
            // 1. سکه و طلا (Gold & Coins)
            MarketItem(
                id = "emami",
                nameFa = "سکه امامی (طرح جدید)",
                nameEn = "Emami Gold Coin",
                symbol = "سکه",
                category = MarketCategory.GOLD,
                priceToman = 54400000L,
                change24h = 0.85,
                highToman = 54800000L,
                lowToman = 53900000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "azadi",
                nameFa = "سکه بهار آزادی (طرح قدیم)",
                nameEn = "Bahar Azadi Coin",
                symbol = "سکه",
                category = MarketCategory.GOLD,
                priceToman = 48800000L,
                change24h = 0.62,
                highToman = 49100000L,
                lowToman = 48400000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "nim",
                nameFa = "نیم سکه بهار آزادی",
                nameEn = "Half Gold Coin",
                symbol = "نیم",
                category = MarketCategory.GOLD,
                priceToman = 28900000L,
                change24h = -0.34,
                highToman = 29200000L,
                lowToman = 28700000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "rob",
                nameFa = "ربع سکه بهار آزادی",
                nameEn = "Quarter Gold Coin",
                symbol = "ربع",
                category = MarketCategory.GOLD,
                priceToman = 18600000L,
                change24h = 1.12,
                highToman = 18800000L,
                lowToman = 18400000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "gerami",
                nameFa = "سکه یک گرمی",
                nameEn = "1g Gold Coin",
                symbol = "گرمی",
                category = MarketCategory.GOLD,
                priceToman = 8750000L,
                change24h = 0.0,
                highToman = 8800000L,
                lowToman = 8700000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "gold18",
                nameFa = "طلای ۱۸ عیار (هر گرم)",
                nameEn = "18k Gold (Gram)",
                symbol = "طلا",
                category = MarketCategory.GOLD,
                priceToman = 4660000L,
                change24h = 0.45,
                highToman = 4690000L,
                lowToman = 4630000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "gold24",
                nameFa = "طلای ۲۴ عیار (هر گرم)",
                nameEn = "24k Gold (Gram)",
                symbol = "طلا ۲۴",
                category = MarketCategory.GOLD,
                priceToman = 6213000L,
                change24h = 0.45,
                highToman = 6250000L,
                lowToman = 6180000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "mesghal",
                nameFa = "مثقال طلا (مظنه تهران)",
                nameEn = "Mesghal Gold",
                symbol = "مثقال",
                category = MarketCategory.GOLD,
                priceToman = 20185000L,
                change24h = 0.48,
                highToman = 20300000L,
                lowToman = 20050000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "ounce_gold",
                nameFa = "انس جهانی طلا",
                nameEn = "Gold Ounce (USD)",
                symbol = "XAU",
                category = MarketCategory.GOLD,
                priceToman = (2684.5 * usdRate).toLong(),
                priceUsd = 2684.5,
                change24h = 0.32,
                highToman = (2695.0 * usdRate).toLong(),
                lowToman = (2670.0 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "ounce_silver",
                nameFa = "انس جهانی نقره",
                nameEn = "Silver Ounce (USD)",
                symbol = "XAG",
                category = MarketCategory.GOLD,
                priceToman = (31.8 * usdRate).toLong(),
                priceUsd = 31.8,
                change24h = -0.65,
                highToman = (32.2 * usdRate).toLong(),
                lowToman = (31.4 * usdRate).toLong(),
                lastUpdated = now
            ),

            // 2. ارزها (Forex & Currencies)
            MarketItem(
                id = "usd",
                nameFa = "دلار آمریکا (اسکناس)",
                nameEn = "US Dollar",
                symbol = "USD",
                category = MarketCategory.CURRENCY,
                priceToman = 63200L,
                priceUsd = 1.0,
                change24h = 0.48,
                highToman = 63500L,
                lowToman = 62900L,
                lastUpdated = now
            ),
            MarketItem(
                id = "eur",
                nameFa = "یورو اتحادیه اروپا",
                nameEn = "Euro",
                symbol = "EUR",
                category = MarketCategory.CURRENCY,
                priceToman = 68700L,
                priceUsd = 1.087,
                change24h = 0.35,
                highToman = 69100L,
                lowToman = 68400L,
                lastUpdated = now
            ),
            MarketItem(
                id = "aed",
                nameFa = "درهم امارات",
                nameEn = "UAE Dirham",
                symbol = "AED",
                category = MarketCategory.CURRENCY,
                priceToman = 17220L,
                priceUsd = 0.272,
                change24h = 0.46,
                highToman = 17300L,
                lowToman = 17150L,
                lastUpdated = now
            ),
            MarketItem(
                id = "gbp",
                nameFa = "پوند انگلیس",
                nameEn = "British Pound",
                symbol = "GBP",
                category = MarketCategory.CURRENCY,
                priceToman = 81800L,
                priceUsd = 1.294,
                change24h = 0.22,
                highToman = 82200L,
                lowToman = 81400L,
                lastUpdated = now
            ),
            MarketItem(
                id = "try",
                nameFa = "لیر ترکیه",
                nameEn = "Turkish Lira",
                symbol = "TRY",
                category = MarketCategory.CURRENCY,
                priceToman = 1850L,
                priceUsd = 0.029,
                change24h = -0.54,
                highToman = 1880L,
                lowToman = 1840L,
                lastUpdated = now
            ),
            MarketItem(
                id = "cny",
                nameFa = "یوان چین",
                nameEn = "Chinese Yuan",
                symbol = "CNY",
                category = MarketCategory.CURRENCY,
                priceToman = 8880L,
                priceUsd = 0.14,
                change24h = 0.15,
                highToman = 8920L,
                lowToman = 8850L,
                lastUpdated = now
            ),
            MarketItem(
                id = "iqd",
                nameFa = "دینار عراق (۱۰۰۰ دینار)",
                nameEn = "Iraqi Dinar (1k)",
                symbol = "IQD",
                category = MarketCategory.CURRENCY,
                priceToman = 48200L,
                priceUsd = 0.763,
                change24h = 0.0,
                highToman = 48500L,
                lowToman = 48000L,
                lastUpdated = now
            ),
            MarketItem(
                id = "cad",
                nameFa = "دلار کانادا",
                nameEn = "Canadian Dollar",
                symbol = "CAD",
                category = MarketCategory.CURRENCY,
                priceToman = 46400L,
                priceUsd = 0.734,
                change24h = 0.30,
                highToman = 46700L,
                lowToman = 46100L,
                lastUpdated = now
            ),
            MarketItem(
                id = "aud",
                nameFa = "دلار استرالیا",
                nameEn = "Australian Dollar",
                symbol = "AUD",
                category = MarketCategory.CURRENCY,
                priceToman = 42100L,
                priceUsd = 0.666,
                change24h = -0.18,
                highToman = 42400L,
                lowToman = 41900L,
                lastUpdated = now
            ),
            MarketItem(
                id = "chf",
                nameFa = "فرانک سوئیس",
                nameEn = "Swiss Franc",
                symbol = "CHF",
                category = MarketCategory.CURRENCY,
                priceToman = 73400L,
                priceUsd = 1.161,
                change24h = 0.41,
                highToman = 73800L,
                lowToman = 73000L,
                lastUpdated = now
            ),

            // 3. ارزهای دیجیتال (Cryptocurrency)
            MarketItem(
                id = "btc",
                nameFa = "بیت‌کوین",
                nameEn = "Bitcoin",
                symbol = "BTC",
                category = MarketCategory.CRYPTO,
                priceToman = (64800.0 * usdRate).toLong(),
                priceUsd = 64800.0,
                change24h = 2.45,
                highToman = (65500.0 * usdRate).toLong(),
                lowToman = (63900.0 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "eth",
                nameFa = "اتریوم",
                nameEn = "Ethereum",
                symbol = "ETH",
                category = MarketCategory.CRYPTO,
                priceToman = (2650.0 * usdRate).toLong(),
                priceUsd = 2650.0,
                change24h = 1.82,
                highToman = (2710.0 * usdRate).toLong(),
                lowToman = (2580.0 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "usdt",
                nameFa = "تتر",
                nameEn = "Tether",
                symbol = "USDT",
                category = MarketCategory.CRYPTO,
                priceToman = 63300L,
                priceUsd = 1.0,
                change24h = 0.05,
                highToman = 63500L,
                lowToman = 63100L,
                lastUpdated = now
            ),
            MarketItem(
                id = "sol",
                nameFa = "سولانا",
                nameEn = "Solana",
                symbol = "SOL",
                category = MarketCategory.CRYPTO,
                priceToman = (152.4 * usdRate).toLong(),
                priceUsd = 152.4,
                change24h = 3.65,
                highToman = (156.0 * usdRate).toLong(),
                lowToman = (147.0 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "bnb",
                nameFa = "بایننس کوین",
                nameEn = "BNB",
                symbol = "BNB",
                category = MarketCategory.CRYPTO,
                priceToman = (585.0 * usdRate).toLong(),
                priceUsd = 585.0,
                change24h = 1.15,
                highToman = (595.0 * usdRate).toLong(),
                lowToman = (578.0 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "ton",
                nameFa = "تون‌کوین (تلگرام)",
                nameEn = "Toncoin",
                symbol = "TON",
                category = MarketCategory.CRYPTO,
                priceToman = (5.75 * usdRate).toLong(),
                priceUsd = 5.75,
                change24h = -0.84,
                highToman = (5.92 * usdRate).toLong(),
                lowToman = (5.65 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "xrp",
                nameFa = "ریپل",
                nameEn = "XRP",
                symbol = "XRP",
                category = MarketCategory.CRYPTO,
                priceToman = (0.585 * usdRate).toLong(),
                priceUsd = 0.585,
                change24h = -1.20,
                highToman = (0.605 * usdRate).toLong(),
                lowToman = (0.575 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "doge",
                nameFa = "دوج‌کوین",
                nameEn = "Dogecoin",
                symbol = "DOGE",
                category = MarketCategory.CRYPTO,
                priceToman = (0.118 * usdRate).toLong(),
                priceUsd = 0.118,
                change24h = 4.25,
                highToman = (0.124 * usdRate).toLong(),
                lowToman = (0.112 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "trx",
                nameFa = "ترون",
                nameEn = "TRON",
                symbol = "TRX",
                category = MarketCategory.CRYPTO,
                priceToman = (0.155 * usdRate).toLong(),
                priceUsd = 0.155,
                change24h = 0.45,
                highToman = (0.158 * usdRate).toLong(),
                lowToman = (0.152 * usdRate).toLong(),
                lastUpdated = now
            ),
            MarketItem(
                id = "ada",
                nameFa = "کاردانو",
                nameEn = "Cardano",
                symbol = "ADA",
                category = MarketCategory.CRYPTO,
                priceToman = (0.362 * usdRate).toLong(),
                priceUsd = 0.362,
                change24h = 1.05,
                highToman = (0.375 * usdRate).toLong(),
                lowToman = (0.355 * usdRate).toLong(),
                lastUpdated = now
            )
        )
    }
}
