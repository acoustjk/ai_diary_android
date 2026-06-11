package com.example.aidiarycheomsak.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidiarycheomsak.data.CompressionHelper
import com.example.aidiarycheomsak.data.DiaryReport
import com.example.aidiarycheomsak.data.PreferenceHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.HttpURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentResultScreen(
    originalContent: String,
    feedback: String,
    spellingScore: Int,
    expressionScore: Int,
    stamp: String,
    hasBonus: Boolean,
    rewrittenContent: String = "",
    firstSpellingScore: Int = -1,
    firstExpressionScore: Int = -1,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    var childName by remember { mutableStateOf(preferenceHelper.childName) }
    var showNameDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var pairedReviewersList by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    // Listen to child profile document to fetch connected reviewers in real-time
    DisposableEffect(preferenceHelper.childId) {
        val docRef = FirebaseFirestore.getInstance().collection("children").document(preferenceHelper.childId)
        val registration = docRef.addSnapshotListener { snapshot, e ->
            if (snapshot != null && snapshot.exists()) {
                val reviewers = snapshot.get("pairedReviewers") as? List<Map<String, String>>
                pairedReviewersList = reviewers ?: emptyList()
            }
        }
        onDispose {
            registration.remove()
        }
    }

    // Score Fill Animation
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val animatedSpellingFill by animateFloatAsState(
        targetValue = if (animationPlayed) (spellingScore / 100f) else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "spelling"
    )

    val animatedExpressionFill by animateFloatAsState(
        targetValue = if (animationPlayed) (expressionScore / 100f) else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "expression"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👩‍🏫 마법 힌트 & 결과", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7FAFC))
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAFC))
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Stamp Decoration
            val stampColor = when (stamp) {
                "참 잘했어요" -> Color(0xFFE53E3E)
                "좋은 시도예요" -> Color(0xFFDD6B20)
                else -> Color(0xFF3182CE)
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(3.dp, stampColor, RoundedCornerShape(50.dp))
                    .background(Color.White, RoundedCornerShape(50.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💮", fontSize = 24.sp)
                    Text(
                        text = stamp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = stampColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Scores Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF8FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📊 오늘 받은 평가 점수",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B6CB0),
                        fontSize = 15.sp
                    )

                    // Spelling Score Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("✨ 맞춤법 점수", fontSize = 13.sp, color = Color(0xFF4A5568))
                            Text("${spellingScore}점", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B6CB0))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedSpellingFill },
                            color = Color(0xFF3182CE),
                            trackColor = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }

                    // Expression Score Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💡 표현력 점수", fontSize = 13.sp, color = Color(0xFF4A5568))
                            Text("${expressionScore}점", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B6CB0))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedExpressionFill },
                            color = Color(0xFF48BB78),
                            trackColor = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }

                    if (hasBonus) {
                        Text(
                            text = "🎯 미션 성공! 보너스 점수가 포함되었습니다 (+10)",
                            color = Color(0xFFE53E3E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // AI Feedback Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👾 AI고치의 다정한 피드백",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748),
                        fontSize = 15.sp
                    )

                    Text(
                        text = feedback,
                        fontSize = 14.sp,
                        color = Color(0xFF4A5568),
                        lineHeight = 20.sp
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pairedReviewersList.isEmpty()) {
                    // Not paired: Show warning for both 1st and 2nd draft stages
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                        border = BorderStroke(1.dp, Color(0xFFFEB2B2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("⚠️", fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = "부모님 앱과 연결되어 있지 않습니다",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFC53030)
                                    )
                                    Text(
                                        text = if (rewrittenContent.isEmpty()) {
                                            "AI고치의 힌트를 보고 일기를 고쳐 쓰더라도 부모님이 보실 수 없어. 먼저 기기 연결(페어링)을 진행해줘!"
                                        } else {
                                            "일기는 저장되었으나, 부모님이 실시간으로 보시려면 기기 연결(페어링)이 필요합니다."
                                        },
                                        fontSize = 12.sp,
                                        color = Color(0xFF9B2C2C),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("기기 연결(페어링) 설정하러 가기")
                            }
                        }
                    }
                } else {
                    // Paired: Show different message for 1st and 2nd draft stages
                    if (rewrittenContent.isEmpty()) {
                        // 1st draft: Show info that rewrite is needed to send
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("✅", fontSize = 24.sp)
                                Column {
                                    val names = pairedReviewersList.map { it["name"] ?: "보호자" }.joinToString(", ")
                                    Text(
                                        text = "부모님 앱과 연결되어 있습니다",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "연결된 분: $names\n아래의 '다시 고쳐 쓰러 가기' 버튼을 눌러 일기를 수정하고 완료하면 보고서가 자동으로 전송됩니다.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // 2nd draft (completed rewrite): Show success report sent card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🚀", fontSize = 24.sp)
                                Column {
                                    val names = pairedReviewersList.map { it["name"] ?: "보호자" }.joinToString(", ")
                                    Text(
                                        text = "부모님 앱으로 보고서 전송 완료!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "작성하신 일기와 AI 첨삭 결과가 연결된 보호자($names) 앱으로 자동 전송되었습니다.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Go back button
                OutlinedButton(
                    onClick = {
                        if (rewrittenContent.isNotEmpty()) {
                            // Completed rewrite: prepare to clear input field for a new diary
                            preferenceHelper.clearDiaryTextPending = true
                        } else {
                            // First draft completed: prepare to enter rewrite mode
                            preferenceHelper.isRewriteModePending = true
                            preferenceHelper.pendingOriginalContent = originalContent
                            preferenceHelper.pendingOriginalFeedback = feedback
                            preferenceHelper.pendingSpellingScore = spellingScore
                            preferenceHelper.pendingExpressionScore = expressionScore
                        }
                        onBack()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(if (rewrittenContent.isNotEmpty()) "✏️ 다른 일기 쓰러 가기" else "✏️ 다시 고쳐 쓰러 가기")
                }
            }
        }
    }
}
