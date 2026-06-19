package com.example.aidiarycheomsak.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import com.example.aidiarycheomsak.data.DiaryReport
import com.example.aidiarycheomsak.data.GeminiService
import com.example.aidiarycheomsak.data.PreferenceHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    val scanner = remember { com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context) }
    var isScanning by remember { mutableStateOf(false) }
    var showApprovedDialog by remember { mutableStateOf(false) }
    var scannedSessionId by remember { mutableStateOf("") }

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

    var pairedReviewersList by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var credits by remember { mutableIntStateOf(0) }
    var showNoCreditDialog by remember { mutableStateOf(false) }

    // Listen to child profile document to fetch connected reviewers & credits in real-time
    DisposableEffect(preferenceHelper.childId) {
        val docRef = FirebaseFirestore.getInstance().collection("children").document(preferenceHelper.childId)
        val registration = docRef.addSnapshotListener { snapshot, e ->
            if (snapshot != null) {
                if (snapshot.exists()) {
                    val reviewers = snapshot.get("pairedReviewers") as? List<Map<String, String>>
                    pairedReviewersList = reviewers ?: emptyList()
                    
                    val serverCredits = snapshot.getLong("credits")
                    if (serverCredits != null) {
                        credits = serverCredits.toInt()
                    } else {
                        // Document exists but no credits field, initialize it
                        docRef.update("credits", 0, "totalCreditsGranted", 0)
                        credits = 0
                    }
                } else {
                    // Document does not exist yet. Initialize it with 0 credits!
                    val initialData = mapOf(
                        "childId" to preferenceHelper.childId,
                        "childName" to preferenceHelper.childName.ifBlank { "무명 어린이" },
                        "credits" to 0,
                        "totalCreditsGranted" to 0,
                        "pairedReviewers" to emptyList<Map<String, String>>()
                    )
                    docRef.set(initialData)
                    credits = 0
                }
            }
        }
        onDispose {
            registration.remove()
        }
    }


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
                    preferenceHelper.draftDiaryId = ""
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
        if (pairedReviewersList.isEmpty() && credits == 0) {
            var homePairingCode by remember { mutableStateOf("") }
            var isGeneratingHomeCode by remember { mutableStateOf(false) }

            LaunchedEffect(preferenceHelper.childId) {
                val docRef = FirebaseFirestore.getInstance().collection("children").document(preferenceHelper.childId)
                docRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val code = snapshot.getString("pairingCode") ?: ""
                        val expires = snapshot.getLong("pairingCodeExpires") ?: 0L
                        if (code.isNotEmpty() && expires > System.currentTimeMillis() / 1000) {
                            homePairingCode = code
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F4F8))
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔗 보호자 연결 필요",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748)
                        )

                        Text(
                            text = "AI고치 일기장을 사용하려면 보호자 기기와의 연결이 필요합니다.\n\n먼저 아래에 어린이의 이름을 적고 연결 코드를 발급받아 보호자 앱에 등록해 주세요! 🎁",
                            fontSize = 13.sp,
                            color = Color(0xFF718096),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        var childNameInput by remember { mutableStateOf(preferenceHelper.childName) }
                        var lastSyncedPairingName by remember { mutableStateOf(preferenceHelper.childName) }

                        LaunchedEffect(childNameInput) {
                            if (childNameInput.isNotBlank() && childNameInput != lastSyncedPairingName) {
                                kotlinx.coroutines.delay(1000) // Debounce for 1 second
                                if (childNameInput != lastSyncedPairingName) {
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("children")
                                        .document(preferenceHelper.childId)
                                        .update("childName", childNameInput)
                                        .addOnSuccessListener {
                                            lastSyncedPairingName = childNameInput
                                        }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = childNameInput,
                            onValueChange = {
                                childNameInput = it
                                preferenceHelper.childName = it
                            },
                            placeholder = { Text("이름을 입력해 주세요") },
                            label = { Text("어린이 이름") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (homePairingCode.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEDF2F7), RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("인증 연결 코드 (10분간 유효)", fontSize = 11.sp, color = Color(0xFF718096))
                                Text(
                                    text = "${homePairingCode.substring(0, 3)} ${homePairingCode.substring(3, 6)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2B6CB0),
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cleanName = childNameInput.trim()
                                if (cleanName.isEmpty()) {
                                    Toast.makeText(context, "이름을 먼저 입력해 주세요!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGeneratingHomeCode = true
                                val newCode = (100000..999999).random().toString()
                                val childData = mapOf(
                                    "childId" to preferenceHelper.childId,
                                    "childName" to cleanName,
                                    "pairingCode" to newCode,
                                    "pairingCodeExpires" to (System.currentTimeMillis() / 1000 + 600) // 10 minutes
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("children")
                                    .document(preferenceHelper.childId)
                                    .set(childData, SetOptions.merge())
                                    .addOnSuccessListener {
                                        homePairingCode = newCode
                                        isGeneratingHomeCode = false
                                        Toast.makeText(context, "페어링 코드가 발급되었습니다! 10분간 유효합니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isGeneratingHomeCode = false
                                        Toast.makeText(context, "코드 발급 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            enabled = !isGeneratingHomeCode,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF48BB78)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingHomeCode) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (homePairingCode.isEmpty()) "연결 코드 발급받기" else "새로운 연결 코드 재발급",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
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
                    var lastSyncedDashboardName by remember { mutableStateOf(preferenceHelper.childName) }
                    
                    LaunchedEffect(preferenceHelper.childName) {
                        if (nameInput != preferenceHelper.childName) {
                            nameInput = preferenceHelper.childName
                        }
                    }

                    LaunchedEffect(nameInput) {
                        if (nameInput.isNotBlank() && nameInput != lastSyncedDashboardName) {
                            kotlinx.coroutines.delay(1000) // Debounce for 1 second
                            if (nameInput != lastSyncedDashboardName) {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("children")
                                    .document(preferenceHelper.childId)
                                    .update("childName", nameInput)
                                    .addOnSuccessListener {
                                        lastSyncedDashboardName = nameInput
                                    }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            preferenceHelper.childName = it
                        },
                        placeholder = { Text("이름을 입력해 주세요") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF7FAFC),
                            unfocusedContainerColor = Color(0xFFF7FAFC)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 🪙 남은 마법이슬 카드
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
                border = BorderStroke(1.dp, Color(0xFFFBD38D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🪙", fontSize = 24.sp)
                    Column {
                        Text(
                            text = "남은 마법이슬: ${credits}개",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFB7791F)
                        )
                        Text(
                            text = "일기 1회 분석 시작 시 마법이슬 1개가 차감됩니다.",
                            fontSize = 12.sp,
                            color = Color(0xFF744210)
                        )
                    }
                }
            }

            // 🔗 보호자 연결 상태 카드
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (pairedReviewersList.isEmpty()) Color(0xFFFFF5F5) else Color(0xFFF0FDF4)
                ),
                border = BorderStroke(
                    1.dp,
                    if (pairedReviewersList.isEmpty()) Color(0xFFFEB2B2) else Color(0xFFBBF7D0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pairedReviewersList.isEmpty()) {
                        Text(text = "⚠️", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "보호자 앱과 연결되어 있지 않아요",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFC53030)
                            )
                            Text(
                                text = "여기를 눌러 연결 코드(6자리)를 확인하세요.",
                                fontSize = 12.sp,
                                color = Color(0xFF9B2C2C)
                            )
                        }
                    } else {
                        Text(text = "✅", fontSize = 20.sp)
                        Column {
                            val names = pairedReviewersList.map { it["name"] ?: "보호자" }.joinToString(", ")
                            Text(
                                text = "보호자 앱과 연결되었습니다",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "연결된 보호자: $names (일기가 자동으로 전송됩니다)",
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
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
                            text = "👾 AI고치의 다정한 피드백 (참고해서 고쳐 써봐!)",
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
                                text = "💡 AI고치 피드백 반영해서 고쳐 쓰는 중!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF553C9A),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "AI고치가 준 힌트를 바탕으로 고쳐 써봐!",
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
                            preferenceHelper.draftDiaryId = ""
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
                    textStyle = TextStyle(color = Color.Black),
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
                    if (credits <= 0 && !isRewriteMode) {
                        showNoCreditDialog = true
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
                                apiKey = preferenceHelper.geminiApiKey,
                                childId = preferenceHelper.childId,
                                childName = preferenceHelper.childName
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

                            // Determine the diaryId to merge 1st and 2nd writings
                            val diaryId = if (isRewriteMode && preferenceHelper.draftDiaryId.isNotEmpty()) {
                                preferenceHelper.draftDiaryId
                            } else {
                                val newId = java.util.UUID.randomUUID().toString()
                                preferenceHelper.draftDiaryId = newId
                                newId
                            }

                            // Save to Firestore and Local Prefs ONLY when rewrite is completed (2nd draft)
                            if (isRewriteMode) {
                                val report = DiaryReport(
                                    id = diaryId,
                                    name = preferenceHelper.childName.ifBlank { "무명 어린이" },
                                    originalContent = originalContent,
                                    originalFeedback = originalFeedback,
                                    rewrittenContent = diaryText,
                                    firstSpellingScore = prevSpelling,
                                    firstExpressionScore = prevExpression,
                                    secondSpellingScore = finalSpelling,
                                    secondExpressionScore = finalExpression,
                                    stamp = res.stamp,
                                    improved = res.improved,
                                    originalLength = originalContent.length,
                                    rewrittenLength = diaryText.length
                                )
                                preferenceHelper.saveReport(report)

                                // Save to Firebase Firestore under children/{childId}/diaries/{diaryId}
                                val db = FirebaseFirestore.getInstance()
                                val diaryData = mapOf(
                                    "diaryId" to diaryId,
                                    "timestamp" to report.timestamp,
                                    "name" to report.name,
                                    "originalContent" to report.originalContent,
                                    "originalFeedback" to report.originalFeedback,
                                    "rewrittenContent" to report.rewrittenContent,
                                    "firstSpellingScore" to report.firstSpellingScore,
                                    "firstExpressionScore" to report.firstExpressionScore,
                                    "secondSpellingScore" to report.secondSpellingScore,
                                    "secondExpressionScore" to report.secondExpressionScore,
                                    "stamp" to report.stamp,
                                    "improved" to report.improved,
                                    "originalLength" to report.originalLength,
                                    "rewrittenLength" to report.rewrittenLength
                                )
                                db.collection("children").document(preferenceHelper.childId)
                                    .collection("diaries").document(diaryId)
                                    .set(diaryData, SetOptions.merge())
                            }

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
                            Toast.makeText(context, "AI고치 연결 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                    Text("AI고치가 분석하는 중 (약 5초)...")
                } else {
                    Text(if (isRewriteMode) "✨ 고친 일기 다시 보여주기" else "👾 AI고치에게 일기 보여주기")
                }
            }

            // 💻 컴퓨터로 일기쓰기 (웹 화면 연동) 버튼 추가
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    if (preferenceHelper.childId.isBlank()) {
                        Toast.makeText(context, "자녀 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    isScanning = true
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val rawValue = barcode.rawValue ?: ""
                            if (rawValue.startsWith("session_")) {
                                scannedSessionId = rawValue
                                showApprovedDialog = true
                            } else {
                                Toast.makeText(context, "올바르지 않은 QR 코드입니다.", Toast.LENGTH_SHORT).show()
                            }
                            isScanning = false
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "스캔 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            isScanning = false
                        }
                },
                enabled = !isScanning && !isLoading,
                border = BorderStroke(1.5.dp, Color(0xFF3182CE)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3182CE)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = Color(0xFF3182CE), modifier = Modifier.size(24.dp))
                } else {
                    Text("💻 컴퓨터로 일기쓰기 (웹 화면 연동)", fontWeight = FontWeight.Bold)
                }
            }

            // Test Toggle for Rewrite Mode (Since we are testing, this lets the user simulate receiving feedback and rewriting)
            if (diaryText.length > 5 && !isRewriteMode) {
                OutlinedButton(
                    onClick = {
                        originalContent = diaryText
                        originalFeedback = "피드백 예시: '컴퓨터' 단어를 쓴 것은 좋지만 맞춤법에 주의해봐! '맛있는' 대신 '꿀맛인'을 써볼까?"
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

    if (showNoCreditDialog) {
        AlertDialog(
            onDismissRequest = { showNoCreditDialog = false },
            title = { Text("🪙 마법이슬 부족", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("무료 마법이슬을 모두 사용했어요!\n보호자 스마트폰 앱에서 마법이슬을 충전해 달라고 말씀드려 보세요. 😊", color = Color.DarkGray) },
            confirmButton = {
                Button(onClick = { showNoCreditDialog = false }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNoCreditDialog = false
                        scope.launch {
                            try {
                                val serverUrl = preferenceHelper.serverUrl
                                val success = GeminiService.requestCredits(
                                    serverUrl = serverUrl,
                                    childId = preferenceHelper.childId,
                                    childName = preferenceHelper.childName
                                )
                                if (success) {
                                    Toast.makeText(context, "보호자님께 마법이슬 충전 요청을 보냈어요! 💌", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "충전 요청 전송에 실패했어요. 😢", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("보호자님께 충전 요청하기", color = Color(0xFFE53E3E), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showApprovedDialog) {
        AlertDialog(
            onDismissRequest = { showApprovedDialog = false },
            title = { Text("💻 컴퓨터 로그인 승인", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("컴퓨터(웹 화면)에서 이 어린이 계정으로 로그인하시겠습니까?\n\n이름: ${preferenceHelper.childName.ifBlank { "무명 어린이" }}", color = Color.DarkGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showApprovedDialog = false
                        val db = FirebaseFirestore.getInstance()
                        db.collection("webSessions").document(scannedSessionId)
                            .update(mapOf(
                                "status" to "approved",
                                "childId" to preferenceHelper.childId,
                                "childName" to preferenceHelper.childName.ifBlank { "무명 어린이" }
                            ))
                            .addOnSuccessListener {
                                Toast.makeText(context, "컴퓨터 로그인이 승인되었습니다! 🎉", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "승인 처리 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    }
                ) {
                    Text("승인하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApprovedDialog = false }) {
                    Text("취소")
                }
            }
        )
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
