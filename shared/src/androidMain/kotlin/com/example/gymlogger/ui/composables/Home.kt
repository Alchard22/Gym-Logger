package com.example.gymlogger.ui.composables

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlogger.repository.GymRepository
import com.example.gymlogger.ui.composables.analytics.Analytics

@Composable
fun HomeScreen(repository: GymRepository, navController: NavController) {
    val context = LocalContext.current

    LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(0.dp),// No extra spacing between cards
    horizontalAlignment = Alignment.CenterHorizontally
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

        item {
            Button({ navController.navigate("analytics/0") }, shape = MaterialTheme.shapes.medium) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                text = "Analytics Page",
                style = MaterialTheme.typography.titleMedium
            ) }
        }
    }
}
