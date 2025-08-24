package com.example.gymlogger.ui.composables

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymlogger.ui.Animations
import com.example.gymlogger.repository.GymRepository
import database.TrainingPlan
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ViewTrainingPlans(
    repository: GymRepository,
    onPlanSelected: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val animations = Animations()
    var isExpanded by remember { mutableStateOf(false) }
    var mostRecentPlan by remember { mutableStateOf<TrainingPlan?>(null) }

    val trainingPlans by repository.getAllTrainingPlans().collectAsState(initial = emptyList())
    val sortedPlans = trainingPlans.sortedByDescending { it.start_date }
    val allWorkoutSessions by repository.getAllWorkoutSessions().collectAsState(initial = emptyList())

    // Determine the most recent plan based on workout activity
    LaunchedEffect(trainingPlans, allWorkoutSessions) {
        mostRecentPlan = when {
            trainingPlans.isEmpty() -> null
            allWorkoutSessions.isEmpty() -> sortedPlans.firstOrNull()
            else -> {
                // Find the plan with the most recent workout session
                val planWithMostRecentWorkout = allWorkoutSessions
                    .sortedByDescending { it.date }
                    .firstNotNullOfOrNull { session ->
                        // Handle both regular plans and sessions without a plan (PrePlan training)
                        session.training_plan_id?.let { planId ->
                            trainingPlans.find { it.id == planId }
                        }
                    }

                // If no plan has workout sessions, fall back to most recent plan by creation date
                planWithMostRecentWorkout ?: sortedPlans.firstOrNull()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Card Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                // Header - Always Visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (sortedPlans.size > 1) {
                                isExpanded = !isExpanded
                            }
                        }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue Training Plan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!isExpanded && sortedPlans.size > 1) {
                            Text(
                                text = "Tap to see more plans",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Most Recent Plan (Always Visible)
                mostRecentPlan?.let { plan ->
                    TrainingPlanCard(
                        plan = plan,
                        isRecent = true,
                        onClick = { onPlanSelected(plan.id) }
                    )
                }

                // No Plans Message
                if (sortedPlans.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Training Plans Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first training plan to get started with your fitness journey.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Expanded Plans List
                AnimatedVisibility(
                    visible = isExpanded && sortedPlans.size > 1,
                    enter = animations.fadeInMenu,
                    exit = animations.fadeOutMenu
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "All Training Plans",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )

                        // Show all plans except the first one (since it's already shown above)
                        sortedPlans.drop(1).forEach { plan ->
                            TrainingPlanCard(
                                plan = plan,
                                isRecent = false,
                                onClick = { onPlanSelected(plan.id) }
                            )
                        }
                    }
                }
            }
        }

        // Plan Type Selection (Stubbed for future implementation)
        AnimatedVisibility(
            visible = false, // Keep this false until other plan types are implemented
            enter = animations.fadeInForm
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                when (PlanType.SELF_DIRECTED) { // Stubbed for future plan types
                    PlanType.SELF_DIRECTED -> {
                        // Self-directed plan content would go here
                    }
                    PlanType.PROGRAMMED -> {
                        // Programmed plan content would go here
                    }
                    PlanType.PRE_BUILT -> {
                        // Pre-built plan content would go here
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingPlanCard(
    plan: TrainingPlan,
    isRecent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isRecent) 16.dp else 0.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isRecent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecent) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (isRecent) {
                        Text(
                            text = "Most Recent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isRecent) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.height(36.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Plan Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Started: ${formatDate(Instant.fromEpochMilliseconds(plan.start_date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecent) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        }
                    )

                    plan.end_date?.let { endDate ->
                        Text(
                            text = "Ended: ${formatDate(Instant.fromEpochMilliseconds(endDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRecent) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            }
                        )
                    }
                }

                Text(
                    text = "Self-Directed", // Currently all plans are self-directed
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRecent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    fontWeight = FontWeight.Medium
                )
            }

            // Description if available
            plan.description?.let { description ->
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRecent) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        }
                    )
                }
            }
        }
    }
}

private fun formatDate(instant: Instant): String {
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = localDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "${month.substring(0, 3)} ${localDate.dayOfMonth}, ${localDate.year}"
}