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
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()

    private val _items = MutableStateFlow<List<MarketItem>>(emptyList())
    val marketItems: StateFlow<List<MarketItem>> = _items.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _lastRefresh = MutableStateFlow(0L)
    val lastRefreshTime: StateFlow<Long> = _lastRefresh.asStateFlow()

    suspend fun refreshRates(): Result<Unit> = withContext(Dispatchers.IO) {
        _refreshing.value = true
        try {
            val fresh = mutableListOf<MarketItem>()
            fetchFirst(FIAT_URLS)?.let { fresh += parseFiat(it) }
            fetchFirst(GOLD_URLS)?.let { fresh += parseGold(it) }
            val usdToman = fresh.firstOrNull { it.id == "usd" }?.priceToman ?: 0L
            fetch(CRYPTO_URL)?.let { fresh += parseCrypto(it, usdToman) }

            if (fresh.isEmpty()) return@withContext Result.failure(
                IllegalStateException("No live market source was available")
            )

            // Keep still-valid results from another provider when only one request fails.
            val merged = _items.value.associateBy { it.id }.toMutableMap()
            fresh.forEach { merged[it.id] = it }
            _items.value = merged.values.sortedWith(
                compareBy<MarketItem> { it.category.ordinal }.thenBy {
                    DISPLAY_ORDER.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index }
                }
            )
            _lastRefresh.value = fresh.maxOf { it.lastUpdated }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            _refreshing.value = false
        }
    }

    private fun fetchFirst(urls: List<String>): JSONObject? {
        urls.forEach { fetch(it)?.let { json -> return json } }
        return null
    }

    private fun fetch(url: String): JSONObject? = runCatching {
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Daric-Android/1.3")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body?.string()?.takeIf(String::isNotBlank)?.let(::JSONObject)
        }
    }.getOrNull()

    private fun parseFiat(json: JSONObject) = FIAT_ITEMS.mapNotNull { meta ->
        val data = json.optJSONObject(meta.sourceId) ?: return@mapNotNull null
        val price = data.optDouble("value", 0.0).toLong()
        if (price <= 0) return@mapNotNull null
        marketItem(meta, MarketCategory.CURRENCY, price, data)
    }

    private fun parseGold(json: JSONObject) = GOLD_ITEMS.mapNotNull { meta ->
        val data = json.optJSONObject(meta.sourceId) ?: return@mapNotNull null
        val price = data.optDouble("value", 0.0)
        if (price <= 0) return@mapNotNull null
        marketItem(
            meta = meta,
            category = MarketCategory.GOLD,
            priceToman = if (meta.sourceId == "usd_xau") 0 else price.toLong(),
            data = data,
            priceUsd = if (meta.sourceId == "usd_xau") price else null
        )
    }

    private fun parseCrypto(json: JSONObject, usdToman: Long) = CRYPTO_ITEMS.mapNotNull { meta ->
        val data = json.optJSONObject(meta.sourceId) ?: return@mapNotNull null
        val usd = data.optDouble("usd", 0.0)
        if (usd <= 0) return@mapNotNull null
        MarketItem(
            id = meta.id, nameFa = meta.fa, nameEn = meta.en, symbol = meta.symbol,
            category = MarketCategory.CRYPTO,
            priceToman = if (usdToman > 0) (usd * usdToman).toLong() else 0,
            priceUsd = usd,
            change24h = data.optDouble("usd_24h_change", 0.0),
            lastUpdated = System.currentTimeMillis(),
            source = "CoinGecko"
        )
    }

    private fun marketItem(
        meta: Meta,
        category: MarketCategory,
        priceToman: Long,
        data: JSONObject,
        priceUsd: Double? = null
    ) = MarketItem(
        id = meta.id, nameFa = meta.fa, nameEn = meta.en, symbol = meta.symbol,
        category = category, priceToman = priceToman, priceUsd = priceUsd,
        change24h = data.optDouble("change_pct", 0.0),
        lastUpdated = data.optLong("date", 0).takeIf { it > 0 }?.times(1000)
            ?: System.currentTimeMillis(),
        source = NAVASAN_SOURCE
    )

    private data class Meta(
        val sourceId: String,
        val id: String = sourceId,
        val fa: String,
        val en: String,
        val symbol: String
    )

    companion object {
        private const val NAVASAN_SOURCE = "Navasan (GitHub)"
        private val FIAT_URLS = listOf(
            "https://raw.githubusercontent.com/HosseinOdd/Navasan-API/main/data/fiat.json",
            "https://cdn.jsdelivr.net/gh/HosseinOdd/Navasan-API@main/data/fiat.json"
        )
        private val GOLD_URLS = listOf(
            "https://raw.githubusercontent.com/HosseinOdd/Navasan-API/main/data/gold.json",
            "https://cdn.jsdelivr.net/gh/HosseinOdd/Navasan-API@main/data/gold.json"
        )
        private const val CRYPTO_URL = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,tether,solana,binancecoin,ripple,the-open-network,cardano,dogecoin,tron&vs_currencies=usd&include_24hr_change=true"

        private val FIAT_ITEMS = listOf(
            Meta("usd", fa = "دلار آمریکا", en = "US Dollar", symbol = "USD"),
            Meta("eur", fa = "یورو", en = "Euro", symbol = "EUR"),
            Meta("gbp", fa = "پوند انگلیس", en = "British Pound", symbol = "GBP"),
            Meta("aed", fa = "درهم امارات", en = "UAE Dirham", symbol = "AED"),
            Meta("try", fa = "لیر ترکیه", en = "Turkish Lira", symbol = "TRY"),
            Meta("cad", fa = "دلار کانادا", en = "Canadian Dollar", symbol = "CAD"),
            Meta("aud", fa = "دلار استرالیا", en = "Australian Dollar", symbol = "AUD"),
            Meta("chf", fa = "فرانک سوئیس", en = "Swiss Franc", symbol = "CHF")
        )
        private val GOLD_ITEMS = listOf(
            Meta("sekkeh", "emami", "سکه امامی", "Emami Gold Coin", "سکه"),
            Meta("bahar", "azadi", "سکه بهار آزادی", "Bahar Azadi Coin", "سکه"),
            Meta("nim", fa = "نیم سکه", en = "Half Gold Coin", symbol = "نیم"),
            Meta("rob", fa = "ربع سکه", en = "Quarter Gold Coin", symbol = "ربع"),
            Meta("gerami", fa = "سکه گرمی", en = "One Gram Coin", symbol = "گرمی"),
            Meta("18ayar", "gold18", "طلای ۱۸ عیار", "18k Gold (Gram)", "طلا"),
            Meta("abshodeh", "melted_gold", "طلای آب‌شده", "Melted Gold", "آب‌شده"),
            Meta("usd_xau", "ounce_gold", "انس جهانی طلا", "Gold Ounce", "XAU")
        )
        private val CRYPTO_ITEMS = listOf(
            Meta("bitcoin", "btc", "بیت‌کوین", "Bitcoin", "BTC"),
            Meta("ethereum", "eth", "اتریوم", "Ethereum", "ETH"),
            Meta("tether", "usdt", "تتر", "Tether", "USDT"),
            Meta("solana", "sol", "سولانا", "Solana", "SOL"),
            Meta("binancecoin", "bnb", "بایننس‌کوین", "BNB", "BNB"),
            Meta("ripple", "xrp", "ریپل", "XRP", "XRP"),
            Meta("the-open-network", "ton", "تون‌کوین", "Toncoin", "TON"),
            Meta("cardano", "ada", "کاردانو", "Cardano", "ADA"),
            Meta("dogecoin", "doge", "دوج‌کوین", "Dogecoin", "DOGE"),
            Meta("tron", "trx", "ترون", "TRON", "TRX")
        )
        private val DISPLAY_ORDER = listOf(
            "emami", "azadi", "nim", "rob", "gerami", "gold18", "melted_gold", "ounce_gold",
            "usd", "eur", "gbp", "aed", "try", "cad", "aud", "chf",
            "btc", "eth", "usdt", "sol", "bnb", "xrp", "ton", "ada", "doge", "trx"
        )
    }
}
