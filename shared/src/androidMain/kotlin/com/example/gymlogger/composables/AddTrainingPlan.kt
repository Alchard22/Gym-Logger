package com.example.gymlogger.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymlogger.repository.GymRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class PlanType {
    SELF_DIRECTED,
    PROGRAMMED,
    PRE_BUILT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainingPlanScreen(
    repository: GymRepository,
    onPlanAdded: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedPlanType by remember { mutableStateOf<PlanType?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dropdown Card Section
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
                            if (selectedPlanType == null) {
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
                            text = "Add Training Plan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!isExpanded && selectedPlanType == null) {
                            Text(
                                text = "Tap to see options",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Expanded Options
                AnimatedVisibility(
                    visible = isExpanded && selectedPlanType == null,
                    enter = fadeIn(spring(Spring.DampingRatioLowBouncy)) +
                            expandVertically(spring(Spring.DampingRatioLowBouncy)),
                    exit = fadeOut(spring(Spring.DampingRatioLowBouncy)) +
                            shrinkVertically(spring(Spring.DampingRatioLowBouncy))
                ){
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlanTypeCard(
                            title = "Self-Directed",
                            description = "Create workouts as you go. Perfect for experienced lifters.",
                            onClick = { selectedPlanType = PlanType.SELF_DIRECTED }
                        )

                        PlanTypeCard(
                            title = "Programmed",
                            description = "Design your own workout templates and schedule.",
                            onClick = { selectedPlanType = PlanType.PROGRAMMED }
                        )

                        PlanTypeCard(
                            title = "Pre-Built Programs",
                            description = "Choose from proven routines like Push/Pull/Legs.",
                            onClick = { selectedPlanType = PlanType.PRE_BUILT }
                        )
                    }
                }
            }
        }

        // Form Sections
        AnimatedVisibility(
            visible = selectedPlanType != null,
            enter = fadeIn(spring(Spring.DampingRatioNoBouncy)) +
                    expandVertically(spring(Spring.DampingRatioNoBouncy))
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
                when (selectedPlanType) {
                    PlanType.SELF_DIRECTED -> SelfDirectedPlanForm(
                        repository = repository,
                        onPlanAdded = onPlanAdded,
                        onNavigateBack = {
                            selectedPlanType = null
                        }
                    )

                    PlanType.PROGRAMMED -> ProgrammedPlanForm(
                        repository = repository,
                        onPlanAdded = onPlanAdded,
                        onNavigateBack = {
                            selectedPlanType = null
                        }
                    )

                    PlanType.PRE_BUILT -> PreBuiltProgramForm(
                        repository = repository,
                        onPlanAdded = onPlanAdded,
                        onNavigateBack = {
                            selectedPlanType = null
                        }
                    )

                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun PlanTypeCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelfDirectedPlanForm(
    repository: GymRepository,
    onPlanAdded: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val now = Clock.System.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Self-Directed Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Plan Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Start Date: Today (${formatDate(now)})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• End Date: Open (set later if needed)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Type: Flexible workout logging",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage = "Plan name is required"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    try {
                        val planId = repository.insertTrainingPlan(
                            name = name.trim(),
                            description = description.trim().takeIf { it.isNotEmpty() },
                            startDate = now.toEpochMilliseconds(),
                            endDate = null
                        )
                        onPlanAdded(planId)
                    } catch (e: Exception) {
                        errorMessage = "Failed to create plan: ${e.message}"
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
                    Text("Creating Plan...")
                }
            } else {
                Text(
                    text = "Create Self-Directed Plan",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ProgrammedPlanForm(
    repository: GymRepository,
    onPlanAdded: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Programmed Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateBack) {
                Text("Back")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Create custom workout templates and schedules. This feature is under development.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PreBuiltProgramForm(
    repository: GymRepository,
    onPlanAdded: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pre-Built Programs",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateBack) {
                Text("Back")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Choose from proven programs like Push/Pull/Legs, 5/3/1, Starting Strength, and more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun formatDate(instant: Instant): String {
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = localDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "${month.substring(0, 3)} ${localDate.dayOfMonth}, ${localDate.year}"
}