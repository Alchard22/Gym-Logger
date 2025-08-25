package com.example.gymlogger.database

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