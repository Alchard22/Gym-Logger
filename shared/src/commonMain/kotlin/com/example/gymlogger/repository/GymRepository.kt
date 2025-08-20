package com.example.gymlogger.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import database.Exercise
import com.example.gymlogger.database.GymDatabase
import database.TrainingPlan
import database.WorkoutSession
import database.WorkoutSet
import database.MuscleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class GymRepository(private val database: GymDatabase) {

    // === MUSCLE GROUPS ===

    fun getAllMuscleGroups(): Flow<List<MuscleGroup>> {
        return database.gymDatabaseQueries.selectAllMuscleGroups()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun insertMuscleGroup(name: String, category: String) {
        withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertMuscleGroup(name, category)
        }
    }

    // === TRAINING PLANS ===

    fun getAllTrainingPlans(): Flow<List<TrainingPlan>> {
        return database.gymDatabaseQueries.selectAllTrainingPlans()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun insertTrainingPlan(
        name: String,
        description: String?,
        startDate: Long,
        endDate: Long?
    ): Long {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertTrainingPlan(name, description, startDate, endDate)
            database.gymDatabaseQueries.lastInsertRowId().executeAsOne()
        }
    }

    suspend fun getTrainingPlan(id: Long): TrainingPlan? {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.selectTrainingPlanById(id).executeAsOneOrNull()
        }
    }

    // === WORKOUT SESSIONS ===

    fun getWorkoutSessionsForPlan(planId: Long): Flow<List<WorkoutSession>> {
        return database.gymDatabaseQueries.selectWorkoutSessionsForPlan(planId)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> {
        return database.gymDatabaseQueries.selectAllWorkoutSessions()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun insertWorkoutSession(
        trainingPlanId: Long?,
        name: String,
        date: Long,
        notes: String?
    ): Long {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertWorkoutSession(trainingPlanId, name, date, notes)
            database.gymDatabaseQueries.lastInsertRowId().executeAsOne()
        }
    }

    suspend fun getWorkoutSession(id: Long): WorkoutSession? {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.selectWorkoutSessionById(id).executeAsOneOrNull()
        }
    }

    // === EXERCISES ===

    fun getAllExercises(): Flow<List<Exercise>> {
        return database.gymDatabaseQueries.selectAllExercises()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun insertExercise(
        name: String,
        description: String?,
        videoLink: String?
    ): Long {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertExercise(name, description, videoLink)
            database.gymDatabaseQueries.lastInsertRowId().executeAsOne()
        }
    }

    suspend fun getExercise(id: Long): Exercise? {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.selectExerciseById(id).executeAsOneOrNull()
        }
    }

    suspend fun getExerciseWithMuscleGroups(id: Long): List<ExerciseWithMuscleGroup> {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries
                .selectExerciseWithMuscleGroups(id)
                .executeAsList()
                .map { row ->
                    ExerciseWithMuscleGroup(
                        exercise = Exercise(
                            id = row.id,
                            name = row.name,
                            aliases = row.aliases,
                            description = row.description,
                            video_link = row.video_link
                        ),
                        muscleGroupName = row.muscle_group_name,
                        involvementType = row.involvement_type
                    )
                }
        }
    }


    // === WORKOUT SETS ===

    suspend fun insertWorkoutSet(
        workoutSessionId: Long,
        exerciseId: Long,
        setNumber: Long,
        reps: Long,
        weight: Double,
        intensity: Long?,
        restSeconds: Long?
    ): Long {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertWorkoutSet(
                workoutSessionId, exerciseId, setNumber, reps, weight, intensity, restSeconds
            )
            database.gymDatabaseQueries.lastInsertRowId().executeAsOne()
        }
    }

    fun getWorkoutSetsForSession(sessionId: Long): Flow<List<WorkoutSet>> {
        return database.gymDatabaseQueries.selectWorkoutSetsForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun getWorkoutSet(id: Long): WorkoutSet? {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.selectWorkoutSetById(id).executeAsOneOrNull()
        }
    }

    suspend fun updateWorkoutSet(
        id: Long,
        reps: Long,
        weight: Double,
        intensity: Long?,
        restSeconds: Long?
    ) {
        withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.updateWorkoutSet(reps, weight, intensity, restSeconds, id)
        }
    }

    // === MUSCLE GROUP RELATIONSHIPS ===

    suspend fun linkExerciseToMuscleGroup(
        exerciseId: Long,
        muscleGroupId: Long,
        involvementType: String
    ) {
        withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertExerciseMuscleGroup(exerciseId, muscleGroupId, involvementType)
        }
    }

    suspend fun linkWorkoutSessionToMuscleGroup(
        workoutSessionId: Long,
        muscleGroupId: Long
    ) {
        withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.insertWorkoutSessionMuscleGroup(workoutSessionId, muscleGroupId)
        }
    }

    // === ANALYTICS / INSIGHTS ===

    // Get recent workouts (last 30 days)
    suspend fun getRecentWorkouts(dayCount: Int = 30): List<WorkoutSession> {
        return withContext(Dispatchers.Default) {
            val cutoffDate = Clock.System.now().toEpochMilliseconds() - (dayCount * 24 * 60 * 60 * 1000L)
            database.gymDatabaseQueries.selectRecentWorkouts(cutoffDate).executeAsList()
        }
    }

    // Get total sets for an exercise (for progress tracking)
    suspend fun getTotalSetsForExercise(exerciseId: Long): Long {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.countSetsForExercise(exerciseId).executeAsOne()
        }
    }

    // Get personal best for an exercise (max weight)
    suspend fun getPersonalBest(exerciseId: Long): WorkoutSet? {
        return withContext(Dispatchers.Default) {
            database.gymDatabaseQueries.selectPersonalBest(exerciseId).executeAsOneOrNull()
        }
    }
}

// Data class for exercise with muscle group info
data class ExerciseWithMuscleGroup(
    val exercise: Exercise,
    val muscleGroupName: String,
    val involvementType: String,
)