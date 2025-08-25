package com.example.gymlogger.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import database.MuscleGroup
import database.TrainingPlan
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewWorkout(
    planId: Long,
    repository: GymRepository,
    onWorkoutCreated: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val animations = Animations()
    var workoutName by remember { mutableStateOf("") }
    var selectedMuscleGroups by remember { mutableStateOf(setOf<Long>()) }
    var trainingPlan by remember { mutableStateOf<TrainingPlan?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    val muscleGroups by repository.getAllMuscleGroups().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val now = Clock.System.now()

    // Load training plan details
    LaunchedEffect(planId) {
        trainingPlan = repository.getTrainingPlan(planId)
        // Generate default workout name
        workoutName = generateDefaultWorkoutName(now)
    }

    // Group muscle groups by category for better organization
    val groupedMuscleGroups = muscleGroups.groupBy { it.category }
    val primaryGroups = groupedMuscleGroups["primary"] ?: emptyList()
    val secondaryGroups = groupedMuscleGroups["secondary"] ?: emptyList()
    val specificGroups = groupedMuscleGroups["specific"] ?: emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Card
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
                    // Header with Back Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Start New Workout",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            trainingPlan?.let { plan ->
                                Text(
                                    text = "for ${plan.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = onNavigateBack) {
                            Text("Back")
                        }
                    }

                    // Workout Name Input
                    OutlinedTextField(
                        value = workoutName,
                        onValueChange = {
                            workoutName = it
                            errorMessage = null
                        },
                        label = { Text("Workout Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        singleLine = true,
                        isError = workoutName.isBlank() && errorMessage != null,
                        shape = MaterialTheme.shapes.medium,
                        placeholder = { Text("e.g., Push Day, Leg Day, etc.") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Muscle Groups Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Target Muscle Groups (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text( // TODO review text below the Workout! part goes to new line
                                text = "Select the muscle groups you plan to work. This helps suggest exercises later, or just go ahead and start the workout!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Primary Categories (Push, Pull, Legs, Full Body)
                    if (primaryGroups.isNotEmpty()) {
                        item {
                            CompactMuscleGroupSection(
                                title = "Workout Type",
                                muscleGroups = primaryGroups,
                                selectedGroups = selectedMuscleGroups,
                                onSelectionChange = { groupId, isSelected ->
                                    selectedMuscleGroups = if (isSelected) {
                                        selectedMuscleGroups + groupId
                                    } else {
                                        selectedMuscleGroups - groupId
                                    }
                                },
                                isHighlighted = true
                            )
                        }
                    }

                    // Collapsible Section for Secondary and Specific Muscle Groups
                    item {
                        Column {
                            // Tap to expand/collapse header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "More Muscle Groups",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (!isExpanded) {
                                        Text(
                                            text = "Tap to view more options",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Collapsible content
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = animations.fadeInMenu,
                                exit = animations.fadeOutMenu
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // Secondary Categories (Upper Body, Lower Body, Arms, Core)
                                    if (secondaryGroups.isNotEmpty()) {
                                        CompactMuscleGroupSection(
                                            title = "Body Regions",
                                            muscleGroups = secondaryGroups,
                                            selectedGroups = selectedMuscleGroups,
                                            onSelectionChange = { groupId, isSelected ->
                                                selectedMuscleGroups = if (isSelected) {
                                                    selectedMuscleGroups + groupId
                                                } else {
                                                    selectedMuscleGroups - groupId
                                                }
                                            }
                                        )
                                    }

                                    // Specific Muscle Groups
                                    if (specificGroups.isNotEmpty()) {
                                        CompactMuscleGroupSection(
                                            title = "Specific Muscles",
                                            muscleGroups = specificGroups,
                                            selectedGroups = selectedMuscleGroups,
                                            onSelectionChange = { groupId, isSelected ->
                                                selectedMuscleGroups = if (isSelected) {
                                                    selectedMuscleGroups + groupId
                                                } else {
                                                    selectedMuscleGroups - groupId
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Clear Selection Button
                    if (selectedMuscleGroups.isNotEmpty()) {
                        item {
                            TextButton(
                                onClick = { selectedMuscleGroups = emptySet() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear Selection (${selectedMuscleGroups.size} selected)")
                            }
                        }
                    }

                    // Add some bottom padding for the floating button
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
        }

        // Floating Start Workout Button
        Button(
            onClick = {
                if (workoutName.isBlank()) {
                    errorMessage = "Workout name is required"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    try {
                        val workoutSessionId = repository.insertWorkoutSession(
                            trainingPlanId = planId,
                            name = workoutName.trim(),
                            date = now.toEpochMilliseconds(),
                            notes = if (selectedMuscleGroups.isNotEmpty()) {
                                "Targeting: " + muscleGroups
                                    .filter { it.id in selectedMuscleGroups }
                                    .joinToString(", ") { it.name }
                            } else null
                        )

                        // Link selected muscle groups to the workout session
                        selectedMuscleGroups.forEach { muscleGroupId ->
                            repository.linkWorkoutSessionToMuscleGroup(
                                workoutSessionId,
                                muscleGroupId
                            )
                        }

                        onWorkoutCreated(workoutSessionId)
                    } catch (e: Exception) {
                        errorMessage = "Failed to create workout: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 24.dp)
                .height(56.dp)
                .align(Alignment.BottomCenter),
            enabled = !isLoading && workoutName.isNotBlank(),
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
                    Text("Creating Workout...")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Workout",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactMuscleGroupSection( // TODO select boxes purple?
    title: String,
    muscleGroups: List<MuscleGroup>,
    selectedGroups: Set<Long>,
    onSelectionChange: (Long, Boolean) -> Unit,
    isHighlighted: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            muscleGroups.forEach { muscleGroup ->
                MuscleGroupChip(
                    muscleGroup = muscleGroup,
                    isSelected = muscleGroup.id in selectedGroups,
                    onClick = { isSelected ->
                        onSelectionChange(muscleGroup.id, isSelected)
                    },
                    isHighlighted = isHighlighted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleGroupChip(
    muscleGroup: MuscleGroup,
    isSelected: Boolean,
    onClick: (Boolean) -> Unit,
    isHighlighted: Boolean = false
) {
    FilterChip(
        selected = isSelected,
        onClick = { onClick(!isSelected) },
        label = {
            Text(
                text = muscleGroup.name,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isHighlighted && isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            labelColor = if (isHighlighted && isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    )
}

private fun generateDefaultWorkoutName(now: Instant): String {
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayOfWeek = localDateTime.dayOfWeek.name.lowercase()
        .replaceFirstChar { it.uppercase() }
    return "$dayOfWeek Workout"
}