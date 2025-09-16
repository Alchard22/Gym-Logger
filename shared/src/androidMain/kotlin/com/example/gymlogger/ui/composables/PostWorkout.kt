package com.example.gymlogger.ui.composables

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.gymlogger.repository.GymRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostWorkout(
    workoutSessionId: Long,
    repository: GymRepository,
    onNavigateBack: () -> Unit = {}
) {}