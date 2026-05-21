package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BudgetTrackerViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val viewModel: BudgetTrackerViewModel by viewModels {
      BudgetTrackerViewModel.Factory(application)
    }

    setContent {
      val pref by viewModel.preferences.collectAsStateWithLifecycle()
      MyApplicationTheme(darkModeSetting = pref.darkModeSetting) {
        MainDashboard(viewModel = viewModel)
      }
    }
  }
}
