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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    var childName by remember { mutableStateOf(preferenceHelper.childName) }
    var showNameDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                        text = "👩‍🏫 AI 선생님의 마법 피드백",
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
                // Share with Parent Button (Only visible after rewrite)
                if (rewrittenContent.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (preferenceHelper.childName.isNotBlank()) {
                                childName = preferenceHelper.childName
                                scope.launch {
                                    val actualFirstSpelling = if (firstSpellingScore != -1) firstSpellingScore else (spellingScore - 10)
                                    val actualFirstExpression = if (firstExpressionScore != -1) firstExpressionScore else (expressionScore - 10)
                                    val actualSecondSpelling = spellingScore
                                    val actualSecondExpression = expressionScore

                                    // Prepare DiaryReport data
                                    val report = DiaryReport(
                                        name = childName,
                                        originalContent = originalContent,
                                        originalFeedback = feedback,
                                        rewrittenContent = if (rewrittenContent.isNotEmpty()) rewrittenContent else originalContent,
                                        firstSpellingScore = actualFirstSpelling,
                                        firstExpressionScore = actualFirstExpression,
                                        secondSpellingScore = actualSecondSpelling,
                                        secondExpressionScore = actualSecondExpression,
                                        stamp = stamp,
                                        improved = rewrittenContent.isNotEmpty()
                                    )

                                    // Serialize to JSON, compress and encode
                                    val jsonStr = Json.encodeToString(report)
                                    val encoded = CompressionHelper.compress(jsonStr)
                                    val serverUrl = preferenceHelper.serverUrl.trim().removeSuffix("/")
                                    val longLink = "$serverUrl/?r=$encoded"

                                    // Shorten link using backend proxy endpoint
                                    val webLink = shortenUrl(serverUrl, longLink)

                                    // Trigger Share Intent
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "✨ [AI 일기 마법 첨삭 보고서] 💮\n\n" +
                                                    "👦 작가: $childName 어린이\n" +
                                                    "💮 평가: $stamp\n\n" +
                                                    "선생님의 피드백 힌트를 읽고 스스로 글솜씨를 쑥쑥 레벨업했어요! 아래 링크를 눌러 아이의 일기 성장 보고서를 확인해보세요. 👇\n" +
                                                    "🔗 보고서 보기: $webLink"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "부모님께 보고서 자랑하기")
                                    context.startActivity(shareIntent)
                                }
                            } else {
                                showNameDialog = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF48BB78)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "공유")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("💌 부모님께 성장 보고서 자랑하기", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Let the user know they can share after they try rewriting
                    Text(
                        text = "💡 선생님의 힌트를 읽고 아래 버튼을 눌러 일기를 고쳐 쓰면 부모님께 성장 보고서를 공유할 수 있어요!",
                        fontSize = 12.sp,
                        color = Color(0xFF718096),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
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

    // Name dialog for sharing report
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("어린이 이름 입력", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("부모님께 보낼 보고서에 들어갈 이름을 입력해주세요.")
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it },
                        placeholder = { Text("예: 김하늘") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (childName.isBlank()) {
                            Toast.makeText(context, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showNameDialog = false
                        preferenceHelper.childName = childName // Save name to preferences

                        scope.launch {
                            val actualFirstSpelling = if (firstSpellingScore != -1) firstSpellingScore else (spellingScore - 10)
                            val actualFirstExpression = if (firstExpressionScore != -1) firstExpressionScore else (expressionScore - 10)
                            val actualSecondSpelling = spellingScore
                            val actualSecondExpression = expressionScore

                            // Prepare DiaryReport data
                            val report = DiaryReport(
                                name = childName,
                                originalContent = originalContent,
                                originalFeedback = feedback,
                                rewrittenContent = if (rewrittenContent.isNotEmpty()) rewrittenContent else originalContent,
                                firstSpellingScore = actualFirstSpelling,
                                firstExpressionScore = actualFirstExpression,
                                secondSpellingScore = actualSecondSpelling,
                                secondExpressionScore = actualSecondExpression,
                                stamp = stamp,
                                improved = rewrittenContent.isNotEmpty()
                            )

                            // Serialize to JSON, compress and encode
                            val jsonStr = Json.encodeToString(report)
                            val encoded = CompressionHelper.compress(jsonStr)
                            val serverUrl = preferenceHelper.serverUrl.trim().removeSuffix("/")
                            val longLink = "$serverUrl/?r=$encoded"

                            // Shorten link using backend proxy endpoint
                            val webLink = shortenUrl(serverUrl, longLink)

                            // Trigger Share Intent
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "✨ [AI 일기 마법 첨삭 보고서] 💮\n\n" +
                                            "👦 작가: $childName 어린이\n" +
                                            "💮 평가: $stamp\n\n" +
                                            "선생님의 피드백 힌트를 읽고 스스로 글솜씨를 쑥쑥 레벨업했어요! 아래 링크를 눌러 아이의 일기 성장 보고서를 확인해보세요. 👇\n" +
                                            "🔗 보고서 보기: $webLink"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "부모님께 보고서 자랑하기")
                            context.startActivity(shareIntent)
                        }
                    }
                ) {
                    Text("전송하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

suspend fun shortenUrl(serverUrl: String, longUrl: String): String = withContext(Dispatchers.IO) {
    try {
        val apiUrl = "${serverUrl}/shorten?url=" + java.net.URLEncoder.encode(longUrl, "UTF-8")
        val url = URL(apiUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        
        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            val json = org.json.JSONObject(response.toString())
            json.optString("shorturl", longUrl)
        } else {
            longUrl
        }
    } catch (e: Exception) {
        e.printStackTrace()
        longUrl
    }
}
