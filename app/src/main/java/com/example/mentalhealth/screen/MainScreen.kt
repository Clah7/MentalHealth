package com.example.mentalhealth.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.mentalhealth.model.UserData
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Home : Screen("home", { Icon(Icons.Default.Home, contentDescription = "Home") }, "Home")
    object Profile : Screen("profile", { Icon(Icons.Default.Person, contentDescription = "Profile") }, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    steps: Int?,
    sleep: List<SleepSessionRecord>?,
    heartRate: Double?,
    error: String?,
    isFetching: Boolean,
    userData: UserData,
    onUserDataChange: (UserData) -> Unit
) {     
    val scrollState = rememberScrollState()
    var showFetchDialog by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var bmiExpanded by remember { mutableStateOf(false) }
    var sleepDisorderExpanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    
    val genderOptions = listOf("Male", "Female")
    val bmiCategories = listOf("Normal", "Overweight")
    val sleepDisorderOptions = listOf("None", "Insomnia", "Sleep Apnea")

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.Home,
                    onClick = { currentScreen = Screen.Home },
                    icon = Screen.Home.icon,
                    label = { Text(Screen.Home.label) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Profile,
                    onClick = { currentScreen = Screen.Profile },
                    icon = Screen.Profile.icon,
                    label = { Text(Screen.Profile.label) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Fetch Data Button
                        // Button(
                        //     onClick = { showFetchDialog = true },
                        //     modifier = Modifier
                        //         .fillMaxWidth()
                        //         .padding(bottom = 16.dp)
                        // ) {
                        //     if (isFetching) {
                        //         CircularProgressIndicator(
                        //             modifier = Modifier.size(24.dp),
                        //             color = MaterialTheme.colorScheme.onPrimary
                        //         )
                        //         Spacer(modifier = Modifier.width(8.dp))
                        //         Text("Mengambil Data...")
                        //     } else {
                        //         Text("Ambil Data dari Smartwatch")
                        //     }
                        // }

                        if (error != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Manual Input Form
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("Input Manual", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Gender Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = genderExpanded,
                                    onExpandedChange = { genderExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = userData.gender,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Gender") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = genderExpanded,
                                        onDismissRequest = { genderExpanded = false }
                                    ) {
                                        genderOptions.forEach { gender ->
                                            DropdownMenuItem(
                                                text = { Text(gender) },
                                                onClick = {
                                                    onUserDataChange(userData.copy(gender = gender))
                                                    genderExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Age
                                OutlinedTextField(
                                    value = userData.age,
                                    onValueChange = { onUserDataChange(userData.copy(age = it)) },
                                    label = { Text("Age") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Sleep Duration with success indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = userData.sleepDuration,
                                        onValueChange = { onUserDataChange(userData.copy(sleepDuration = it)) },
                                        label = { Text("Sleep Duration (hours)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (sleep != null && sleep.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Data synced",
                                            tint = Color.Green,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Quality of Sleep
                                OutlinedTextField(
                                    value = userData.sleepQuality,
                                    onValueChange = { onUserDataChange(userData.copy(sleepQuality = it)) },
                                    label = { Text("Quality of Sleep (1-10)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // BMI Category
                                ExposedDropdownMenuBox(
                                    expanded = bmiExpanded,
                                    onExpandedChange = { bmiExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = userData.bmiCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("BMI Category") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bmiExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = bmiExpanded,
                                        onDismissRequest = { bmiExpanded = false }
                                    ) {
                                        bmiCategories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category) },
                                                onClick = {
                                                    onUserDataChange(userData.copy(bmiCategory = category))
                                                    bmiExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Heart Rate with success indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = userData.heartRate,
                                        onValueChange = { onUserDataChange(userData.copy(heartRate = it)) },
                                        label = { Text("Heart Rate (bpm)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (heartRate != null) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Data synced",
                                            tint = Color.Green,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Daily Steps with success indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = userData.dailySteps,
                                        onValueChange = { onUserDataChange(userData.copy(dailySteps = it)) },
                                        label = { Text("Daily Steps") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (steps != null) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Data synced",
                                            tint = Color.Green,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Sleep Disorder
                                ExposedDropdownMenuBox(
                                    expanded = sleepDisorderExpanded,
                                    onExpandedChange = { sleepDisorderExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = userData.sleepDisorder.ifEmpty { "None" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Sleep Disorder") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sleepDisorderExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sleepDisorderExpanded,
                                        onDismissRequest = { sleepDisorderExpanded = false }
                                    ) {
                                        sleepDisorderOptions.forEach { disorder ->
                                            DropdownMenuItem(
                                                text = { Text(disorder) },
                                                onClick = {
                                                    onUserDataChange(userData.copy(sleepDisorder = disorder))
                                                    sleepDisorderExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Screen.Profile -> {
                    ProfileScreen(
                        userData = userData,
                        onUserDataChange = onUserDataChange
                    )
                }
            }
        }
    }

    if (showFetchDialog) {
        AlertDialog(
            onDismissRequest = { showFetchDialog = false },
            title = { Text("Ambil Data dari Smartwatch") },
            text = { Text("Apakah Anda ingin mengambil data terbaru dari smartwatch?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFetchDialog = false
                        // Trigger fetch data here
                    }
                ) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFetchDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }
}
