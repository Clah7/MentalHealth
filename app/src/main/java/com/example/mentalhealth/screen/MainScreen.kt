package com.example.mentalhealth.screen

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

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
    onUserDataChange: (UserData) -> Unit,
    permissionLauncher: ActivityResultLauncher<Set<String>>,
    requiredPermissions: Set<String>
) {
    val scrollState = rememberScrollState()
    var showFetchDialog by remember { mutableStateOf(false) }
    var showCelebrityDialog by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var bmiExpanded by remember { mutableStateOf(false) }
    var sleepDisorderExpanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var predictionResult by remember { mutableStateOf<String?>(null) }
    var showPredictionError by remember { mutableStateOf(false) }
    var predictionError by remember { mutableStateOf("") }

    val genderOptions = listOf("Male", "Female")
    val bmiCategories = listOf("Normal", "Overweight")
    val sleepDisorderOptions = listOf("Nothing", "Insomnia", "Sleep Apnea")

    // Local state for editable health values
    var editableSleepDuration by remember { mutableStateOf(userData.sleepDuration.ifEmpty { "0" }) }
    var editableHeartRate by remember { mutableStateOf(heartRate?.toInt()?.toString() ?: userData.heartRate.ifEmpty { "0" }) }
    var editableDailySteps by remember { mutableStateOf(steps?.toString() ?: userData.dailySteps.ifEmpty { "0" }) }

    // Track if the values were manually edited
    var isManuallyEditing by remember { mutableStateOf(false) }

    // Update local state when props change, but only if not manually editing
    LaunchedEffect(steps, sleep, heartRate, userData) {
        if (!isManuallyEditing) {
            Log.d("MainScreen", "Props changed - Steps: $steps, Heart Rate: $heartRate, UserData: $userData")
            editableSleepDuration = userData.sleepDuration.ifEmpty { "0" }
            editableHeartRate = heartRate?.toInt()?.toString() ?: userData.heartRate.ifEmpty { "0" }
            editableDailySteps = steps?.toString() ?: userData.dailySteps.ifEmpty { "0" }
            Log.d("MainScreen", "Updated local state - Sleep: $editableSleepDuration, Heart Rate: $editableHeartRate, Steps: $editableDailySteps")
        }
    }

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
                                    text = "Hello, ${userData.name.ifEmpty { "User" }}",
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
                                isManuallyEditing = true
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
                                isManuallyEditing = true
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
                                isManuallyEditing = true
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
                            value = userData.sleepDisorder.ifEmpty { "Nothing" },
                            options = sleepDisorderOptions,
                            expanded = sleepDisorderExpanded,
                            onExpandedChange = { sleepDisorderExpanded = it },
                            onOptionSelected = { option ->
                                onUserDataChange(userData.copy(sleepDisorder = option))
                                sleepDisorderExpanded = false
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Add Prediction Button at the bottom
                        Button(
                            onClick = {
                                // Validate all required fields
                                if (userData.gender.isEmpty() ||
                                    userData.age.isEmpty() ||
                                    userData.sleepDuration.isEmpty() ||
                                    userData.sleepQuality.isEmpty() ||
                                    userData.bmiCategory.isEmpty() ||
                                    userData.heartRate.isEmpty() ||
                                    userData.dailySteps.isEmpty() ||
                                    userData.sleepDisorder.isEmpty()
                                ) {
                                    showPredictionError = true
                                    predictionError = "Please fill in all required fields"
                                    return@Button
                                }

                                try {
                                    // Prepare input data with proper type conversion
                                    val inputData = StressPredictionRequest(
                                        Gender = userData.gender,
                                        Age = userData.age.toInt(),
                                        Sleep_Duration = userData.sleepDuration.toDouble(),
                                        Quality_of_Sleep = userData.sleepQuality.toInt(),
                                        BMI_Category = userData.bmiCategory,
                                        Heart_Rate = userData.heartRate.toInt(),
                                        Daily_Steps = userData.dailySteps.toInt(),
                                        Sleep_Disorder = userData.sleepDisorder
                                    )

                                    // Make API call
                                    ApiClient.api.predictStress(inputData).enqueue(object : Callback<StressPredictionResponse> {
                                        override fun onResponse(
                                            call: Call<StressPredictionResponse>,
                                            response: Response<StressPredictionResponse>
                                        ) {
                                            if (response.isSuccessful) {
                                                response.body()?.let { result ->
                                                    predictionResult = "Stress Level: ${result.predicted_stress_level}"
                                                    showPredictionError = false
                                                }
                                            } else {
                                                showPredictionError = true
                                                predictionError = "Failed to get prediction: ${response.code()} - ${response.message()}"
                                                // Log the error response body if available
                                                response.errorBody()?.string()?.let { errorBody ->
                                                    Log.e("API Error", "Error body: $errorBody")
                                                }
                                            }
                                        }

                                        override fun onFailure(call: Call<StressPredictionResponse>, t: Throwable) {
                                            showPredictionError = true
                                            predictionError = "Error: ${t.message}"
                                            Log.e("API Error", "Network error", t)
                                        }
                                    })
                                } catch (e: NumberFormatException) {
                                    showPredictionError = true
                                    predictionError = "Invalid number format in input fields"
                                    Log.e("API Error", "Number format error", e)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Predict Stress Level",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Show prediction result or error
                        if (predictionResult != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = predictionResult!!,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showPredictionError) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = predictionError,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

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
                        permissionLauncher.launch(requiredPermissions)
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
    var localValue by remember(value) { mutableStateOf(value) }

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
                value = localValue,
                onValueChange = { newValue ->
                    localValue = newValue
                    onValueChange(newValue)
                },
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = TextAlign.End,
                    fontSize = 16.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
        }
    }
}

// Update data classes for API request and response
data class StressPredictionRequest(
    val Gender: String,
    val Age: Int,
    val Sleep_Duration: Double,
    val Quality_of_Sleep: Int,
    val BMI_Category: String,
    val Heart_Rate: Int,
    val Daily_Steps: Int,
    val Sleep_Disorder: String
)

data class StressPredictionResponse(
    val predicted_stress_level: String
)

// Update API interface
interface StressPredictionApi {
    @POST("/predict")
    fun predictStress(@Body input: StressPredictionRequest): Call<StressPredictionResponse>
}

// Add API client
object ApiClient {
    private const val BASE_URL = "https://stress-api-265312655492.asia-southeast2.run.app/"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: StressPredictionApi = retrofit.create(StressPredictionApi::class.java)
}
