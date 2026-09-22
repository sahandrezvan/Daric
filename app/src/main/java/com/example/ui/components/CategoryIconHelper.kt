package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "home" -> Icons.Default.Home
            "apartment", "rent" -> Icons.Default.Apartment
            "receipt_long", "bills" -> Icons.Default.ReceiptLong
            "wifi", "internet" -> Icons.Default.Wifi
            "directions_bus", "transport" -> Icons.Default.DirectionsBus
            "directions_car", "car" -> Icons.Default.DirectionsCar
            "medical_services", "health" -> Icons.Default.MedicalServices
            "checkroom", "clothing" -> Icons.Default.Checkroom
            "sports_esports", "entertainment" -> Icons.Default.SportsEsports
            "flight", "travel" -> Icons.Default.Flight
            "school", "education" -> Icons.Default.School
            "card_giftcard", "gift" -> Icons.Default.CardGiftcard
            "subscriptions" -> Icons.Default.Subscriptions
            "security", "insurance" -> Icons.Default.Security
            "account_balance", "tax", "bank" -> Icons.Default.AccountBalance
            "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
            "payments", "salary", "cash" -> Icons.Default.Payments
            "storefront", "sales" -> Icons.Default.Storefront
            "trending_up", "investment" -> Icons.Default.TrendingUp
            "redeem" -> Icons.Default.Redeem
            "savings", "profit" -> Icons.Default.Savings
            "work", "side_income" -> Icons.Default.Work
            "replay", "refund" -> Icons.Default.Replay
            "monetization_on", "gold" -> Icons.Default.MonetizationOn
            "attach_money" -> Icons.Default.AttachMoney
            else -> Icons.Default.MoreHoriz
        }
    }
}
