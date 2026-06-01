package com.example.aidiarycheomsak

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.aidiarycheomsak.data.CompressionHelper
import com.example.aidiarycheomsak.data.DiaryReport
import com.example.aidiarycheomsak.data.PreferenceHelper
import com.example.aidiarycheomsak.theme.AiDiaryCheomsakTheme
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AiDiaryCheomsakTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(initialReportId = androidx.compose.runtime.remember { mutableStateOf(null) })
        }
      }
    }
  }
}
