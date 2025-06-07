package com.example.mentalhealth

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        healthConnectClient = HealthConnectClient.getOrCreate(this)

        requestPermissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(requiredPermissions)) {
                readStepsLastHour()
            } else {
                Log.w("MainActivity", "Permission Health Connect ditolak")
            }
        }

        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(requiredPermissions)) {
                readStepsLastHour()
            } else {
                requestPermissionLauncher.launch(requiredPermissions)
            }
        }
    }

    private fun readStepsLastHour() {
        lifecycleScope.launch {
            val now = Instant.now()
            val oneHourAgo = now.minusSeconds(3600)

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.between(oneHourAgo, now)
                )
            )

            val totalSteps = response.records.sumOf { it.count }
            Log.i("MainActivity", "Langkah 1 jam terakhir: $totalSteps")
            // TODO: tampilkan totalSteps di UI (TextView / Compose)
        }
    }
}
