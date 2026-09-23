package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.text.DecimalFormat

class DaricSummaryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    companion object {
        private const val PREFS = "daric_widget"
        fun publish(context: Context, balance: Long, income: Long, expense: Long) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("balance", balance).putLong("income", income).putLong("expense", expense).apply()
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DaricSummaryWidget::class.java)
            manager.updateAppWidget(component, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val format = DecimalFormat("#,###")
            val views = RemoteViews(context.packageName, R.layout.widget_daric_summary)
            views.setTextViewText(R.id.widget_balance, "موجودی: ${format.format(prefs.getLong("balance", 0))} تومان")
            views.setTextViewText(R.id.widget_flow, "درآمد ${format.format(prefs.getLong("income", 0))}  •  هزینه ${format.format(prefs.getLong("expense", 0))}")
            val intent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            return views
        }
    }
}
