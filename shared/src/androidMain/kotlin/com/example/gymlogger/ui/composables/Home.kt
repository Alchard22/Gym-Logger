package com.example.gymlogger.ui.composables

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlogger.repository.GymRepository

@Composable
fun HomeScreen(repository: GymRepository, navController: NavController) {
    val context = LocalContext.current
    LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(0.dp) // No extra spacing between cards
    ) {
        item {
            ViewTrainingPlans(
                repository = repository,
                onPlanSelected = { planId ->
                    navController.navigate("training_plan_management/$planId")
                },
                onNavigateBack = {
                    (context as? Activity)?.finish() // or use proper navigation
                },
            )
        }

        item {
            AddTrainingPlanScreen(
                repository = repository,
                onPlanAdded = { planId ->
                    println("Created plan with ID: $planId")
                },
                onNavigateBack = {}
            )
        }
    }
}
