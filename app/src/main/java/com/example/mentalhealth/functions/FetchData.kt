package com.example.mentalhealth.functions

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZoneId
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateRequest

suspend fun readStepsToday(context: Context, client: HealthConnectClient): Long {
    try {
        // Check if Health Connect is available by trying to get permissions
        client.permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        Log.e("FetchData", "Health Connect is not available: ${e.message}")
        return 0
    }

    return try {
        withTimeout(10000) { // 10 seconds timeout
            val now = Instant.now()
            val todayStart = now.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

            Log.d("FetchData", "Fetching steps data between $todayStart and $now")
            val resp = client.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.between(todayStart, now)
                )
            )
            val steps = resp.records.sumOf { it.count }
            Log.d("FetchData", "Fetched steps: $steps")
            steps
        }
    } catch (e: TimeoutCancellationException) {
        Log.e("FetchData", "Timeout while reading steps: ${e.message}")
        0
    } catch (e: Exception) {
        Log.e("FetchData", "Error reading steps: ${e.message}")
        0
    }
}

suspend fun readSleepToday(context: Context, client: HealthConnectClient): List<SleepSessionRecord> {
    try {
        // Check if Health Connect is available by trying to get permissions
        client.permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        Log.e("FetchData", "Health Connect is not available: ${e.message}")
        return emptyList()
    }

    return try {
        withTimeout(10000) { // 10 seconds timeout
            val now = Instant.now()
            val todayStart = now.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

            Log.d("FetchData", "Fetching sleep data between $todayStart and $now")
            val response = client.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    TimeRangeFilter.between(todayStart, now)
                )
            )
            Log.d("FetchData", "Fetched ${response.records.size} sleep records")
            response.records
        }
    } catch (e: TimeoutCancellationException) {
        Log.e("FetchData", "Timeout while reading sleep: ${e.message}")
        emptyList()
    } catch (e: Exception) {
        Log.e("FetchData", "Error reading sleep: ${e.message}")
        emptyList()
    }
}

suspend fun readHeartRateToday(context: Context, client: HealthConnectClient): Double {
    try {
        // Check if Health Connect is available by trying to get permissions
        client.permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        Log.e("FetchData", "Health Connect is not available: ${e.message}")
        return 0.0
    }

    return try {
        withTimeout(10000) { // 10 seconds timeout
            val now = Instant.now()
            val todayStart = now.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

            Log.d("FetchData", "Fetching heart rate data between $todayStart and $now using aggregate API")

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        HeartRateRecord.BPM_AVG,
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX
                    ),
                    timeRangeFilter = TimeRangeFilter.between(todayStart, now)
                )
            )

            val averageBpm = response[HeartRateRecord.BPM_AVG]
            Log.d("FetchData", "Raw heart rate data: $response")
            Log.d("FetchData", "Average BPM: $averageBpm")

            if (averageBpm == null) {
                Log.d("FetchData", "No average BPM data found for today (null result from aggregate)")
                return@withTimeout 0.0
            }

            averageBpm.toDouble()
        }
    } catch (e: TimeoutCancellationException) {
        Log.e("FetchData", "Timeout while reading heart rate: ${e.message}")
        0.0
    } catch (e: Exception) {
        Log.e("FetchData", "Error reading heart rate: ${e.message}")
        Log.e("FetchData", "Error stack trace:", e)
        0.0
    }
}