package com.example.aidiarycheomsak.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.example.aidiarycheomsak.data.DiaryReport
import com.example.aidiarycheomsak.data.GeminiService
import com.example.aidiarycheomsak.data.PreferenceHelper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onNavigateToResult: (originalContent: String, feedback: String, spellingScore: Int, expressionScore: Int, stamp: String, hasBonus: Boolean, rewrittenContent: String, firstSpelling: Int, firstExpression: Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    val scope = rememberCoroutineScope()

    // State variables
    var diaryText by rememberSaveable { mutableStateOf("") }
    var charCount by rememberSaveable { mutableIntStateOf(0) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    // Mission Words (Selected randomly once)
    val wordPool = remember { listOf("행복", "초콜릿", "하늘", "우당탕탕", "친구", "컴퓨터", "선생님", "강아지", "자전거", "비밀", "맛있는", "가족") }
    var missionWordsString by rememberSaveable { mutableStateOf("") }
    val missionWords = remember(missionWordsString) {
        if (missionWordsString.isEmpty()) {
            val words = wordPool.shuffled().take(3)
            missionWordsString = words.joinToString(",")
            words
        } else {
            missionWordsString.split(",")
        }
    }

    // Rewrite mode state (passed in via local storage or memory)
    var isRewriteMode by rememberSaveable { mutableStateOf(false) }
    var originalContent by rememberSaveable { mutableStateOf("") }
    var originalFeedback by rememberSaveable { mutableStateOf("") }
    var prevSpelling by rememberSaveable { mutableIntStateOf(0) }
    var prevExpression by rememberSaveable { mutableIntStateOf(0) }

    // Load draft on initial composition
    LaunchedEffect(Unit) {
        if (preferenceHelper.draftDiaryText.isNotEmpty()) {
            diaryText = preferenceHelper.draftDiaryText
            charCount = diaryText.length
            isRewriteMode = preferenceHelper.draftIsRewriteMode
            originalContent = preferenceHelper.draftOriginalContent
            originalFeedback = preferenceHelper.draftOriginalFeedback
            prevSpelling = preferenceHelper.draftPrevSpelling
            prevExpression = preferenceHelper.draftPrevExpression
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (preferenceHelper.isRewriteModePending) {
                    isRewriteMode = true
                    originalContent = preferenceHelper.pendingOriginalContent
                    originalFeedback = preferenceHelper.pendingOriginalFeedback
                    prevSpelling = preferenceHelper.pendingSpellingScore
                    prevExpression = preferenceHelper.pendingExpressionScore
                    
                    // Retain the text and fill the input field with the child's draft so they can edit it
                    diaryText = preferenceHelper.pendingOriginalContent
                    charCount = diaryText.length

                    preferenceHelper.isRewriteModePending = false

                    // Save to persistent draft
                    preferenceHelper.draftDiaryText = diaryText
                    preferenceHelper.draftIsRewriteMode = true
                    preferenceHelper.draftOriginalContent = originalContent
                    preferenceHelper.draftOriginalFeedback = originalFeedback
                    preferenceHelper.draftPrevSpelling = prevSpelling
                    preferenceHelper.draftPrevExpression = prevExpression
                } else if (preferenceHelper.clearDiaryTextPending) {
                    diaryText = ""
                    charCount = 0
                    isRewriteMode = false
                    originalContent = ""
                    originalFeedback = ""
                    prevSpelling = 0
                    prevExpression = 0
                    
                    preferenceHelper.clearDiaryTextPending = false

                    // Clear persistent draft
                    preferenceHelper.draftDiaryText = ""
                    preferenceHelper.draftIsRewriteMode = false
                    preferenceHelper.draftOriginalContent = ""
                    preferenceHelper.draftOriginalFeedback = ""
                    preferenceHelper.draftPrevSpelling = 0
                    preferenceHelper.draftPrevExpression = 0
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Streak Check
    var streak by remember { mutableIntStateOf(preferenceHelper.streakCount) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("✍️ 어린이 일기장", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF0F4F8))
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F4F8))
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 👦 어린이 작가 이름 카드
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "👦 어린이 작가 이름:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2D3748)
                    )
                    var nameInput by remember { mutableStateOf(preferenceHelper.childName) }
                    
                    LaunchedEffect(preferenceHelper.childName) {
                        nameInput = preferenceHelper.childName
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            preferenceHelper.childName = it
                        },
                        placeholder = { Text("이름을 입력해 주세요") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF7FAFC),
                            unfocusedContainerColor = Color(0xFFF7FAFC)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Dashboard (Streak & Badges)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF8FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔥 연속 일기 작가 도전!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B6CB0),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "현재 연속 ${streak}일째 일기 쓰는 중! 멋져요!",
                        fontSize = 14.sp,
                        color = Color(0xFF4A5568)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BadgeItem(icon = "🌱", name = "새싹(1일)", active = streak >= 1)
                        BadgeItem(icon = "🪵", name = "꾸준함(3일)", active = streak >= 3)
                        BadgeItem(icon = "👑", name = "마스터(5일)", active = streak >= 5)
                    }
                }
            }

            // Dashboard details card helper
            if (isRewriteMode) {
                // AI Feedback Reference Card (Replaces Mission words in Rewrite Mode)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFADF)),
                    border = BorderStroke(1.dp, Color(0xFFF6AD55)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👩‍🏫 AI 선생님의 마법 힌트 (참고해서 고쳐 써요)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDD6B20),
                            fontSize = 15.sp
                        )
                        Text(
                            text = originalFeedback,
                            fontSize = 13.sp,
                            color = Color(0xFF7B341E),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // Mission Box (Only visible in normal mode)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFADF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎯 오늘의 비밀 단어 미션",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDD6B20),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "아래 단어들을 일기에 넣으면 보너스 점수 +10점!",
                            fontSize = 13.sp,
                            color = Color(0xFF4A5568)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            missionWords.forEach { word ->
                                val isUsed = diaryText.contains(word)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isUsed) Color(0xFF48BB78) else Color(0xFFED8936))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "📍 $word",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rewrite Mode Banner
            if (isRewriteMode) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                    border = BorderStroke(1.dp, Color(0xFF805AD5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💡 선생님 피드백 반영해서 고쳐 쓰는 중!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF553C9A),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "선생님이 준 힌트를 바탕으로 고쳐 써보세요.",
                                color = Color(0xFF553C9A),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = {
                            isRewriteMode = false
                            originalContent = ""
                            originalFeedback = ""
                            prevSpelling = 0
                            prevExpression = 0
                            
                            preferenceHelper.draftIsRewriteMode = false
                            preferenceHelper.draftOriginalContent = ""
                            preferenceHelper.draftOriginalFeedback = ""
                            preferenceHelper.draftPrevSpelling = 0
                            preferenceHelper.draftPrevExpression = 0
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "취소", tint = Color(0xFF553C9A))
                        }
                    }
                }
            }

            // Diary Write Box
            Column {
                OutlinedTextField(
                    value = diaryText,
                    onValueChange = { newValue ->
                        // Copy/paste detection: if the length jumps by 10+ characters suddenly, block it
                        if (newValue.length - diaryText.length > 10) {
                            Toast.makeText(context, "일기는 직접 손으로 적어야 실력이 늘어요! 😉", Toast.LENGTH_SHORT).show()
                        } else {
                            diaryText = newValue
                            charCount = newValue.length
                            preferenceHelper.draftDiaryText = newValue
                        }
                    },
                    placeholder = { Text("오늘 하루는 어땠나요? 오늘의 비밀 단어를 넣어서 신나게 적어보세요!") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⏱️ 글자 수: ${charCount}자",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568),
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    val serverUrl = preferenceHelper.serverUrl
                    if (serverUrl.isBlank()) {
                        Toast.makeText(context, "설정 화면에서 백엔드 서버 주소를 등록해주세요!", Toast.LENGTH_LONG).show()
                        onNavigateToSettings()
                        return@Button
                    }
                    if (diaryText.trim().isBlank()) {
                        Toast.makeText(context, "일기 내용을 입력해주세요!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Check mission success
                    val missionSuccess = missionWords.all { diaryText.contains(it) }

                    scope.launch {
                        isLoading = true
                        try {
                            val res = GeminiService.checkDiary(
                                serverUrl = serverUrl,
                                content = diaryText,
                                originalContent = if (isRewriteMode) originalContent else null,
                                feedback = if (isRewriteMode) originalFeedback else null,
                                apiKey = preferenceHelper.geminiApiKey
                            )

                            // Apply bonus score if mission succeeded
                            val finalSpelling = if (missionSuccess) Math.min(100, res.spelling_score + 10) else res.spelling_score
                            val finalExpression = if (missionSuccess) Math.min(100, res.expression_score + 10) else res.expression_score

                            // Update continuous write counts on success
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            if (preferenceHelper.lastWriteDate != todayStr) {
                                val yesterdaysCal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
                                val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdaysCal.time)
                                if (preferenceHelper.lastWriteDate == yesterdayStr) {
                                    preferenceHelper.streakCount += 1
                                } else {
                                    preferenceHelper.streakCount = 1
                                }
                                preferenceHelper.lastWriteDate = todayStr
                                streak = preferenceHelper.streakCount
                            }

                            // Save this draft into parent records (to trigger "Parent Report" export)
                            val report = DiaryReport(
                                originalContent = if (isRewriteMode) originalContent else diaryText,
                                originalFeedback = if (isRewriteMode) originalFeedback else res.feedback,
                                rewrittenContent = if (isRewriteMode) diaryText else "",
                                firstSpellingScore = if (isRewriteMode) prevSpelling else finalSpelling,
                                firstExpressionScore = if (isRewriteMode) prevExpression else finalExpression,
                                secondSpellingScore = if (isRewriteMode) finalSpelling else 0,
                                secondExpressionScore = if (isRewriteMode) finalExpression else 0,
                                stamp = res.stamp,
                                improved = res.improved
                            )
                            preferenceHelper.saveReport(report)

                            onNavigateToResult(
                                if (isRewriteMode) originalContent else diaryText,
                                res.feedback,
                                finalSpelling,
                                finalExpression,
                                res.stamp,
                                missionSuccess,
                                if (isRewriteMode) diaryText else "",
                                if (isRewriteMode) prevSpelling else finalSpelling,
                                if (isRewriteMode) prevExpression else finalExpression
                            )

                            // Clear rewrite flags on success
                            if (isRewriteMode) {
                                isRewriteMode = false
                            }

                            // Clear persistent draft on success
                            preferenceHelper.draftDiaryText = ""
                            preferenceHelper.draftIsRewriteMode = false
                            preferenceHelper.draftOriginalContent = ""
                            preferenceHelper.draftOriginalFeedback = ""
                            preferenceHelper.draftPrevSpelling = 0
                            preferenceHelper.draftPrevExpression = 0

                        } catch (e: Exception) {
                            Toast.makeText(context, "AI 선생님 연결 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRewriteMode) Color(0xFF805AD5) else Color(0xFF3182CE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 선생님이 분석하는 중 (약 5초)...")
                } else {
                    Text(if (isRewriteMode) "✨ 고친 일기 다시 검사받기" else "🤖 AI 선생님께 일기 검사받기")
                }
            }

            // Test Toggle for Rewrite Mode (Since we are testing, this lets the user simulate receiving feedback and rewriting)
            if (diaryText.length > 5 && !isRewriteMode) {
                OutlinedButton(
                    onClick = {
                        originalContent = diaryText
                        originalFeedback = "선생님 피드백 예시: '컴퓨터' 단어를 쓴 것은 좋으나 맞춤법에 주의해보세요. '맛있는' 어휘 외에 '꿀맛인'을 써볼까요?"
                        prevSpelling = 70
                        prevExpression = 75
                        isRewriteMode = true

                        // Save to persistent draft
                        preferenceHelper.draftDiaryText = diaryText
                        preferenceHelper.draftIsRewriteMode = true
                        preferenceHelper.draftOriginalContent = originalContent
                        preferenceHelper.draftOriginalFeedback = originalFeedback
                        preferenceHelper.draftPrevSpelling = prevSpelling
                        preferenceHelper.draftPrevExpression = prevExpression
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚙️ [테스트용] 피드백 수정 모드 강제 켜기")
                }
            }
        }
    }
}

@Composable
fun BadgeItem(icon: String, name: String, active: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier
                .padding(bottom = 2.dp)
                .alpha(if (active) 1f else 0.2f)
        )
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color(0xFF2B6CB0) else Color(0xFF718096),
            modifier = Modifier.alpha(if (active) 1f else 0.3f)
        )
    }
}
