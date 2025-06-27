package com.example.mentalhealth.screen

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mentalhealth.R
import com.example.mentalhealth.model.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


class StressViewModel : ViewModel() {
    var predictionResult by mutableStateOf<String?>(null)
    var predictionError by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
}

sealed class Screen(val icon: @Composable () -> Unit, val label: String) {
    object Home : Screen({ Text("🏠") }, "Home")
    object Profile : Screen({ Text("👤") }, "Profile")
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

    val genderOptions = listOf("Male", "Female")
    val bmiCategories = listOf("Normal", "Overweight", "Obese")
    val sleepDisorderOptions = listOf("Nothing", "Insomnia", "Sleep Apnea")

    val viewModel: StressViewModel = viewModel()

    // Initialize local state with better fallback logic
    var editableSleepDuration by remember {
        mutableStateOf(
            if (userData.sleepDuration.isNotEmpty()) userData.sleepDuration
            else ""
        )
    }

    var editableHeartRate by remember {
        mutableStateOf(
            if (userData.heartRate.isNotEmpty()) userData.heartRate
            else ""
        )
    }

    var editableDailySteps by remember {
        mutableStateOf(
            if (userData.dailySteps.isNotEmpty()) userData.dailySteps
            else steps?.toString() ?: ""
        )
    }

    // Track if the values were manually edited
    var isManuallyEditing by remember { mutableStateOf(false) }

    // Track if fields are locked or editable - START AS LOCKED (false means locked)
    var isEditMode by remember { mutableStateOf(false) }

    // Update local state when props change, but only if not manually editing
    // AND only if the new values are actually available (not null)
    LaunchedEffect(steps, sleep, heartRate, userData) {
        if (!isManuallyEditing) {
            Log.d(
                "MainScreen",
                "Props changed - Steps: $steps, Heart Rate: $heartRate, UserData: $userData"
            )

            // Only update if userData has values, otherwise keep current values
            if (userData.sleepDuration.isNotEmpty()) {
                editableSleepDuration = userData.sleepDuration
            }

            // For heart rate, prioritize userData, then heartRate prop, then keep current
            if (userData.heartRate.isNotEmpty()) {
                editableHeartRate = userData.heartRate
            } else if (heartRate != null) {
                editableHeartRate = heartRate.toInt().toString()
            }
            // If both are null/empty, keep the current editableHeartRate value

            // For steps, prioritize userData, then steps prop, then keep current
            if (userData.dailySteps.isNotEmpty()) {
                editableDailySteps = userData.dailySteps
            } else if (steps != null) {
                editableDailySteps = steps.toString()
            }
            // If both are null/empty, keep the current editableDailySteps value

            Log.d(
                "MainScreen",
                "Updated local state - Sleep: $editableSleepDuration, Heart Rate: $editableHeartRate, Steps: $editableDailySteps"
            )
        }
    }

    // Reset manual editing state when sync is triggered
    LaunchedEffect(isFetching) {
        if (isFetching) {
            isManuallyEditing = false
        }
    }

    // Di MainScreen, tambahkan state error untuk setiap field required
    var sleepDurationError by remember { mutableStateOf(false) }
    var heartRateError by remember { mutableStateOf(false) }
    var dailyStepsError by remember { mutableStateOf(false) }
    var genderError by remember { mutableStateOf(false) }
    var ageError by remember { mutableStateOf(false) }
    var sleepQualityError by remember { mutableStateOf(false) }
    var bmiCategoryError by remember { mutableStateOf(false) }
    var sleepDisorderError by remember { mutableStateOf(false) }

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
                            .fillMaxSize()
                    ) {
                        // Header Section with Sync Button and Edit Toggle (sticky)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Edit/Lock Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { isEditMode = !isEditMode }
                                ) {
                                    Icon(
                                        painter = if (!isEditMode) painterResource(id = R.drawable.edit) else painterResource(id = R.drawable.check),
                                        contentDescription = if (!isEditMode) "Edit" else "Lock",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(if (!isEditMode) 24.dp else 28.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
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
                                    Icon(
                                        painter = painterResource(id = R.drawable.sync),
                                        modifier = Modifier.size(18.dp),
                                        contentDescription = "Sync"
                                    )
                                }
                            }
                        }
                        // Scrollable content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Spacer(modifier = Modifier.height(0.dp))
                            // Sleep Duration
                            InputCard(
                                title = "Sleep Duration",
                                value = editableSleepDuration,
                                onValueChange = {
                                    if (isEditMode) {
                                        isManuallyEditing = true
                                        editableSleepDuration = it
                                        onUserDataChange(userData.copy(sleepDuration = it))
                                        sleepDurationError = it.isEmpty()
                                    }
                                },
                                placeholder = "...",
                                isEditMode = isEditMode,
                                helperText = "hours (e.g. 7.2)",
                                valueFontSize = 24.sp,
                                valueFontWeight = FontWeight.Bold,
                                isError = sleepDurationError
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Heart Rate
                            InputCard(
                                title = "Heart Rate",
                                value = editableHeartRate,
                                onValueChange = {
                                    if (isEditMode) {
                                        isManuallyEditing = true
                                        editableHeartRate = it
                                        onUserDataChange(userData.copy(heartRate = it))
                                        heartRateError = it.isEmpty()
                                    }
                                },
                                placeholder = "...",
                                isEditMode = isEditMode,
                                helperText = "bpm",
                                valueFontSize = 24.sp,
                                valueFontWeight = FontWeight.Bold,
                                isError = heartRateError
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Daily Steps
                            InputCard(
                                title = "Daily Steps",
                                value = editableDailySteps,
                                onValueChange = {
                                    if (isEditMode) {
                                        isManuallyEditing = true
                                        editableDailySteps = it
                                        onUserDataChange(userData.copy(dailySteps = it))
                                        dailyStepsError = it.isEmpty()
                                    }
                                },
                                placeholder = "...",
                                isEditMode = isEditMode,
                                helperText = "steps",
                                valueFontSize = 24.sp,
                                valueFontWeight = FontWeight.Bold,
                                isError = dailyStepsError
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // BMI Category Dropdown Card
                            DropdownCard(
                                title = "BMI Category",
                                value = userData.bmiCategory.ifEmpty { "Normal" },
                                options = bmiCategories,
                                expanded = bmiExpanded,
                                onExpandedChange = { if (isEditMode) bmiExpanded = it },
                                onOptionSelected = { option ->
                                    if (isEditMode) {
                                        onUserDataChange(userData.copy(bmiCategory = option))
                                        bmiExpanded = false
                                    }
                                },
                                isEditMode = isEditMode
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Gender Dropdown Card
                            DropdownCard(
                                title = "Gender",
                                value = userData.gender.ifEmpty { "Select Gender" },
                                options = genderOptions,
                                expanded = genderExpanded,
                                onExpandedChange = { if (isEditMode) genderExpanded = it },
                                onOptionSelected = { option ->
                                    if (isEditMode) {
                                        onUserDataChange(userData.copy(gender = option))
                                        genderExpanded = false
                                    }
                                },
                                isEditMode = isEditMode
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Age Input Card
                            InputCard(
                                title = "Age",
                                value = userData.age,
                                onValueChange = {
                                    if (isEditMode) {
                                        onUserDataChange(userData.copy(age = it))
                                        ageError = it.isEmpty()
                                    }
                                },
                                placeholder = "...",
                                isEditMode = isEditMode,
                                isError = ageError
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sleep Quality Input Card
                            InputCard(
                                title = "Sleep Quality",
                                value = userData.sleepQuality,
                                onValueChange = {
                                    if (isEditMode) {
                                        onUserDataChange(userData.copy(sleepQuality = it))
                                        sleepQualityError = it.isEmpty()
                                    }
                                },
                                placeholder = "...",
                                isEditMode = isEditMode,
                                helperText = "Rate 1-10",
                                isError = sleepQualityError
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sleep Disorder Dropdown Card
                            DropdownCard(
                                title = "Sleep Disorder",
                                value = userData.sleepDisorder.ifEmpty { "Nothing" },
                                options = sleepDisorderOptions,
                                expanded = sleepDisorderExpanded,
                                onExpandedChange = { if (isEditMode) sleepDisorderExpanded = it },
                                onOptionSelected = { option ->
                                    if (isEditMode) {
                                        onUserDataChange(userData.copy(sleepDisorder = option))
                                        sleepDisorderExpanded = false
                                    }
                                },
                                isEditMode = isEditMode
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Prediction Button
                            Button(
                                onClick = {
                                    // Validate all required fields
                                    sleepDurationError = editableSleepDuration.isEmpty()
                                    heartRateError = editableHeartRate.isEmpty()
                                    dailyStepsError = editableDailySteps.isEmpty()
                                    genderError = userData.gender.isEmpty()
                                    ageError = userData.age.isEmpty()
                                    sleepQualityError = userData.sleepQuality.isEmpty()
                                    bmiCategoryError = userData.bmiCategory.isEmpty()
                                    sleepDisorderError = userData.sleepDisorder.isEmpty()

                                    Log.d("PredictDebug", "gender='${userData.gender}', age='${userData.age}', sleepDuration='${editableSleepDuration}', sleepQuality='${userData.sleepQuality}', bmiCategory='${userData.bmiCategory}', heartRate='${editableHeartRate}', dailySteps='${editableDailySteps}', sleepDisorder='${userData.sleepDisorder}'")
                                    Log.d("PredictDebug", "genderError=$genderError, ageError=$ageError, sleepDurationError=$sleepDurationError, sleepQualityError=$sleepQualityError, bmiCategoryError=$bmiCategoryError, heartRateError=$heartRateError, dailyStepsError=$dailyStepsError, sleepDisorderError=$sleepDisorderError")

                                    // Validasi khusus
                                    var crossCheckError = false
                                    // Sleep Quality 1-10
                                    val sleepQualityInt = userData.sleepQuality.toIntOrNull()
                                    if (!sleepQualityError && (sleepQualityInt == null || sleepQualityInt < 1 || sleepQualityInt > 10)) {
                                        sleepQualityError = true
                                        crossCheckError = true
                                    }
                                    // Sleep Duration <= 24
                                    val sleepDurationDouble = editableSleepDuration.toDoubleOrNull()
                                    if (!sleepDurationError && (sleepDurationDouble == null || sleepDurationDouble > 24.0)) {
                                        sleepDurationError = true
                                        crossCheckError = true
                                    }
                                    // Age 3-100
                                    val ageInt = userData.age.toIntOrNull()
                                    if (!ageError && (ageInt == null || ageInt < 3 || ageInt > 100)) {
                                        ageError = true
                                        crossCheckError = true
                                    }

                                    if (genderError || ageError || sleepDurationError || sleepQualityError || bmiCategoryError || heartRateError || dailyStepsError || sleepDisorderError) {
                                        viewModel.predictionError = if (crossCheckError) "Crosscheck your input" else "Please fill in all required fields"
                                        viewModel.predictionResult = null
                                        return@Button
                                    }

                                    try {
                                        // Create the request using current values
                                        val inputData = StressPredictionRequest(
                                            Gender = userData.gender,
                                            Age = userData.age.toInt(),
                                            Sleep_Duration = editableSleepDuration.toDouble(),
                                            Quality_of_Sleep = userData.sleepQuality.toInt(),
                                            BMI_Category = userData.bmiCategory,
                                            Heart_Rate = editableHeartRate.toInt(),
                                            Daily_Steps = editableDailySteps.toInt(),
                                            Sleep_Disorder = userData.sleepDisorder
                                        )

                                        viewModel.isLoading = true
                                        viewModel.predictionError = null

                                        // Make API call
                                        ApiClient.api.predictStress(inputData)
                                            .enqueue(object : Callback<StressPredictionResponse> {
                                                override fun onResponse(
                                                    call: Call<StressPredictionResponse>,
                                                    response: Response<StressPredictionResponse>
                                                ) {
                                                    viewModel.isLoading = false
                                                    if (response.isSuccessful) {
                                                        response.body()?.let { result ->
                                                            viewModel.predictionResult =
                                                                "Stress Level: ${result.predicted_stress_level}"
                                                            viewModel.predictionError = null
                                                        }
                                                    } else {
                                                        viewModel.predictionError =
                                                            "Failed to get prediction: ${response.code()} - ${response.message()}"
                                                        viewModel.predictionResult = null
                                                        // Log the error response body if available
                                                        response.errorBody()?.string()
                                                            ?.let { errorBody ->
                                                                Log.e(
                                                                    "API Error",
                                                                    "Error body: $errorBody"
                                                                )
                                                            }
                                                    }
                                                }

                                                override fun onFailure(
                                                    call: Call<StressPredictionResponse>,
                                                    t: Throwable
                                                ) {
                                                    viewModel.isLoading = false
                                                    viewModel.predictionError = "Error: ${t.message}"
                                                    viewModel.predictionResult = null
                                                    Log.e("API Error", "Network error", t)
                                                }
                                            })
                                    } catch (e: NumberFormatException) {
                                        viewModel.isLoading = false
                                        viewModel.predictionError =
                                            "Invalid number format in input fields"
                                        viewModel.predictionResult = null
                                        Log.e("API Error", "Number format error", e)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = !viewModel.isLoading
                            ) {
                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        text = "Predict Stress Level",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Show prediction result
                            if (viewModel.predictionResult != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom=16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        text = viewModel.predictionResult!!,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Show prediction error
                            if (viewModel.predictionError != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = viewModel.predictionError!!,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            // Show sync error
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
    onOptionSelected: (String) -> Unit,
    isEditMode: Boolean = false
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
                    .clickable(enabled = isEditMode) { onExpandedChange(!expanded) }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = value,
                        fontSize = 16.sp,
                        color = if (isEditMode)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (isEditMode) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                }
            }

            if (isEditMode) {
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
}

@Composable
fun InputCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isEditMode: Boolean = false,
    helperText: String? = null,
    valueFontSize: TextUnit = 16.sp,
    valueFontWeight: FontWeight = FontWeight.Normal,
    isError: Boolean = false
) {
    var localValue by remember(value) { mutableStateOf(value) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isError) MaterialTheme.colorScheme.error.copy(0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom kiri: title + helper text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (helperText != null) {
                    Text(
                        text = helperText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // Input field di kanan
            OutlinedTextField(
                value = localValue,
                onValueChange = { newValue ->
                    localValue = newValue
                    onValueChange(newValue)
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.width(100.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = TextAlign.End,
                    fontSize = valueFontSize,
                    fontWeight = valueFontWeight
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent
                ),
                singleLine = true,
                enabled = isEditMode,
                isError = isError
            )
        }
    }
}

// Data classes for API request and response
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

// API interface
interface StressPredictionApi {
    @POST("/predict")
    fun predictStress(@Body input: StressPredictionRequest): Call<StressPredictionResponse>
}

// API client
object ApiClient {
    private const val BASE_URL = "https://stress-api-265312655492.asia-southeast2.run.app "

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: StressPredictionApi = retrofit.create(StressPredictionApi::class.java)
}