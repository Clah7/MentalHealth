package com.example.mentalhealth

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
import com.example.mentalhealth.data.UserPreferences
import com.example.mentalhealth.functions.readHeartRateToday
import com.example.mentalhealth.functions.readSleepToday
import com.example.mentalhealth.functions.readStepsToday
import com.example.mentalhealth.model.UserData
import com.example.mentalhealth.screen.MainScreen
import kotlinx.coroutines.launch
import java.time.Duration

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var userPreferences: UserPreferences

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectClient = HealthConnectClient.getOrCreate(this)
        userPreferences = UserPreferences(this)

        setContent {
            MaterialTheme {
                MainScreenContainer(healthConnectClient, requiredPermissions, this, userPreferences)
            }
        }
    }
}

@Composable
fun MainScreenContainer(
    healthConnectClient: HealthConnectClient,
    requiredPermissions: Set<String>,
    context: ComponentActivity,
    userPreferences: UserPreferences
) {
    val stepsState = remember { mutableStateOf<Int?>(null) }
    val sleepState = remember { mutableStateOf<List<SleepSessionRecord>?>(null) }
    val heartRateState = remember { mutableStateOf<Double?>(null) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val isFetchingState = remember { mutableStateOf(false) }
    
    // Collect user data from DataStore
    val userDataFlow = userPreferences.userData
    val userData by userDataFlow.collectAsState(initial = UserData())
    
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(requiredPermissions)) {
            scope.launch {
                try {
                    isFetchingState.value = true
                    Log.d("MainScreen", "Starting data fetch...")
                    
                    // Fetch all data
                    val fetchedSteps = readStepsToday(context, healthConnectClient).toInt()
                    val fetchedSleep = readSleepToday(context, healthConnectClient)
                    val fetchedHeartRate = readHeartRateToday(context, healthConnectClient)
                    
                    Log.d("MainScreen", "Fetched data - Steps: $fetchedSteps, Sleep records: ${fetchedSleep.size}, Heart rate: $fetchedHeartRate")
                    
                    // Update states
                    stepsState.value = fetchedSteps
                    sleepState.value = fetchedSleep
                    heartRateState.value = fetchedHeartRate
                    
                    // Calculate sleep duration
                    val sleepDuration = fetchedSleep.let { sleepRecords ->
                        if (sleepRecords.isNotEmpty()) {
                            val totalDuration = sleepRecords.sumOf { 
                                Duration.between(it.startTime, it.endTime).toMinutes() 
                            }
                            val hours = totalDuration / 60
                            val minutes = totalDuration % 60
                            "$hours.$minutes"
                        } else {
                            userData.sleepDuration
                        }
                    }
                    
                    Log.d("MainScreen", "Calculated sleep duration: $sleepDuration")
                    
                    // Update user data with smartwatch data
                    val updatedUserData = userData.copy(
                        sleepDuration = sleepDuration,
                        dailySteps = if (fetchedSteps > 0) fetchedSteps.toString() else userData.dailySteps,
                        heartRate = if (fetchedHeartRate > 0) String.format("%.1f", fetchedHeartRate) else userData.heartRate
                    )
                    
                    Log.d("MainScreen", "Updating user data: $updatedUserData")
                    
                    // Save updated data to DataStore
                    userPreferences.saveUserData(updatedUserData)
                    
                    errorState.value = null
                    Log.d("MainScreen", "Data fetch and update completed successfully")
                } catch (e: Exception) {
                    errorState.value = "Gagal mengambil data: ${e.message}"
                    Log.e("MainScreen", "Error fetching data", e)
                } finally {
                    isFetchingState.value = false
                }
            }
        } else {
            errorState.value = "Izin ditolak. Aplikasi membutuhkan izin untuk mengakses data kesehatan."
            Log.w("MainScreen", "Permission ditolak")
        }
    }

    LaunchedEffect(Unit) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(requiredPermissions)) {
            scope.launch {
                try {
                    isFetchingState.value = true
                    Log.d("MainScreen", "Starting data fetch...")
                    
                    // Fetch all data
                    val fetchedSteps = readStepsToday(context, healthConnectClient).toInt()
                    val fetchedSleep = readSleepToday(context, healthConnectClient)
                    val fetchedHeartRate = readHeartRateToday(context, healthConnectClient)
                    
                    Log.d("MainScreen", "Fetched data - Steps: $fetchedSteps, Sleep records: ${fetchedSleep.size}, Heart rate: $fetchedHeartRate")
                    
                    // Update states
                    stepsState.value = fetchedSteps
                    sleepState.value = fetchedSleep
                    heartRateState.value = fetchedHeartRate
                    
                    // Calculate sleep duration
                    val sleepDuration = fetchedSleep.let { sleepRecords ->
                        if (sleepRecords.isNotEmpty()) {
                            val totalDuration = sleepRecords.sumOf { 
                                Duration.between(it.startTime, it.endTime).toMinutes() 
                            }
                            val hours = totalDuration / 60
                            val minutes = totalDuration % 60
                            "$hours.$minutes"
                        } else {
                            userData.sleepDuration
                        }
                    }
                    
                    Log.d("MainScreen", "Calculated sleep duration: $sleepDuration")
                    
                    // Update user data with smartwatch data
                    val updatedUserData = userData.copy(
                        sleepDuration = sleepDuration,
                        dailySteps = if (fetchedSteps > 0) fetchedSteps.toString() else userData.dailySteps,
                        heartRate = if (fetchedHeartRate > 0) String.format("%.1f", fetchedHeartRate) else userData.heartRate
                    )
                    
                    Log.d("MainScreen", "Updating user data: $updatedUserData")
                    
                    // Save updated data to DataStore
                    userPreferences.saveUserData(updatedUserData)
                    
                    errorState.value = null
                    Log.d("MainScreen", "Data fetch and update completed successfully")
                } catch (e: Exception) {
                    errorState.value = "Gagal mengambil data: ${e.message}"
                    Log.e("MainScreen", "Error fetching data", e)
                } finally {
                    isFetchingState.value = false
                }
            }
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    MainScreen(
        steps = stepsState.value,
        sleep = sleepState.value,
        heartRate = heartRateState.value,
        error = errorState.value,
        isFetching = isFetchingState.value,
        userData = userData,
        onUserDataChange = { newData -> 
            scope.launch {
                userPreferences.saveUserData(newData)
            }
        },
        permissionLauncher = permissionLauncher,
        requiredPermissions = requiredPermissions
    )
}
