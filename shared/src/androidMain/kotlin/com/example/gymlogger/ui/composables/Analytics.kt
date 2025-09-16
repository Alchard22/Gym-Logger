package com.example.gymlogger.ui.composables

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymlogger.repository.GymRepository
import database.Exercise
import database.MuscleGroup
import database.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.collections.emptyList
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import database.WorkoutSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.math.*

enum class GraphType(val displayName: String) {
    LINE("Line Chart"),
    BAR("Bar Chart"),
    PIE("Pie Chart")
}

data class LineGraphPoint(
    val x: Float,
    val y: Float,
    val label: String,
    val value: String
)

data class LineGraphSeries(
    val name: String,
    val points: List<LineGraphPoint>,
    val color: Color
)

data class ExerciseProgress(
    val exercise: Exercise,
    val sessions: List<ExerciseSession>
)

data class ExerciseSession(
    val date: Long,
    val maxWeight: Double,
    val totalVolume: Double,
    val totalSets: Int,
    val totalReps: Long,
    val averageReps: Double
)

data class MuscleGroupAnalytics(
    val muscleGroup: MuscleGroup,
    val totalSessions: Int,
    val totalSets: Int,
    val lastWorked: Long?,
    val averageSessionsPerWeek: Double,
    val totalVolume: Double // Sets * freq
)

data class WorkoutAnalytics(
    val totalWorkouts: Int,
    val totalSets: Int,
    val streakDays: Int,
    val weeklyFrequency: Double
)

enum class AnalyticsTimeframe(val displayName: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    QUARTER("3 Months", 90),
    YEAR("1 Year", 365),
    ALL_TIME("All Time", Int.MAX_VALUE)
}

enum class AnalyticsMode(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    GENERAL("General", "Overall fitness statistics", Icons.Default.Assessment),
    EXERCISE("Exercise", "Specific exercise progress", Icons.Default.FitnessCenter),
    MUSCLE_GROUP("Muscle Group", "Muscle group analysis", Icons.Default.Accessibility)
}

@Composable
fun Analytics(
    repository: GymRepository,
    workoutSessionId: Long? = null,
    selectedExercises: List<Long> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.MONTH) }
    var showOverview by remember { mutableStateOf(true) }
    var showExerciseProgress by remember { mutableStateOf(true) }
    var showMuscleGroupAnalysis by remember { mutableStateOf(true) }

    val allExercises by repository.getAllExercises().collectAsState(initial = emptyList())
    val allMuscleGroups by repository.getAllMuscleGroups().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var workoutAnalytics by remember { mutableStateOf<WorkoutAnalytics?>(null) }
    var exerciseProgressList by remember { mutableStateOf<List<ExerciseProgress>>(emptyList()) }
    var muscleGroupAnalytics by remember { mutableStateOf<List<MuscleGroupAnalytics>>(emptyList()) }

    LaunchedEffect(selectedTimeframe, workoutSessionId, selectedExercises) {
        scope.launch {
            val recentWorkouts = repository.getRecentWorkouts(selectedTimeframe.days)
            workoutAnalytics = calculateWorkoutAnalytics(recentWorkouts)

            // Calculate exercise progress
            val exercisesToAnalyze = if (selectedExercises.isNotEmpty()) {
                selectedExercises
            } else if (workoutSessionId != null) {
                // Get exercises from specific workout session
                repository.getWorkoutSetsForSessionOnce(workoutSessionId)
                    .map { it.exercise_id }
                    .distinct()
            } else {
                // Get top 5 most used exercises
                allExercises.take(5).map { it.id }
            }

            exerciseProgressList = calculateExerciseProgress(
                repository,
                exercisesToAnalyze,
                selectedTimeframe.days
            )

            muscleGroupAnalytics = calculateMuscleGroupAnalytics(
                repository,
                allMuscleGroups,
                selectedTimeframe.days
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsHeader(
                selectedTimeframe = selectedTimeframe,
                onTimeframeChanged = { selectedTimeframe = it },
                workoutSessionId = workoutSessionId
            )
        }

        item {
            ExpandableAnalyticsCard(
                title = "Workout Overview",
                icon = Icons.Default.Analytics,
                isExpanded = showOverview,
                onExpandedChange = { showOverview = it }
            ) {
                workoutAnalytics?.let { analytics ->
                    WorkoutOverviewContent(analytics = analytics)
                }
            }
        }
        item {
            ExpandableAnalyticsCard(
                title = "Exercise Progress",
                icon = Icons.Default.FitnessCenter,
                isExpanded = showExerciseProgress,
                onExpandedChange = { showExerciseProgress = it }
            ) {
                ExerciseProgressContent(
                    progressList = exerciseProgressList,
                    timeframe = selectedTimeframe
                )
            }
        }
        item {
            AnalyticsGraphPicker {
                val colorScheme = MaterialTheme.colorScheme
                val lineGraphSeries = remember(exerciseProgressList, colorScheme) {
                    createExerciseProgressLineGraph(exerciseProgressList, colorScheme)
                }

                ModernLineGraph(
                    series = lineGraphSeries,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        item {
            ExpandableAnalyticsCard(
                title = "Muscle Group Analysis",
                icon = Icons.Default.BarChart,
                isExpanded = showMuscleGroupAnalysis,
                onExpandedChange = { showMuscleGroupAnalysis = it }
            ) {
                MuscleGroupAnalysisContent(
                    analytics = muscleGroupAnalytics,
                    timeframe = selectedTimeframe
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsHeader(
    selectedTimeframe: AnalyticsTimeframe,
    onTimeframeChanged: (AnalyticsTimeframe) -> Unit,
    workoutSessionId: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (workoutSessionId != null) "Workout Analytics" else "Fitness Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = if (workoutSessionId != null)
                    "Performance insights for this workout"
                else
                    "Track your progress and performance over time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            // Timeframe selector
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsTimeframe.values().forEach { timeframe ->
                    FilterChip(
                        selected = selectedTimeframe == timeframe,
                        onClick = { onTimeframeChanged(timeframe) },
                        label = { Text(timeframe.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableAnalyticsCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    content()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkoutOverviewContent(analytics: WorkoutAnalytics) { // TODO might need modes
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MetricCard(
                title = "Total Workouts",
                value = analytics.totalWorkouts.toString(),
                icon = Icons.Default.FitnessCenter,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            MetricCard(
                title = "Total Sets",
                value = analytics.totalSets.toString(),
                icon = Icons.Default.BarChart,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            MetricCard(
                title = "Weekly Frequency",
                value = String.format("%.1f", analytics.weeklyFrequency),
                icon = Icons.Default.DateRange,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        item {
            MetricCard(
                title = "Current Streak",
                value = "${analytics.streakDays} days",
                icon = if (analytics.streakDays > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                color = if (analytics.streakDays > 0) Color(0xFF4CAF50) else Color(0xFFf44336)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ExerciseProgressContent( // TODO Might need mode
    progressList: List<ExerciseProgress>,
    timeframe: AnalyticsTimeframe
) {
    if (progressList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No exercise data available for this timeframe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        progressList.forEach { progress ->
            ExerciseProgressCard(
                exerciseProgress = progress,
                timeframe = timeframe
            )
        }
    }
}

@Composable
private fun ExerciseProgressCard(
    exerciseProgress: ExerciseProgress,
    timeframe: AnalyticsTimeframe
) {
    var showDetails by remember { mutableStateOf(false) }

    val latestSession = exerciseProgress.sessions.maxByOrNull { it.date }
    val earliestSession = exerciseProgress.sessions.minByOrNull { it.date }

    val weightProgress = if (latestSession != null && earliestSession != null && earliestSession.maxWeight > 0) {
        val change = latestSession.maxWeight - earliestSession.maxWeight
        val percentage = (change / earliestSession.maxWeight) * 100
        Pair(change, percentage)
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseProgress.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Icon(
                    imageVector = if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (showDetails) "Hide details" else "Show details",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${exerciseProgress.sessions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = latestSession?.maxWeight?.let { "${it}kg" } ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Max Weight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) { // TODO Colour not using material theme
                    weightProgress?.let { (change, percentage) ->
                        Text(
                            text = "${if (change > 0) "+" else ""}${change.roundToInt()}kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (change > 0) Color(0xFF4CAF50) else if (change < 0) Color(0xFFf44336) else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${if (percentage > 0) "+" else ""}${percentage.roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (change > 0) Color(0xFF4CAF50) else if (change < 0) Color(0xFFf44336) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    } ?: run {
                        Text(
                            text = "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showDetails) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Simple progress visualization
                    SimpleProgressChart(exerciseProgress.sessions)
                    Text(
                        text = "Recent Sessions:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    exerciseProgress.sessions.takeLast(3).forEach { session ->
                        SessionSummaryRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleProgressChart(sessions: List<ExerciseSession>) {
    val sortedSessions = sessions.sortedBy { it.date }

    if (sortedSessions.size < 2) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxWeight = sortedSessions.maxOfOrNull { it.maxWeight } ?: 1.0

                sortedSessions.takeLast(8).forEach { session ->
                    val heightRatio = if (maxWeight > 0) session.maxWeight / maxWeight else 0.0
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height((30 * heightRatio).dp.coerceAtLeast(2.dp))
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryRow(session: ExerciseSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = session.date.toString(), // TODO review
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )

        Text(
            text = "${session.maxWeight}kg × ${session.totalSets} sets",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Text(
            text = "Vol: ${session.totalVolume.roundToInt()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MuscleGroupAnalysisContent( // TODO might need mode
    analytics: List<MuscleGroupAnalytics>,
    timeframe: AnalyticsTimeframe
) {
    if (analytics.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No muscle group data available for this timeframe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sort by frequency (most worked first)
        val sortedAnalytics = analytics.sortedByDescending { it.totalSessions }

        sortedAnalytics.forEach { muscleGroupAnalytic ->
            MuscleGroupAnalyticsCard(
                analytics = muscleGroupAnalytic,
                maxSessions = sortedAnalytics.first().totalSessions
            )
        }
    }
}

@Composable
private fun MuscleGroupAnalyticsCard(
    analytics: MuscleGroupAnalytics,
    maxSessions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = analytics.muscleGroup.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${analytics.totalSessions} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar
            val progressRatio = if (maxSessions > 0) analytics.totalSessions.toFloat() / maxSessions else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressRatio)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${analytics.totalSets} total sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                analytics.lastWorked?.let { lastWorked ->
                    Text(
                        text = "Last: ${lastWorked}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Text(
                    text = "Never worked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun calculateWorkoutAnalytics(workouts: List<WorkoutSession>): WorkoutAnalytics {
    val totalWorkouts = workouts.size
    val totalSets = 0 // TODO: Calculate from repository
    val streakDays = calculateCurrentStreak(workouts)
    val weeklyFrequency = if (workouts.isNotEmpty()) {
        val timeSpan = (System.currentTimeMillis() - workouts.minOf { it.date }) / (1000 * 60 * 60 * 24 * 7.0)
        if (timeSpan > 0) totalWorkouts / timeSpan else 0.0
    } else 0.0

    return WorkoutAnalytics(
        totalWorkouts = totalWorkouts,
        totalSets = totalSets,
        streakDays = streakDays,
        weeklyFrequency = weeklyFrequency
    )
}

private suspend fun calculateExerciseProgress(
    repository: GymRepository,
    exerciseIds: List<Long>,
    days: Int
): List<ExerciseProgress> {
    val cutoffDate = Clock.System.now().toEpochMilliseconds() - (days * 24 * 60 * 60 * 1000L)
    val exercises = repository.getExerciseByIds(exerciseIds)

    return exercises.map { exercise ->
        val allSessions = repository.getAllWorkoutSessions().first()

        val exerciseSessions = mutableListOf<ExerciseSession>()

        allSessions.forEach { session ->
            if (session.date >= cutoffDate || days == Int.MAX_VALUE) {
                val sets = repository.getWorkoutSetsForSessionOnce(session.id)
                    .filter { it.exercise_id == exercise.id }

                if (sets.isNotEmpty()) {
                    val maxWeight = sets.maxOfOrNull { it.weight } ?: 0.0
                    val totalVolume = sets.sumOf { it.weight * it.reps }
                    val totalSets = sets.size
                    val totalReps = sets.sumOf { it.reps }
                    val averageReps = if (totalSets > 0) totalReps.toDouble() / totalSets else 0.0

                    exerciseSessions.add(
                        ExerciseSession(
                            date = session.date,
                            maxWeight = maxWeight,
                            totalVolume = totalVolume,
                            totalSets = totalSets,
                            totalReps = totalReps,
                            averageReps = averageReps
                        )
                    )
                }
            }
        }

        ExerciseProgress(
            exercise = exercise,
            sessions = exerciseSessions.sortedBy { it.date }
        )
    }.filter { it.sessions.isNotEmpty() }
}

private suspend fun calculateMuscleGroupAnalytics(
    repository: GymRepository,
    muscleGroups: List<MuscleGroup>,
    days: Int
): List<MuscleGroupAnalytics> {
    val cutoffDate = if (days == Int.MAX_VALUE) 0L else
        Clock.System.now().toEpochMilliseconds() - (days * 24 * 60 * 60 * 1000L)

    val allSessions = repository.getAllWorkoutSessions().first()
    val filteredSessions = if (days == Int.MAX_VALUE) allSessions else
        allSessions.filter { it.date >= cutoffDate }

    return muscleGroups.map { muscleGroup ->
        var totalSessions = 0
        var totalSets = 0
        var lastWorked: Long? = null

        // Get all exercises for this muscle group
        val exercisesWithMuscleGroups = repository.getAllExercisesWithMuscleGroups().first()
        val muscleGroupExerciseIds = exercisesWithMuscleGroups
            .filter { it.muscle_group_id == muscleGroup.id }
            .map { it.id } // todo double check

        filteredSessions.forEach { session ->
            val sessionSets = repository.getWorkoutSetsForSessionOnce(session.id)
            val muscleGroupSetsInSession = sessionSets.filter { set ->
                muscleGroupExerciseIds.contains(set.exercise_id)
            }

            if (muscleGroupSetsInSession.isNotEmpty()) {
                totalSessions++
                totalSets += muscleGroupSetsInSession.size

                if (lastWorked == null || session.date > lastWorked!!) {
                    lastWorked = session.date
                }
            }
        }

        val averageFrequency = if (days != Int.MAX_VALUE && days > 0) {
            val weeks = days / 7.0
            totalSessions / weeks
        } else {
            // For all time, calculate based on time since first workout
            if (filteredSessions.isNotEmpty() && totalSessions > 0) {
                val timeSpanMs = System.currentTimeMillis() - filteredSessions.minOf { it.date }
                val weeks = timeSpanMs / (7 * 24 * 60 * 60 * 1000.0)
                if (weeks > 0) totalSessions / weeks else 0.0
            } else 0.0
        }

        MuscleGroupAnalytics(
            muscleGroup = muscleGroup,
            totalSessions = totalSessions,
            totalSets = totalSets,
            lastWorked = lastWorked,
            averageSessionsPerWeek = averageFrequency,
            totalVolume = (averageFrequency * totalSets)
        )
    }
}

private fun calculateCurrentStreak(workouts: List<WorkoutSession>): Int {
    if (workouts.isEmpty()) return 0

    val sortedWorkouts = workouts.sortedByDescending { it.date }
    val today = Clock.System.now().toEpochMilliseconds()
    val oneDayMs = 24 * 60 * 60 * 1000L

    var streak = 0
    var checkDate = today

    for (workout in sortedWorkouts) {
        val daysDiff = (checkDate - workout.date) / oneDayMs
        if (daysDiff <= 1) {
            streak++
            checkDate = workout.date - oneDayMs
        } else {
            break
        }
    }

    return streak
}

@Composable
fun AnalyticsGraphPicker(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var selectedGraphType by remember { mutableStateOf(GraphType.LINE) }

    Card(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Graph type picker header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress Charts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Future: Add graph type selector here
                // For now, just showing line graph
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Line Chart",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            content()
        }
    }
}


@Composable
fun ModernLineGraph(
    series: List<LineGraphSeries>,
    modifier: Modifier = Modifier,
    minHeight: Dp = 250.dp,
    showGrid: Boolean = true,
    showLegend: Boolean = true,
    animationDuration: Int = 1000
) {
    val colorScheme = MaterialTheme.colorScheme

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "graph_animation"
    )

    if (series.isEmpty() || series.all { it.points.isEmpty() }) {
        EmptyGraphState(modifier = modifier.heightIn(min = minHeight))
        return
    }

    // Calculate bounds
    val allPoints = series.flatMap { it.points }
    val minX = allPoints.minOfOrNull { it.x } ?: 0f
    val maxX = allPoints.maxOfOrNull { it.x } ?: 1f
    val minY = allPoints.minOfOrNull { it.y } ?: 0f
    val maxY = allPoints.maxOfOrNull { it.y } ?: 1f

    val yRange = maxY - minY
    val paddedMinY = (minY - yRange * 0.1f).let { if (it < 0 && minY >= 0) 0f else it }
    val paddedMaxY = maxY + yRange * 0.1f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (showLegend && series.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),

            ) {
                items(series) { seriesData ->
                    LegendItem(
                        name = seriesData.name,
                        color = seriesData.color
                    )
                }
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(vertical = 12.dp)
                .offset(y = 35.dp)
        ) {
            val paddingHeight = 100.dp.toPx()
            val paddingWidth = 34.dp.toPx()
            val offSetPadding = 40.dp.toPx()
            val graphWidth = size.width - (paddingWidth * 2)
            val graphHeight = size.height - (paddingHeight * 2)

            if (showGrid) {
                drawGrid(
                    width = graphWidth,
                    height = graphHeight,
                    offsetX = offSetPadding,
                    offsetY = offSetPadding,
                    color = colorScheme.outline.copy(alpha = 0.2f)
                )
            }

            drawAxes(
                width = graphWidth,
                height = graphHeight,
                offsetX = offSetPadding,
                offsetY = offSetPadding,
                color = colorScheme.outline.copy(alpha = 0.5f)
            )

            drawYAxisLabels(
                minY = paddedMinY,
                maxY = paddedMaxY,
                height = graphHeight,
                offsetX = offSetPadding,
                offsetY = offSetPadding,
                textColor = colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (allPoints.isNotEmpty()) {
                drawXAxisLabels(
                    minX = minX,
                    maxX = maxX,
                    width = graphWidth,
                    offsetX = offSetPadding,
                    offsetY = offSetPadding,
                    textColor = colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            series.forEach { seriesData ->
                if (seriesData.points.size >= 2) {
                    drawAnimatedLine(
                        points = seriesData.points,
                        minX = minX,
                        maxX = maxX,
                        minY = paddedMinY,
                        maxY = paddedMaxY,
                        width = graphWidth,
                        height = graphHeight,
                        offsetX = offSetPadding,
                        offsetY = offSetPadding,
                        color = seriesData.color,
                        progress = animatedProgress
                    )

                    drawAnimatedPoints(
                        points = seriesData.points,
                        minX = minX,
                        maxX = maxX,
                        minY = paddedMinY,
                        maxY = paddedMaxY,
                        width = graphWidth,
                        height = graphHeight,
                        offsetX = offSetPadding,
                        offsetY = offSetPadding,
                        color = seriesData.color,
                        backgroundColor = colorScheme.surface,
                        progress = animatedProgress
                    )
                }
                drawAnimatedPoints(
                    points = seriesData.points,
                    minX = minX,
                    maxX = maxX,
                    minY = paddedMinY,
                    maxY = paddedMaxY,
                    width = graphWidth,
                    height = graphHeight,
                    offsetX = offSetPadding,
                    offsetY = offSetPadding,
                    color = seriesData.color,
                    backgroundColor = colorScheme.surface,
                    progress = animatedProgress
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    name: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun EmptyGraphState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun DrawScope.drawGrid(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color
) {
    val gridLines = 5

    for (i in 0..gridLines) {
        val y = offsetY + (height * i / gridLines)
        drawLine(
            color = color,
            start = Offset(offsetX, y),
            end = Offset(offsetX + width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Vertical grid lines
    for (i in 0..gridLines) {
        val x = offsetX + (width * i / gridLines)
        drawLine(
            color = color,
            start = Offset(x, offsetY),
            end = Offset(x, offsetY + height),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawAxes(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color
) {
    val strokeWidth = 2.dp.toPx()

    drawLine(
        color = color,
        start = Offset(offsetX, offsetY),
        end = Offset(offsetX, offsetY + height),
        strokeWidth = strokeWidth
    )

    drawLine(
        color = color,
        start = Offset(offsetX, offsetY + height),
        end = Offset(offsetX + width, offsetY + height),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawYAxisLabels(
    minY: Float,
    maxY: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    textColor: Color
) {
    val labelCount = 6
    val textPaint = android.graphics.Paint().apply {
        this.color = textColor.toArgb()
        textSize = 12.sp.toPx()
        isAntiAlias = true
    }

    for (i in 0..labelCount) {
        val value = minY + (maxY - minY) * i / labelCount
        val y = offsetY + (height * i / labelCount)

        // Round to nearest 0.5
        val roundedValue = (value * 2).roundToInt() / 2.0
        val label = if (roundedValue == roundedValue.toInt().toDouble()) {
            roundedValue.toInt().toString()
        } else {
            "%.1f".format(roundedValue)
        }

        drawContext.canvas.nativeCanvas.drawText(
            label + "kg", // TODO Add weight type
            offsetX - 25.dp.toPx(),
            y + 4.dp.toPx(),
            textPaint
        )
    }
}

private fun DrawScope.drawXAxisLabels(
    minX: Float,
    maxX: Float,
    width: Float,
    offsetX: Float,
    offsetY: Float,
    textColor: Color
) {
    val textPaint = android.graphics.Paint().apply {
        this.color = textColor.toArgb()
        textSize = 11.sp.toPx()
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    // Create evenly spaced date labels across the time range
    val labelCount = 5 // Show 5 date labels across the graph

    for (i in 0..labelCount) {
        val timestamp = minX + (maxX - minX) * i / labelCount
        val x = offsetX + (width * i / labelCount)

        val dateLabel = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp.toLong()))

        drawContext.canvas.nativeCanvas.drawText(
            dateLabel,
            x,
            offsetY + 20.dp.toPx(),
            textPaint
        )
    }
}

private fun DrawScope.drawAnimatedLine(
    points: List<LineGraphPoint>,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    progress: Float
) {
    val path = Path()
    val totalPoints = points.size
    val animatedPointCount = (totalPoints * progress).toInt().coerceAtLeast(2)
    val animatedPoints = points.take(animatedPointCount)

    animatedPoints.forEachIndexed { index, point ->
        val x = offsetX + ((point.x - minX) / (maxX - minX)) * width
        val y = offsetY + ((point.y - minY) / (maxY - minY)) * height

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawAnimatedPoints(
    points: List<LineGraphPoint>,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    backgroundColor: Color,
    progress: Float
) {
    val totalPoints = points.size
    val animatedPointCount = (totalPoints * progress).toInt()
    val animatedPoints = points.take(animatedPointCount)

    animatedPoints.forEach { point ->
        val x = offsetX + ((point.x - minX) / (maxX - minX)) * width
        val y = offsetY + ((point.y - minY) / (maxY - minY)) * height

        // Draw point background
        drawCircle(
            color = backgroundColor,
            radius = 6.dp.toPx(),
            center = Offset(x, y)
        )

        // Draw point
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

fun createExerciseProgressLineGraph(
    exerciseProgressList: List<ExerciseProgress>,
    colorScheme: ColorScheme
): List<LineGraphSeries> {
    val colors = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.primaryContainer,
        colorScheme.secondaryContainer
    )

    return exerciseProgressList.mapIndexed { index, progress ->
        val points = progress.sessions.sortedBy { it.date }.mapIndexed { pointIndex, session ->
            LineGraphPoint(
                x = session.date.toFloat(), // Use actual timestamp for proper spacing
                y = session.maxWeight.toFloat(),
                label = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(session.date)),
                value = "${session.maxWeight}kg"
            )
        }

        LineGraphSeries(
            name = progress.exercise.name,
            points = points,
            color = colors[index % colors.size]
        )
    }
}