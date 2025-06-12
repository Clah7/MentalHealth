package com.example.mentalhealth.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mentalhealth.R
import com.example.mentalhealth.model.UserData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userData: UserData,
    onUserDataChange: (UserData) -> Unit
) {
    var showPersonalEditDialog by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female")
    val bmiCategories = listOf("Normal", "Overweight")
    val sleepDisorderOptions = listOf("None", "Insomnia", "Sleep Apnea")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header Section
        Box(modifier = Modifier.padding(bottom = 24.dp)) {
            Image(
                painter = painterResource(id = R.drawable.profile_placeholder),
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
            IconButton(
                onClick = { /* TODO: Handle profile picture edit */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile Picture",
                    tint = Color.White
                )
            }
        }

        Text(
            text = "Shane", // Placeholder for Name
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "shane.sine@gmail.com", // Placeholder for Email
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Metrics Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem("2h 30m", "Total time", R.drawable.ic_hourglass_empty)
            MetricItem("7200 cal", "Burned", R.drawable.ic_fire)
            MetricItem("2", "Done", R.drawable.ic_fitness_center)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // List Items (Personal, General, Notification, Help)
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileListItem(
                icon = Icons.Default.Person,
                title = "Personal",
                onClick = { showPersonalEditDialog = true }
            )
            ProfileListItem(
                icon = Icons.Default.Info,
                title = "Help",
                onClick = { /* Handle Help */ }
            )
        }
    }

    // Dialog for Personal Information Edit
    if (showPersonalEditDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalEditDialog = false },
            title = { Text("Edit Personal Information") },
            text = {
                Column {
                    // Gender Dropdown
                    var genderExpanded by remember { mutableStateOf(false) }
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

                    // BMI Category
                    var bmiExpanded by remember { mutableStateOf(false) }
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

                    // Sleep Disorder
                    var sleepDisorderExpanded by remember { mutableStateOf(false) }
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
            },
            confirmButton = {
                TextButton(onClick = { showPersonalEditDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun MetricItem(value: String, label: String, iconRes: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileListItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    MaterialTheme {
        ProfileScreen(UserData(), {})
    }
} 