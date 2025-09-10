package com.example.gymlogger.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlogger.ui.composables.AddTrainingPlanScreen
import com.example.gymlogger.database.DatabaseModule
import com.example.gymlogger.repository.GymRepository
import com.example.gymlogger.ui.composables.MainNavigation
import com.example.gymlogger.ui.composables.ViewTrainingPlans
import com.example.gymlogger.utilities.dismissKeyboardOnTap

class MainActivity : ComponentActivity() {
    private var repository: GymRepository? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = DatabaseModule.getRepository()
        setContent {
            MyApplicationTheme {
                setContent {
                    MyApplicationTheme {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .dismissKeyboardOnTap(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainNavigation(repository!!)
                        }
                    }
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
