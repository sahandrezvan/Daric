package com.example.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MoreMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val route: String
)

@Composable
fun MoreScreen(
    onNavigate: (route: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MoreMenuItem("دفترچه تراکنش‌ها", "مشاهده و جستجو در کلیه تراکنش‌ها، درآمدها و هزینه‌ها", Icons.Default.ReceiptLong, Color(0xFF00897B), "transactions"),
        MoreMenuItem("حساب‌ها و دارایی‌ها", "کارت‌های بانکی، نقدی، طلا و سرمایه", Icons.Default.AccountBalance, Color(0xFF1565C0), "accounts"),
        MoreMenuItem("بودجه‌بندی ماهانه", "سقف مجاز مخارج برای هر دسته‌بندی", Icons.Default.PieChart, Color(0xFFE91E63), "budgets"),
        MoreMenuItem("اهداف پس‌انداز", "برنامه‌ریزی برای خریدهای آینده", Icons.Default.Savings, Color(0xFF00A86B), "goals"),
        MoreMenuItem("وام‌ها و اقساط", "ثبت سررسیدها و اقساط ماهانه", Icons.Default.ReceiptLong, Color(0xFFFF9800), "installments"),
        MoreMenuItem("طلب و بدهی", "قرض‌ها و تسویه‌حساب با اشخاص", Icons.Default.Handshake, Color(0xFF9C27B0), "debts"),
        MoreMenuItem("تقویم مالی روزانه", "بررسی مخارج و درآمد در روزهای تقویم", Icons.Default.CalendarMonth, Color(0xFF00BCD4), "calendar"),
        MoreMenuItem("دسته‌بندی‌ها", "مدیریت و افزودن انواع درآمد و هزینه", Icons.Default.Category, Color(0xFF4CAF50), "categories"),
        MoreMenuItem("پشتیبان‌گیری و بازیابی", "نسخه پشتیبان محلی و گوگل درایو", Icons.Default.CloudSync, Color(0xFF3F51B5), "backup"),
        MoreMenuItem("تنظیمات برنامه", "تم تیره/روشن، رنگ سازمانی، تقویم و زبان", Icons.Default.Settings, Color(0xFF607D8B), "settings"),
        MoreMenuItem("حریم خصوصی و امنیت", "معماری آفلاین، محلی و رمزنگاری", Icons.Default.Security, Color(0xFF00897B), "privacy")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "امکانات داریک",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(items.size) { index ->
            val item = items[index]
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(item.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(item.iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
