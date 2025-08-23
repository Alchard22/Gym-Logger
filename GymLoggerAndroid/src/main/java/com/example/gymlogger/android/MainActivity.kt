package com.example.gymlogger.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gymlogger.Greeting
import com.example.gymlogger.composables.AddTrainingPlanScreen
import com.example.gymlogger.database.DatabaseModule
import com.example.gymlogger.repository.GymRepository

class MainActivity : ComponentActivity() {
    private var repository: GymRepository? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = DatabaseModule.getRepository()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GreetingView(Greeting().greet())
                    AddTrainingPlanScreen(
                        repository = repository!!,
                        onPlanAdded = { planId ->
                            // Handle successful creation
                            println("Created plan with ID: $planId")
                            // Navigate to plan details or back to list
                        },
                        onNavigateBack = {
                            // Handle back navigation
                            finish() // or use proper navigation
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}

//@Preview
//@Composable
//fun DefaultPreview() {
//    MyApplicationTheme {
//        GreetingView("Hello, Android!")
//    }
//}
