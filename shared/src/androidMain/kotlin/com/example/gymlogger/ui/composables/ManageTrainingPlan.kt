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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymlogger.ui.Animations
import com.example.gymlogger.repository.GymRepository
import database.TrainingPlan
import database.WorkoutSession
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class ManagementAction {
    START_WORKOUT,
    VIEW_WORKOUTS,
    EDIT_PLAN,
    END_PLAN,
    DELETE_PLAN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTrainingPlan(
    planId: Long,
    repository: GymRepository,
    onStartWorkout: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val animations = Animations()
    var isExpanded by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<ManagementAction?>(null) }
    var trainingPlan by remember { mutableStateOf<TrainingPlan?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val workoutSessions by repository.getWorkoutSessionsForPlan(planId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Load training plan details
    LaunchedEffect(planId) {
        trainingPlan = repository.getTrainingPlan(planId)
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    trainingPlan?.let { plan ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Main Management Card
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
                                    if (selectedAction == null) {
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
                                    text = plan.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (!isExpanded && selectedAction == null) {
                                    Text(
                                        text = "Tap to manage plan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Plan Info Summary
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Started: ${formatDate(Instant.fromEpochMilliseconds(plan.start_date))}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        plan.end_date?.let { endDate ->
                                            Text(
                                                text = "Ended: ${formatDate(Instant.fromEpochMilliseconds(endDate))}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${workoutSessions.size} Workouts",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = if (plan.end_date == null) "Active" else "Completed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                plan.description?.let { description ->
                                    if (description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Start Workout (if plan is active)
                        if (plan.end_date == null) {
                            Button(
                                onClick = { onStartWorkout(planId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Start New Workout",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            ManagementActionCard(
                                title = "View Workout History",
                                description = "See all ${workoutSessions.size} previous workouts",
                                onClick = { selectedAction = ManagementAction.VIEW_WORKOUTS }
                            )
                        }

                        // Management Options
                        AnimatedVisibility(
                            visible = isExpanded && selectedAction == null,
                            enter = animations.fadeInMenu,
                            exit = animations.fadeOutMenu
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ManagementActionCard(
                                    title = "Edit Plan Details",
                                    description = "Rename or update plan description",
                                    onClick = { selectedAction = ManagementAction.EDIT_PLAN }
                                )

                                if (plan.end_date == null) {
                                    ManagementActionCard(
                                        title = "End Training Plan",
                                        description = "Mark this plan as completed",
                                        onClick = { selectedAction = ManagementAction.END_PLAN }
                                    )
                                }

                                ManagementActionCard(
                                    title = "Delete Training Plan",
                                    description = "Permanently remove this plan and all workouts",
                                    isDestructive = true,
                                    onClick = { selectedAction = ManagementAction.DELETE_PLAN }
                                )
                            }
                        }
                    }
                }
            }
            item {
                // Action Forms
                AnimatedVisibility(
                    visible = selectedAction != null,
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
                        when (selectedAction) {
                            ManagementAction.VIEW_WORKOUTS -> ViewWorkoutsForm(
                                workoutSessions = workoutSessions,
                                onNavigateBack = { selectedAction = null }
                            )

                            ManagementAction.EDIT_PLAN -> EditPlanForm(
                                plan = plan,
                                repository = repository,
                                onPlanUpdated = { updatedPlan ->
                                    trainingPlan = updatedPlan
                                    selectedAction = null
                                },
                                onNavigateBack = { selectedAction = null }
                            )

                            ManagementAction.END_PLAN -> EndPlanForm(
                                plan = plan,
                                repository = repository,
                                onPlanEnded = { updatedPlan ->
                                    trainingPlan = updatedPlan
                                    selectedAction = null
                                },
                                onNavigateBack = { selectedAction = null }
                            )

                            ManagementAction.DELETE_PLAN -> DeletePlanForm(
                                plan = plan,
                                repository = repository,
                                onPlanDeleted = { onNavigateBack() },
                                onNavigateBack = { selectedAction = null }
                            )

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementActionCard(
    title: String,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                }
            )
        }
    }
}

@Composable
private fun ViewWorkoutsForm(
    workoutSessions: List<WorkoutSession>,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workout History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateBack) {
                Text("Back")
            }
        }

        if (workoutSessions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Workouts Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Start your first workout to see it here",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workoutSessions.sortedByDescending { it.date }) { session ->
                    WorkoutSessionCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionCard(session: WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(Instant.fromEpochMilliseconds(session.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            session.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlanForm(
    plan: TrainingPlan,
    repository: GymRepository,
    onPlanUpdated: (TrainingPlan) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(plan.name) }
    var description by remember { mutableStateOf(plan.description ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Plan Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateBack) {
                Text("Cancel")
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            label = { Text("Plan Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = name.isBlank() && errorMessage != null,
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = MaterialTheme.shapes.medium
        )

        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage = "Plan name is required"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    try {
                        repository.updateTrainingPlan(
                            id = plan.id,
                            name = name.trim(),
                            description = description.trim().takeIf { it.isNotEmpty() }
                        )
                        val updatedPlan = plan.copy(
                            name = name.trim(),
                            description = description.trim().takeIf { it.isNotEmpty() }
                        )
                        onPlanUpdated(updatedPlan)
                    } catch (e: Exception) {
                        errorMessage = "Failed to update plan: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && name.isNotBlank(),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Updating...")
                }
            } else {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun EndPlanForm(
    plan: TrainingPlan,
    repository: GymRepository,
    onPlanEnded: (TrainingPlan) -> Unit,
    onNavigateBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "End Training Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateBack) {
                Text("Cancel")
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Are you sure you want to end this training plan?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will mark the plan as completed with today's date. You can still view your workout history, but you won't be able to add new workouts to this plan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            val endDate = Clock.System.now().toEpochMilliseconds()
                            repository.updateTrainingPlanEndDate(plan.id, endDate)
                            val updatedPlan = plan.copy(end_date = endDate)
                            onPlanEnded(updatedPlan)
                        } catch (e: Exception) {
                            // Handle error
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("End Plan")
                }
            }
        }
    }
}

@Composable
private fun DeletePlanForm(
    plan: TrainingPlan,
    repository: GymRepository,
    onPlanDeleted: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Delete Training Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            TextButton(onClick = onNavigateBack) {
                Text("Cancel")
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "⚠️ This action cannot be undone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Deleting this training plan will permanently remove:\n• The training plan\n• All associated workout sessions\n• All workout data and progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Delete Training Plan",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("Confirm Deletion")
            },
            text = {
                Text("Are you absolutely sure you want to delete \"${plan.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                repository.deleteTrainingPlan(plan.id)
                                onPlanDeleted()
                            } catch (e: Exception) {
                                // Handle error - you might want to show an error message
                            } finally {
                                isLoading = false
                                showConfirmDialog = false
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDate(instant: Instant): String {
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = localDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "${month.substring(0, 3)} ${localDate.dayOfMonth}, ${localDate.year}"
}