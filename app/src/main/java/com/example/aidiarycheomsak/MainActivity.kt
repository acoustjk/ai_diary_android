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
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.aidiarycheomsak.data.CompressionHelper
import com.example.aidiarycheomsak.data.DiaryReport
import com.example.aidiarycheomsak.data.PreferenceHelper
import com.example.aidiarycheomsak.theme.AiDiaryCheomsakTheme
import kotlinx.serialization.json.Json
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Create FCM notification channel
    createNotificationChannel()

    // Request POST_NOTIFICATIONS permission for Android 13+
    checkAndRequestNotificationPermission()
    
    // Anonymous Sign In to Firebase on startup
    FirebaseAuth.getInstance().signInAnonymously()
      .addOnFailureListener {
        Toast.makeText(this, "Firebase \uc5f0\uacb0\u0020\uc2e4\ud328: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
      }
      
    setContent {
      AiDiaryCheomsakTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(initialReportId = androidx.compose.runtime.remember { mutableStateOf(null) })
        }
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "일기 성장 보고서"
      val descriptionText = "자녀의 일기 작성 완료 및 크레딧 상태 알림을 수신합니다."
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel("diary_notification_channel", name, importance).apply {
        description = descriptionText
      }
      val notificationManager: NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun checkAndRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }
  }
}
