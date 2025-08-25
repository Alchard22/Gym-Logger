package com.example.gymlogger.ui.composables

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.example.gymlogger.repository.GymRepository
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun MainNavigation(
    gymRepository: GymRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Home/Main screen
        composable("home") {
            HomeScreen(gymRepository, navController)
        }

        // View existing training plans
        composable("view_training_plans") {
            ViewTrainingPlans(
                repository = gymRepository,
                onPlanSelected = { planId ->
                    navController.navigate("training_plan_management/$planId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Training plan management
        composable(
            route = "training_plan_management/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable

            ManageTrainingPlan(
                planId = planId,
                repository = gymRepository,
                onStartWorkout = { planId ->
                    navController.navigate("new_workout/$planId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add training plan
        composable("add_training_plan") {
            AddTrainingPlanScreen(
                repository = gymRepository,
                onPlanAdded = { planId ->
                    // Navigate to the new plan's management screen
                    navController.navigate("training_plan_management/$planId") {
                        popUpTo("view_training_plans")
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "new_workout/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable

            NewWorkout(
                planId = planId,
                repository = gymRepository,
                onWorkoutCreated = { planId ->
                    navController.navigate("start_workout/$planId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                })
        }
    }
}