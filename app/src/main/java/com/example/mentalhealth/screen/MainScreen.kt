package com.example.mentalhealth.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    object Home : Screen("home", { Text("🏠") }, "Home")
    object Profile : Screen("profile", { Text("👤") }, "Profile")
}

@Composable
fun TwoColumnHealthCard(
    title: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    hasData: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column - Title
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            // Right Column - Value and Unit (stacked vertically)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.End,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.width(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
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
    var showCelebrityDialog by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var bmiExpanded by remember { mutableStateOf(false) }
    var sleepDisorderExpanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val genderOptions = listOf("Male", "Female")
    val bmiCategories = listOf("Normal", "Overweight")
    val sleepDisorderOptions = listOf("None", "Insomnia", "Sleep Apnea")

    // Local state for editable health values
    var editableSleepDuration by remember { mutableStateOf(userData.sleepDuration.ifEmpty { "4" }) }
    var editableHeartRate by remember { mutableStateOf(heartRate?.toInt()?.toString() ?: userData.heartRate.ifEmpty { "98" }) }
    var editableDailySteps by remember { mutableStateOf(steps?.toString() ?: userData.dailySteps.ifEmpty { "4300" }) }

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
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header Section with Sync Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hello, Sharine",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Sync Button
                            IconButton(
                                onClick = { showFetchDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = "\uD83D\uDD04",
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sleep Duration Card (Two Column)
                        TwoColumnHealthCard(
                            title = "Sleep\nDuration",
                            value = editableSleepDuration,
                            unit = "hours",
                            onValueChange = {
                                editableSleepDuration = it
                                onUserDataChange(userData.copy(sleepDuration = it))
                            },
                            hasData = sleep != null && sleep.isNotEmpty()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Heart Rate Card (Two Column)
                        TwoColumnHealthCard(
                            title = "Heart\nRate",
                            value = editableHeartRate,
                            unit = "bpm",
                            onValueChange = {
                                editableHeartRate = it
                                onUserDataChange(userData.copy(heartRate = it))
                            },
                            hasData = heartRate != null
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Daily Steps Card (Two Column)
                        TwoColumnHealthCard(
                            title = "Daily\nSteps",
                            value = editableDailySteps,
                            unit = "steps",
                            onValueChange = {
                                editableDailySteps = it
                                onUserDataChange(userData.copy(dailySteps = it))
                            },
                            hasData = steps != null
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // BMI Category Dropdown Card
                        DropdownCard(
                            title = "BMI Category",
                            value = userData.bmiCategory.ifEmpty { "Normal" },
                            options = bmiCategories,
                            expanded = bmiExpanded,
                            onExpandedChange = { bmiExpanded = it },
                            onOptionSelected = { option ->
                                onUserDataChange(userData.copy(bmiCategory = option))
                                bmiExpanded = false
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Gender Dropdown Card
                        DropdownCard(
                            title = "Gender",
                            value = userData.gender.ifEmpty { "Select Gender" },
                            options = genderOptions,
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = it },
                            onOptionSelected = { option ->
                                onUserDataChange(userData.copy(gender = option))
                                genderExpanded = false
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Age Input Card
                        InputCard(
                            title = "Age",
                            value = userData.age,
                            onValueChange = { onUserDataChange(userData.copy(age = it)) },
                            placeholder = "Enter your age"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sleep Quality Input Card
                        InputCard(
                            title = "Sleep Quality",
                            value = userData.sleepQuality,
                            onValueChange = { onUserDataChange(userData.copy(sleepQuality = it)) },
                            placeholder = "Rate 1-10"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sleep Disorder Dropdown Card
                        DropdownCard(
                            title = "Sleep Disorder",
                            value = userData.sleepDisorder.ifEmpty { "None" },
                            options = sleepDisorderOptions,
                            expanded = sleepDisorderExpanded,
                            onExpandedChange = { sleepDisorderExpanded = it },
                            onOptionSelected = { option ->
                                onUserDataChange(userData.copy(sleepDisorder = option))
                                sleepDisorderExpanded = false
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (error != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
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
            title = { Text("Sync Data from Smartwatch") },
            text = { Text("Do you want to fetch the latest data from your smartwatch?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFetchDialog = false
                        // Trigger fetch data here
                    }
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Yes")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFetchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCelebrityDialog) {
        AlertDialog(
            onDismissRequest = { showCelebrityDialog = false },
            title = { Text("Celebrity Training") },
            text = { Text("Great choice! Let's start your celebrity training program.") },
            confirmButton = {
                TextButton(
                    onClick = { showCelebrityDialog = false }
                ) {
                    Text("Let's Go!")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownCard(
    title: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
                    .padding(16.dp)
                    .menuAnchor(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = value,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        }
    }
}

@Composable
fun InputCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = TextAlign.End,
                    fontSize = 16.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}