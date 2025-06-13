package com.example.mentalhealth.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentalhealth.model.UserData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    currentName: String,
    currentEmail: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameValue by remember { mutableStateOf(currentName) }
    var emailValue by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emailValue,
                    onValueChange = { emailValue = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(nameValue, emailValue)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userData: UserData,
    onUserDataChange: (UserData) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Picture
        Box(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        // Profile Header with Name and Email
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Name with Edit Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = userData.name.ifEmpty { "Enter Name" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { 
                            editingField = "profile"
                            showEditDialog = true 
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email
                Text(
                    text = userData.email.ifEmpty { "Enter Email" },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Profile Data Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gender and Age Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = userData.gender.ifEmpty { "Gender" },
                    value = "",
                    isDropdown = true,
                    onClick = {
                        editingField = "gender"
                        showEditDialog = true
                    }
                )
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = "",
                    value = userData.age.ifEmpty { "18" },
                    unit = "years old",
                    onClick = {
                        editingField = "age"
                        editingValue = userData.age
                        showEditDialog = true
                    }
                )
            }

            // Height and Weight Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = "",
                    value = userData.height.ifEmpty { "167" },
                    unit = "cm",
                    onClick = {
                        editingField = "height"
                        editingValue = userData.height
                        showEditDialog = true
                    }
                )
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = "",
                    value = userData.weight.ifEmpty { "58" },
                    unit = "kg",
                    onClick = {
                        editingField = "weight"
                        editingValue = userData.weight
                        showEditDialog = true
                    }
                )
            }

            // BMI and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = "BMI",
                    value = "",
                    isDark = true,
                    onClick = {
                        editingField = "bmi"
                        editingValue = userData.bmiCategory
                        showEditDialog = true
                    }
                )
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = userData.bmiCategory.ifEmpty { "Normal" },
                    value = "",
                    onClick = {
                        editingField = "bmiCategory"
                        showEditDialog = true
                    }
                )
            }

            // Sleep Disorder Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = "Sleep Disorder",
                    value = "",
                    isDark = true,
                    onClick = {
                        editingField = "sleepDisorder"
                        showEditDialog = true
                    }
                )
                ProfileDataCard(
                    modifier = Modifier.weight(1f),
                    label = userData.sleepDisorder.ifEmpty { "Insomnia" },
                    value = "",
                    onClick = {
                        editingField = "sleepDisorder"
                        showEditDialog = true
                    }
                )
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        when (editingField) {
            "profile" -> {
                ProfileEditDialog(
                    currentName = userData.name,
                    currentEmail = userData.email,
                    onSave = { name, email ->
                        onUserDataChange(userData.copy(name = name, email = email))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            "gender" -> {
                GenderSelectionDialog(
                    currentGender = userData.gender,
                    onGenderSelected = { gender ->
                        onUserDataChange(userData.copy(gender = gender))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            "age" -> {
                NumberInputDialog(
                    title = "Enter Age",
                    currentValue = editingValue,
                    onValueChanged = { value ->
                        onUserDataChange(userData.copy(age = value))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            "height" -> {
                NumberInputDialog(
                    title = "Enter Height (cm)",
                    currentValue = userData.height,
                    onValueChanged = { value ->
                        onUserDataChange(userData.copy(height = value))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            "weight" -> {
                NumberInputDialog(
                    title = "Enter Weight (kg)",
                    currentValue = userData.weight,
                    onValueChanged = { value ->
                        onUserDataChange(userData.copy(weight = value))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            "bmiCategory" -> {
                BMICategorySelectionDialog(
                    currentCategory = userData.bmiCategory,
                    onCategorySelected = { category ->
                        onUserDataChange(userData.copy(bmiCategory = category))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            "sleepDisorder" -> {
                SleepDisorderSelectionDialog(
                    currentDisorder = userData.sleepDisorder,
                    onDisorderSelected = { disorder ->
                        onUserDataChange(userData.copy(sleepDisorder = disorder))
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
        }
    }
}

@Composable
fun ProfileDataCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String = "",
    isDark: Boolean = false,
    isDropdown: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (label.isNotEmpty() && value.isEmpty()) {
                // Show only label (like "BMI", "Sleep Disorder", "Female")
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            } else if (value.isNotEmpty()) {
                // Show value with unit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = unit,
                            fontSize = 12.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
    title: String,
    currentValue: String,
    onValueChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValueChanged(textValue)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberInputDialog(
    title: String,
    currentValue: String,
    onValueChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValueChanged(value)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GenderSelectionDialog(
    currentGender: String,
    onGenderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val genderOptions = listOf("Male", "Female")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Gender") },
        text = {
            Column {
                genderOptions.forEach { gender ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGenderSelected(gender) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentGender == gender,
                            onClick = { onGenderSelected(gender) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(gender)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BMICategorySelectionDialog(
    currentCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("Underweight", "Normal", "Overweight", "Obese")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select BMI Category") },
        text = {
            Column {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentCategory == category,
                            onClick = { onCategorySelected(category) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SleepDisorderSelectionDialog(
    currentDisorder: String,
    onDisorderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val disorders = listOf("Nothing", "Insomnia", "Sleep Apnea")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Sleep Disorder") },
        text = {
            Column {
                disorders.forEach { disorder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDisorderSelected(disorder) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentDisorder == disorder || (currentDisorder.isEmpty() && disorder == "None"),
                            onClick = { onDisorderSelected(disorder) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(disorder)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}