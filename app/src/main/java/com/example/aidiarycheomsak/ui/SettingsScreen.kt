package com.example.aidiarycheomsak.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidiarycheomsak.data.GeminiService
import com.example.aidiarycheomsak.data.PreferenceHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRoleChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    var serverUrl by remember { mutableStateOf(preferenceHelper.serverUrl) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7FAFC)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAFC))
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Server URL Settings Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🌐 중앙 백엔드 서버 설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )

                    Text(
                        text = "AI 첨삭 분석을 요청할 서버 주소를 설정합니다. 아이들은 아무런 API Key 발급 없이 즉시 분석을 받을 수 있습니다.\n* 기본 배포 주소: https://ai-diary-cheomsak.onrender.com\n* 로컬 에뮬레이터 테스트: http://10.0.2.2:8000",
                        fontSize = 12.sp,
                        color = Color(0xFF718096),
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("서버 주소 입력") },
                        textStyle = TextStyle(color = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Connection Test
                        Button(
                            onClick = {
                                if (serverUrl.isBlank()) {
                                    Toast.makeText(context, "서버 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isTesting = true
                                    try {
                                        // Simple ping
                                        GeminiService.checkDiary(
                                            serverUrl = serverUrl,
                                            content = "안녕",
                                            apiKey = preferenceHelper.geminiApiKey
                                        )
                                        Toast.makeText(context, "서버 연결 성공! 설정이 저장되었습니다. 🎉", Toast.LENGTH_LONG).show()
                                        // Save
                                        preferenceHelper.serverUrl = serverUrl
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "연결 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = !isTesting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182CE)),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("연결 테스트 및 저장")
                            }
                        }
                    }
                }
            }

            // 🔗 부모님/교사 기기 연결 설정 카드
            var pairingCode by remember { mutableStateOf("") }
            var isGeneratingCode by remember { mutableStateOf(false) }
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

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🔗 부모님/교사 연결 (페어링)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )

                    Text(
                        text = "아래에서 생성한 6자리 인증 코드를 부모님 또는 선생님의 스마트폰 앱에 입력하여 일기 첨삭 내용을 함께 볼 수 있습니다. (다대다 연결 지원)",
                        fontSize = 12.sp,
                        color = Color(0xFF718096),
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    if (pairingCode.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEDF2F7), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("연결 코드 (10분간 유효)", fontSize = 11.sp, color = Color(0xFF718096))
                            Text(
                                text = "${pairingCode.substring(0, 3)} ${pairingCode.substring(3, 6)}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B6CB0),
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isGeneratingCode = true
                                val newCode = (100000..999999).random().toString()
                                val childName = preferenceHelper.childName.ifBlank { "무명 어린이" }
                                val childData = mapOf(
                                    "childId" to preferenceHelper.childId,
                                    "childName" to childName,
                                    "pairingCode" to newCode,
                                    "pairingCodeExpires" to (System.currentTimeMillis() / 1000 + 600) // 10 minutes
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("children")
                                    .document(preferenceHelper.childId)
                                    .set(childData, SetOptions.merge())
                                    .addOnSuccessListener {
                                        pairingCode = newCode
                                        isGeneratingCode = false
                                        Toast.makeText(context, "페어링 코드가 발급되었습니다! 10분간 유효합니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isGeneratingCode = false
                                        Toast.makeText(context, "코드 발급 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        enabled = !isGeneratingCode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF48BB78)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingCode) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("페어링 코드 발급받기", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Connected reviewers list
                    Text(
                        text = "👥 현재 연결된 보호자/선생님 목록",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )

                    if (pairedReviewersList.isEmpty()) {
                        Text(
                            text = "아직 연결된 보호자가 없습니다. 페어링 코드를 부모님께 공유해 주세요.",
                            fontSize = 12.sp,
                            color = Color(0xFF718096)
                        )
                    } else {
                        pairedReviewersList.forEach { reviewer ->
                            val reviewerName = reviewer["name"] ?: "알 수 없음"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF7FAFC), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "✔️ $reviewerName 님 연결됨", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2B6CB0))
                                
                                TextButton(
                                    onClick = {
                                        val db = FirebaseFirestore.getInstance()
                                        val updatedReviewers = pairedReviewersList.toMutableList()
                                        updatedReviewers.remove(reviewer)
                                        
                                        db.collection("children").document(preferenceHelper.childId)
                                            .update("pairedReviewers", updatedReviewers)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "연결을 해제했습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                ) {
                                    Text("연결 해제", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
