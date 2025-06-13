package com.example.mentalhealth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.mentalhealth.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val NAME = stringPreferencesKey("name")
        private val EMAIL = stringPreferencesKey("email")
        private val GENDER = stringPreferencesKey("gender")
        private val AGE = stringPreferencesKey("age")
        private val BMI_CATEGORY = stringPreferencesKey("bmi_category")
        private val SLEEP_DISORDER = stringPreferencesKey("sleep_disorder")
        private val SLEEP_DURATION = stringPreferencesKey("sleep_duration")
        private val SLEEP_QUALITY = stringPreferencesKey("sleep_quality")
        private val HEART_RATE = stringPreferencesKey("heart_rate")
        private val DAILY_STEPS = stringPreferencesKey("daily_steps")
        private val HEIGHT = stringPreferencesKey("height")
        private val WEIGHT = stringPreferencesKey("weight")
    }

    val userData: Flow<UserData> = context.dataStore.data.map { preferences ->
        UserData(
            name = preferences[NAME] ?: "",
            email = preferences[EMAIL] ?: "",
            gender = preferences[GENDER] ?: "",
            age = preferences[AGE] ?: "",
            bmiCategory = preferences[BMI_CATEGORY] ?: "",
            sleepDisorder = preferences[SLEEP_DISORDER] ?: "",
            sleepDuration = preferences[SLEEP_DURATION] ?: "",
            sleepQuality = preferences[SLEEP_QUALITY] ?: "",
            heartRate = preferences[HEART_RATE] ?: "",
            dailySteps = preferences[DAILY_STEPS] ?: "",
            height = preferences[HEIGHT] ?: "",
            weight = preferences[WEIGHT] ?: ""
        )
    }

    suspend fun saveUserData(userData: UserData) {
        context.dataStore.edit { preferences ->
            preferences[NAME] = userData.name
            preferences[EMAIL] = userData.email
            preferences[GENDER] = userData.gender
            preferences[AGE] = userData.age
            preferences[BMI_CATEGORY] = userData.bmiCategory
            preferences[SLEEP_DISORDER] = userData.sleepDisorder
            preferences[SLEEP_DURATION] = userData.sleepDuration
            preferences[SLEEP_QUALITY] = userData.sleepQuality
            preferences[HEART_RATE] = userData.heartRate
            preferences[DAILY_STEPS] = userData.dailySteps
            preferences[HEIGHT] = userData.height
            preferences[WEIGHT] = userData.weight
        }
    }
} 