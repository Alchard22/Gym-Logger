package com.example.gymlogger.database

import com.example.gymlogger.repository.ExerciseGroupedWithMuscleGroupsDB

public data class ExerciseGroupedWithMuscleGroups(
    public val id: Long,
    public val name: String,
    public val aliases: String?,
    public val description: String?,
    public val video_link: String?,
    public val favourite: Int,
    public val muscle_group_id: List<Long>,
    public val muscle_group_name: List<String>,
    public val muscle_group_category: List<String?>,
    public val involvement_type: String,
)

fun ConvertToExerciseGroupedWithMuscleGroups(input: ExerciseGroupedWithMuscleGroupsDB): com.example.gymlogger.database.ExerciseGroupedWithMuscleGroups {
    return com.example.gymlogger.database.ExerciseGroupedWithMuscleGroups(
        id = input.id,
        name = input.name,
        aliases = input.aliases,
        description = input.description,
        video_link = input.video_link,
        favourite = input.favourite,
        muscle_group_id = input.muscle_group_id,
        muscle_group_name = input.muscle_group_name,
        muscle_group_category = input.muscle_group_category,
        involvement_type = input.involvement_type
    )
}