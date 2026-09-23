package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.theme.DaricTheme
import com.example.ui.viewmodel.DaricViewModel
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.core.worker.FinanceMaintenanceWorker
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    private val viewModel: DaricViewModel by viewModels()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            FinanceMaintenanceWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<FinanceMaintenanceWorker>(24, TimeUnit.HOURS).build()
        )
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()

            DaricTheme(
                themeMode = settings.themeMode,
                accentColor = com.example.core.model.AccentColorChoice.normalize(settings.accentColor),
                language = settings.language
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
