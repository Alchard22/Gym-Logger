package com.example.gymlogger.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlogger.database.ExerciseGroupedWithMuscleGroups
import com.example.gymlogger.repository.ExerciseWithMuscleGroup
import com.example.gymlogger.repository.GymRepository
import com.example.gymlogger.ui.Animations
import database.Exercise
import database.MuscleGroup
import database.SelectAllExerciseWithMuscleGroups
import database.WorkoutSession
import kotlinx.coroutines.launch

data class SelectedExercise(
    val exercise: ExerciseGroupedWithMuscleGroups,
    val sets: MutableList<WorkoutSetData> = mutableListOf()
)

data class WorkoutSetData(
    var setNumber: Int,
    var reps: String = "",
    var weight: String = "",
    var intensity: String = "",
    var restSeconds: String = "60",
    var notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartWorkout(
    workoutSessionId: Long,
    repository: GymRepository,
    onWorkoutCompleted: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val animations = Animations()
    var workoutSession by remember { mutableStateOf<WorkoutSession?>(null) }
    var selectedExercises by remember { mutableStateOf<List<SelectedExercise>>(emptyList()) }
    var isExerciseSelectionExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAllExercises by remember { mutableStateOf(false) }

    val workoutSessions by repository.getWorkoutSessionMuscleGroup(workoutSessionId).collectAsState(initial = emptyList())
    val getAllExercisesWithMusclesGroups by repository.getAllExercisesWithMuscleGroups().collectAsState(initial = emptyList())
    val allMuscleGroups by repository.getAllMuscleGroups().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val selectedMuscleGroups: Set<Long> = workoutSessions.map { it!!.muscle_group_id }.toSet()

    // Load workout session details and favorite exercises
    LaunchedEffect(workoutSessionId) {
        workoutSession = repository.getWorkoutSession(workoutSessionId)
        // Load favorite exercises from database
    }

    // Group exercises by id to remove duplicates and combine muscle groups
    val groupedExercises = remember(getAllExercisesWithMusclesGroups) {
        getAllExercisesWithMusclesGroups.groupBy { it.id }.map { (_, exercises) ->
            val first = exercises.first()
            ExerciseGroupedWithMuscleGroups(
                id = first.id,
                name = first.name,
                aliases = first.aliases,
                description = first.description,
                video_link = first.video_link,
                favourite = (first.favourite ?: 0).toInt(),
                muscle_group_id = exercises.map { it.muscle_group_id },
                muscle_group_name = exercises.map { it.muscle_group_name },
                muscle_group_category = exercises.map { it.muscle_group_category },
                involvement_type = first.involvement_type
            )
        }
    }

    // Filter and sort exercises by muscle groups, then favorites, then alphabetically
    val filteredExercises = remember(groupedExercises, searchQuery, selectedMuscleGroups) {
        val filtered = if (searchQuery.isBlank()) {
            groupedExercises
        } else {
            groupedExercises.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.aliases?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        // Sort: muscle group match first, then favorites, then alphabetically
        filtered.sortedWith(
            compareByDescending<ExerciseGroupedWithMuscleGroups> { exercise ->
                // Muscle group match: if selectedMuscleGroups is empty, treat as "match all"
                if (selectedMuscleGroups.isEmpty()) false
                else exercise.muscle_group_id.any { it in selectedMuscleGroups }
            }.thenBy { exercise ->
                // Favorites first
                if (exercise.favourite == 1) 0 else 1
            }.thenBy { it.name }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = workoutSession?.name ?: "Workout Session",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "In Progress • ${selectedExercises.size} exercises",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(onClick = onNavigateBack) {
                            Text(
                                "Back",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(2.dp))

                if (selectedExercises.isNotEmpty()) {
                    Button(
                        modifier = Modifier.fillMaxWidth().height(30.dp).padding(0.dp),
                        shape = MaterialTheme.shapes.medium,
                        onClick = {
                            scope.launch {
                                // Save all workout sets
                                selectedExercises.forEach { selectedExercise ->
                                    selectedExercise.sets.forEach { setData ->
                                        if (setData.reps.isNotBlank() && setData.weight.isNotBlank()) {
                                            repository.insertWorkoutSet(
                                                workoutSessionId = workoutSessionId,
                                                exerciseId = selectedExercise.exercise.id,
                                                setNumber = setData.setNumber.toLong(),
                                                reps = setData.reps.toLongOrNull() ?: 0,
                                                weight = setData.weight.toDoubleOrNull() ?: 0.0,
                                                intensity = null,
                                                restSeconds = setData.restSeconds.toLongOrNull()
                                            )
                                        }
                                    }
                                }
                                onWorkoutCompleted()
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save and Exit Workout", modifier = Modifier.offset(y = (-2).dp))
                    }
                }
            }

            // Exercise Selection Card
            item {
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
                        // Header - Clickable to expand
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExerciseSelectionExpanded = !isExerciseSelectionExpanded }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Select Exercise",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (!isExerciseSelectionExpanded) {
                                        Text(
                                            text = "Tap to expand",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isExerciseSelectionExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Expanded content
                        AnimatedVisibility(
                            visible = isExerciseSelectionExpanded,
                            enter = animations.fadeInMenu,
                            exit = animations.fadeOutMenu
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Search Bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search exercises") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = if (searchQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear search"
                                                )
                                            }
                                        }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )

                                // Add Exercise Button
                                OutlinedButton(
                                    onClick = { showAddExerciseDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add New Exercise")
                                }

                                // Exercise List
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Show Less toggle
                                    if (filteredExercises.size > 6 && showAllExercises) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showAllExercises = !showAllExercises },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text =
                                                    "Show less exercises",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                            )
                                        }
                                    }

                                    val exercisesToShow = if (showAllExercises) filteredExercises else filteredExercises.take(6)

                                    exercisesToShow.forEach { exercise ->
                                        ExerciseListItem(
                                            exercise = exercise,
                                            isFavorite = exercise.favourite == 1,
                                            onFavoriteToggle = { isFavorite ->
                                                scope.launch {
                                                    if (isFavorite) {
                                                        repository.updateExercise(exercise.id, exercise.name, exercise.description!!, exercise.video_link, 1)
                                                    } else {
                                                        repository.updateExercise(exercise.id, exercise.name, exercise.description!!, exercise.video_link, 0)
                                                    }
                                                }
                                            },
                                            onSelect = {
                                                val newExercise = SelectedExercise(
                                                    exercise = exercise,
                                                    sets = mutableListOf(
                                                        WorkoutSetData(setNumber = 1)
                                                    )
                                                )
                                                selectedExercises = selectedExercises + newExercise
                                                isExerciseSelectionExpanded = false
                                                searchQuery = ""
                                            }
                                        )
                                    }

                                    // Show All / Show Less toggle
                                    if (filteredExercises.size > 6) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showAllExercises = !showAllExercises },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = if (showAllExercises) {
                                                    "Show less exercises"
                                                } else {
                                                    "Tap to show all ${filteredExercises.size} exercises"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Selected Exercises
            items(selectedExercises.indices.toList()) { index ->
                SelectedExerciseCard(
                    selectedExercise = selectedExercises[index],
                    onSetChange = { setIndex, updatedSet ->
                        selectedExercises = selectedExercises.toMutableList().apply {
                            this[index] = this[index].copy(
                                sets = this[index].sets.toMutableList().apply {
                                    this[setIndex] = updatedSet
                                }
                            )
                        }
                    },
                    onAddSet = {
                        selectedExercises = selectedExercises.toMutableList().apply {
                            this[index] = this[index].copy(
                                sets = (this[index].sets + WorkoutSetData(setNumber = this[index].sets.size + 1)).toMutableList()
                            )
                        }
                    },
                    onRemoveSet = { setIndex ->
                        selectedExercises = selectedExercises.toMutableList().apply {
                            val updatedSets = this[index].sets.filterIndexed { idx, _ -> idx != setIndex }
                                .mapIndexed { idx, set -> set.copy(setNumber = idx + 1) }
                                .toMutableList()

                            this[index] = this[index].copy(sets = updatedSets)
                        }
                    },
                    onRemoveExercise = {
                        selectedExercises = selectedExercises.filterIndexed { i, _ -> i != index }
                    }
                )
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Exercise Dialog (simplified version)
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            muscleGroups = allMuscleGroups,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseAdded = { exerciseName, selectedMuscleGroups ->
                scope.launch {
                    val exerciseId = repository.insertExercise(
                        name = exerciseName,
                        description = null,
                        videoLink = null
                    )

                    selectedMuscleGroups.forEach { muscleGroupId ->
                        repository.linkExerciseToMuscleGroup(
                            exerciseId = exerciseId,
                            muscleGroupId = muscleGroupId,
                            involvementType = "primary"
                        )
                    }
                }
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
private fun ExerciseListItem(
    exercise: ExerciseGroupedWithMuscleGroups,
    isFavorite: Boolean,
    onFavoriteToggle: (Boolean) -> Unit,
    onSelect: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column {
            // Main item row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand button on the left
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Hide details" else "Show details",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (exercise.muscle_group_name.isNotEmpty()) {
                        Text(
                            text = exercise.muscle_group_name.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = { onFavoriteToggle(!isFavorite) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            modifier = Modifier.size(16.dp),
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercise.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!exercise.video_link.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                // TODO Handle video link click
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Link to Video")
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedExerciseCard(
    selectedExercise: SelectedExercise,
    onSetChange: (Int, WorkoutSetData) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedExercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Info Button
                    TextButton(
                        onClick = { showInfoDialog = true }
                    ) {
                        Text(
                            "Info",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(
                        onClick = onRemoveExercise,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove exercise",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Sets
            selectedExercise.sets.forEachIndexed { setIndex, setData ->
                WorkoutSetRow(
                    setData = setData,
                    onSetChange = { updatedSet -> onSetChange(setIndex, updatedSet) },
                    onRemoveSet = if (selectedExercise.sets.size > 1) {
                        { onRemoveSet(setIndex) }
                    } else null
                )
            }

            // Add Set Button
            ElevatedButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Set")
            }
        }
    }

    // Exercise Info Dialog
    if (showInfoDialog) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedExercise.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showInfoDialog = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (selectedExercise.exercise.muscle_group_name.isNotEmpty()) {
                    Text(
                        text = "Muscle Groups: ${selectedExercise.exercise.muscle_group_name.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                selectedExercise.exercise.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!selectedExercise.exercise.video_link.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            // TODO Handle video link click
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Link to Video")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutSetRow(
    setData: WorkoutSetData,
    onSetChange: (WorkoutSetData) -> Unit,
    onRemoveSet: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Main row with reps and weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Set Number
                Text(
                    text = "${setData.setNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )

                // Reps Field
                OutlinedTextField(
                    value = setData.reps,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            onSetChange(setData.copy(reps = newValue))
                        }
                    },
                    label = { Text("Reps", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )

                // Weight Field
                OutlinedTextField(
                    value = setData.weight,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) {
                            onSetChange(setData.copy(weight = newValue))
                        }
                    },
                    label = { Text("Weight", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )

                // Expand/Collapse Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Hide extras" else "Show extras",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Remove Set Button
                onRemoveSet?.let { removeSet ->
                    IconButton(
                        onClick = removeSet,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove set",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Expandable extras section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Intensity Field
                        OutlinedTextField(
                            value = setData.intensity,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                    onSetChange(setData.copy(intensity = newValue))
                                }
                            },
                            label = { Text("Intensity", fontSize = 12.sp) },
                            placeholder = { Text("RPE 1-10", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraSmall
                        )

                        // Rest Field
                        OutlinedTextField(
                            value = setData.restSeconds,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                    onSetChange(setData.copy(restSeconds = newValue))
                                }
                            },
                            label = { Text("Rest (s), fontSize = 12.sp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                    }

                    // Notes Field
                    OutlinedTextField(
                        value = setData.notes,
                        onValueChange = { newNotes ->
                            onSetChange(setData.copy(notes = newNotes))
                        },
                        label = { Text("Notes", fontSize = 12.sp) },
                        placeholder = { Text("Form notes, feelings, etc.", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = MaterialTheme.shapes.extraSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseDialog(
    muscleGroups: List<MuscleGroup>,
    onDismiss: () -> Unit,
    onExerciseAdded: (String, Set<Long>) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedMuscleGroups by remember { mutableStateOf<Set<Long>>(emptySet()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add New Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Muscle Groups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                muscleGroups.forEach { muscleGroup ->
                    FilterChip(
                        selected = muscleGroup.id in selectedMuscleGroups,
                        onClick = {
                            selectedMuscleGroups = if (muscleGroup.id in selectedMuscleGroups) {
                                selectedMuscleGroups - muscleGroup.id
                            } else {
                                selectedMuscleGroups + muscleGroup.id
                            }
                        },
                        label = { Text(muscleGroup.name) },
                        leadingIcon = if (muscleGroup.id in selectedMuscleGroups) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (exerciseName.isNotBlank()) {
                            onExerciseAdded(exerciseName.trim(), selectedMuscleGroups)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = exerciseName.isNotBlank()
                ) {
                    Text("Add Exercise")
                }
            }
        }
    }
}