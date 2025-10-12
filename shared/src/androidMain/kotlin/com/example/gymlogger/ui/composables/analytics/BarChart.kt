package com.example.gymlogger.ui.composables.analytics

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ColorScheme
import com.example.gymlogger.AppSettings
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


@Composable
fun ModernBarChart( // TODO Broken see dates comment below.
    series: List<BarChartSeries>,
    modifier: Modifier = Modifier,
    minHeight: Dp = 250.dp,
    showGrid: Boolean = true,
    showLegend: Boolean = true,
    animationDuration: Int = 1000,
    barSpacing: Dp = 8.dp,
    groupSpacing: Dp = 16.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "bar_animation"
    )

    if (series.isEmpty() || series.all { it.bars.isEmpty() }) {
        EmptyGraphState(modifier = modifier.heightIn(min = minHeight))
        return
    }

    // Calculate bounds
    val allBars = series.flatMap { it.bars }
    val categories = allBars.map { it.category }.distinct() // the passed in string needs to be in the same format as the line graph as well as being in maxX minX. I think it is best to scrap this for now. Maybe move on to streak?
    val minY = allBars.minOfOrNull { it.value } ?: 0f
    val maxY = allBars.maxOfOrNull { it.value } ?: 1f
    val minX = allBars.minOfOrNull { it.value } ?: 0f
    val maxX = allBars.maxOfOrNull { it.value } ?: 1f // Need to calculate in line with line graph
    Log.i("Dates", "${minX.toString()}, ${maxX.toString()}, ${categories}")

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

            drawBarChartAxes(
                width = graphWidth,
                height = graphHeight,
                offsetX = offSetPadding,
                offsetY = offSetPadding,
                color = colorScheme.outline.copy(alpha = 0.5f)
            )

            drawYAxisLabelsBarChart(
                minY = paddedMinY,
                maxY = paddedMaxY,
                height = graphHeight,
                offsetX = offSetPadding,
                offsetY = offSetPadding,
                textColor = colorScheme.onSurface.copy(alpha = 0.7f),
            )

//            if (categories.isNotEmpty()) { // TODO Unsure of purpose
//                this.drawXAxisLabelsForCategories(
//                    categories = categories,
//                    width = graphWidth,
//                    offsetX = offSetPadding,
//                    offsetY = offSetPadding,
//                    textColor = colorScheme.onSurface.copy(alpha = 0.7f),
//                    textMeasurer = textMeasurer
//                )
//            } else {
            drawXAxisLabels( // TODO this is not passed categories, which is holding the date information. Ill need to see how the line graph handles this.
                minX = minX,
                maxX = maxX,
                width = graphWidth,
                offsetX = offSetPadding,
                offsetY = offSetPadding,
                textColor = colorScheme.onSurface.copy(alpha = 0.7f),
                labelCount = 3
            )
//            }

            // Draw bars
            DrawAnimatedBars(
                series = series,
                categories = categories,
                minY = paddedMinY,
                maxY = paddedMaxY,
                width = graphWidth,
                height = graphHeight,
                offsetX = offSetPadding,
                offsetY = offSetPadding,
                progress = animatedProgress,
                barSpacing = barSpacing.toPx(),
                groupSpacing = groupSpacing.toPx(),
                textMeasurer = textMeasurer
            )
        }
    }
}

data class BarChartSeries(
    val name: String,
    val bars: List<BarData>,
    val color: Color
)

data class BarData(
    val category: String,
    val value: Float,
    val label: String? = null
)

private fun DrawScope.DrawAnimatedBars(
    series: List<BarChartSeries>,
    categories: List<String>,
    minY: Float,
    maxY: Float,
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    progress: Float,
    barSpacing: Float,
    groupSpacing: Float,
    textMeasurer: TextMeasurer
) {
    if (categories.isEmpty() || series.isEmpty()) return

    val totalGroupSpacing = (categories.size - 1) * groupSpacing
    val availableWidth = width - totalGroupSpacing
    val categoryWidth = availableWidth / categories.size
    val totalBarsPerCategory = series.size
    val totalBarSpacingPerCategory = (totalBarsPerCategory - 1) * barSpacing
    val barWidth = (categoryWidth - totalBarSpacingPerCategory) / totalBarsPerCategory

    categories.forEachIndexed { categoryIndex, category ->
        val categoryStartX = offsetX + (categoryIndex * (categoryWidth + groupSpacing))

        series.forEachIndexed { seriesIndex, seriesData ->
            val bar = seriesData.bars.find { it.category == category }
            if (bar != null) {
                val barX = categoryStartX + (seriesIndex * (barWidth + barSpacing))

                // Calculate bar height based on value
                val normalizedValue = (bar.value - minY) / (maxY - minY)
                val barHeight = normalizedValue * height * progress
                val barY = offsetY + height - barHeight

                // Draw bar with rounded corners
                drawRoundRect(
                    color = seriesData.color,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                // Draw value label on top of bar if progress is complete
                if (progress > 0.9f && bar.label != null) {
                    drawValueLabel(
                        text = bar.label,
                        x = barX + barWidth / 2,
                        y = barY - 8.dp.toPx(),
                        textColor = Color.Black.copy(alpha = 0.7f),
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawXAxisLabelsForCategories( // TODO I don't see the purpose in cartegories, wouldn't it always be a date??
    categories: List<String>,
    width: Float,
    offsetX: Float,
    offsetY: Float,
    textColor: Color,
    textMeasurer: TextMeasurer
) {
    val totalGroupSpacing = (categories.size - 1) * 16.dp.toPx() // groupSpacing
    val availableWidth = width - totalGroupSpacing
    val categoryWidth = availableWidth / categories.size

    categories.forEachIndexed { index, category ->
        val x = offsetX + (index * (categoryWidth + 16.dp.toPx())) + categoryWidth / 2
        val y = offsetY + size.height - 60.dp.toPx()

        val textResult = textMeasurer.measure(
            text = category,
            style = TextStyle(
                fontSize = 12.sp,
                color = textColor
            )
        )

        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(
                x - textResult.size.width / 2,
                y - textResult.size.height / 2
            )
        )
    }
}

private fun DrawScope.drawValueLabel(
    text: String,
    x: Float,
    y: Float,
    textColor: Color,
    textMeasurer: TextMeasurer
) {
    val textResult = textMeasurer.measure(
        text = text,
        style = TextStyle(
            fontSize = 10.sp,
            color = textColor
        )
    )

    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x - textResult.size.width / 2,
            y - textResult.size.height
        )
    )
}

@Composable
private fun LegendItem(
    name: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
            Text(
                text = "No Data Available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Add some workout data to see your progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// Extension functions that should match your existing ones for grid and axes
private fun DrawScope.drawGrid(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color
) {
    val gridLines = 5
    val stepY = height / gridLines
    val stepX = width / gridLines

    // Horizontal grid lines
    for (i in 0..gridLines) {
        val y = offsetY + (i * stepY)
        drawLine(
            color = color,
            start = Offset(offsetX, y),
            end = Offset(offsetX + width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Vertical grid lines
    for (i in 0..gridLines) {
        val x = offsetX + (i * stepX)
        drawLine(
            color = color,
            start = Offset(x, offsetY),
            end = Offset(x, offsetY + height),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawBarChartAxes(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color
) {
    // X-axis
    drawLine(
        color = color,
        start = Offset(offsetX, offsetY + height),
        end = Offset(offsetX + width, offsetY + height),
        strokeWidth = 2.dp.toPx()
    )

    // Y-axis
    drawLine(
        color = color,
        start = Offset(offsetX, offsetY),
        end = Offset(offsetX, offsetY + height),
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawYAxisLabelsBarChart(
    minY: Float,
    maxY: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    textColor: Color
) {
    // TODO Interpret intention which may alter value type. For example: Streaks
    drawYAxisLabels(minY, maxY, height, offsetX, offsetY, textColor, "kg")
}

fun createWorkoutsPerMonthBarChart(
    exerciseProgressList: List<ExerciseProgress>,
    colorScheme: ColorScheme
): List<BarChartSeries> {
    if (exerciseProgressList.isEmpty()) return emptyList()

    // Get all sessions from all exercises and group by month
    val allSessions = exerciseProgressList.flatMap { progress ->
        progress.sessions.map { session ->
            session.date to progress.exercise.name
        }
    }

    // Group sessions by month // TODO Maybe this needs to be reverted?
    val monthFormat = SimpleDateFormat(AppSettings.getDateFormat, Locale.getDefault())
    val sessionsByMonth = allSessions.groupBy { (date, _) ->
        monthFormat.format(Date(date))
    }

    // Count total workouts per month
    val workoutsPerMonth = sessionsByMonth.map { (month, sessions) ->
        BarData(
            category = month,
            value = sessions.size.toFloat(),
            label = sessions.size.toString()
        )
    }.sortedBy {
        // Sort by actual date to maintain chronological order
        SimpleDateFormat(AppSettings.getDateFormat, Locale.getDefault()).parse(it.category)?.time ?: 0L
    }

    return listOf(
        BarChartSeries(
            name = "Workouts",
            bars = workoutsPerMonth,
            color = colorScheme.primary
        )
    )
}

fun createExerciseVolumePerMonthBarChart(
    exerciseProgressList: List<ExerciseProgress>,
    colorScheme: ColorScheme,
    maxExercisesToShow: Int = 3
): List<BarChartSeries> {
    if (exerciseProgressList.isEmpty()) return emptyList()

    val colors = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.primaryContainer,
        colorScheme.secondaryContainer
    )

    // Get the top exercises by total volume
    val topExercises = exerciseProgressList
        .sortedByDescending { progress ->
            progress.sessions.sumOf { it.totalVolume }
        }
        .take(maxExercisesToShow)
    //TODO here not in right format.
    val monthFormat = SimpleDateFormat(AppSettings.getDateFormat, Locale.getDefault())

    // Get all unique months from all exercises
    val allMonths = topExercises.flatMap { progress ->
        progress.sessions.map { session ->
            monthFormat.format(Date(session.date))
        }
    }.distinct().sorted()

    return topExercises.mapIndexed { index, progress ->
        // Group sessions by month for this exercise
        val sessionsByMonth = progress.sessions.groupBy { session ->
            monthFormat.format(Date(session.date))
        }

        // Create bars for each month (including months with 0 volume)
        val bars = allMonths.map { month ->
            val monthSessions = sessionsByMonth[month] ?: emptyList()
            val totalVolume = monthSessions.sumOf { it.totalVolume }

            BarData(
                category = month,
                value = totalVolume.toFloat(),
                label = if (totalVolume > 0) "${totalVolume.toInt()}kg" else null
            )
        }

        BarChartSeries(
            name = progress.exercise.name,
            bars = bars,
            color = colors[index % colors.size]
        )
    }
}

fun createWorkoutFrequencyByExerciseBarChart(
    exerciseProgressList: List<ExerciseProgress>,
    colorScheme: ColorScheme,
    maxExercisesToShow: Int = 10
): List<BarChartSeries> {
    if (exerciseProgressList.isEmpty()) return emptyList()

    // Count total sessions per exercise and sort by frequency
    val exerciseFrequency = exerciseProgressList
        .map { progress ->
            BarData(
                category = progress.exercise.name.take(15), // Truncate long names
                value = progress.sessions.size.toFloat(),
                label = progress.sessions.size.toString()
            )
        }
        .sortedByDescending { it.value }
        .take(maxExercisesToShow)

    return listOf(
        BarChartSeries(
            name = "Sessions",
            bars = exerciseFrequency,
            color = colorScheme.secondary
        )
    )
}

fun createProgressTrendBarChart(
    exerciseProgressList: List<ExerciseProgress>,
    colorScheme: ColorScheme,
    exerciseId: Long? = null
): List<BarChartSeries> {
    val targetExercise = if (exerciseId != null) {
        exerciseProgressList.find { it.exercise.id == exerciseId }
    } else {
        // Use the exercise with the most sessions if no specific exercise is provided
        exerciseProgressList.maxByOrNull { it.sessions.size }
    }

    if (targetExercise == null || targetExercise.sessions.isEmpty()) return emptyList()

    val monthFormat = SimpleDateFormat(AppSettings.getDateFormat, Locale.getDefault())

    // Group sessions by month and get average weight for each month
    val sessionsByMonth = targetExercise.sessions.groupBy { session ->
        monthFormat.format(Date(session.date))
    }

    val progressBars = sessionsByMonth.map { (month, sessions) ->
        val avgWeight = sessions.map { it.maxWeight }.average()
        BarData(
            category = month,
            value = avgWeight.toFloat(),
            label = "${avgWeight.toInt()}kg"
        )
    }.sortedBy {
        SimpleDateFormat(AppSettings.getDateFormat, Locale.getDefault()).parse(it.category)?.time ?: 0L
    }

    return listOf(
        BarChartSeries(
            name = "${targetExercise.exercise.name} Progress",
            bars = progressBars,
            color = colorScheme.tertiary
        )
    )
}