package com.example.core.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.core.util.InstallmentScheduleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class FinanceMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val database = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
        val repository = FinanceRepository(database)
        repository.processRecurringTransactions()
        val due = repository.getInstallmentsSnapshot().mapNotNull { installment ->
            InstallmentScheduleHelper.nextUnpaid(installment)?.let { installment to it }
        }.filter { (_, item) ->
            item.scheduledDueDate <= System.currentTimeMillis() + THREE_DAYS
        }
        val remindersEnabled = database.financeDao().getUserSettingsSnapshot()?.installmentRemindersEnabled ?: true
        if (remindersEnabled && due.isNotEmpty()) notifyDueInstallments(due.size, due.first().first.title)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun notifyDueInstallments(count: Int, firstTitle: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "یادآوری اقساط", NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val text = if (count == 1) "سررسید «$firstTitle» نزدیک است" else "$count قسط سررسید نزدیک یا معوق دارند"
        NotificationManagerCompat.from(applicationContext).notify(
            1401,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(com.example.R.mipmap.ic_launcher)
                .setContentTitle("یادآوری مالی داریک")
                .setContentText(text)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        const val UNIQUE_NAME = "daric-finance-maintenance"
        private const val CHANNEL_ID = "installment-reminders"
        private const val THREE_DAYS = 3L * 24 * 60 * 60 * 1000
    }
}
