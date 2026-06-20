package com.example.aidiarycheomsak.ui

import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
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
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
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
            // 🔗 보호자/교사 기기 연결 설정 카드
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
                        text = "🔗 보호자/교사 연결 (페어링)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )

                    Text(
                        text = "아래에서 생성한 6자리 인증 코드를 보호자 또는 선생님의 스마트폰 앱에 입력하여 일기 첨삭 내용을 함께 볼 수 있습니다. (다대다 연결 지원)",
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
                            text = "아직 연결된 보호자가 없습니다. 페어링 코드를 보호자님께 공유해 주세요.",
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
                        text = "📄 약관 및 라이선스",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(
                        text = "이용약관",
                        color = Color(0xFF3182CE),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val serverUrl = preferenceHelper.serverUrl.ifBlank { "https://ai-diary-cheomsak.onrender.com" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${serverUrl.trim().removeSuffix("/")}/terms"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp)
                    )

                    Text(
                        text = "개인정보 처리방침",
                        color = Color(0xFF3182CE),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val serverUrl = preferenceHelper.serverUrl.ifBlank { "https://ai-diary-cheomsak.onrender.com" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${serverUrl.trim().removeSuffix("/")}/privacy"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp)
                    )

                    Text(
                        text = "오픈소스 라이선스 고지",
                        color = Color(0xFF3182CE),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val serverUrl = preferenceHelper.serverUrl.ifBlank { "https://ai-diary-cheomsak.onrender.com" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${serverUrl.trim().removeSuffix("/")}/licenses"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            // ⚠️ 계정 및 데이터 관리 카드 (탈퇴/삭제)
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
                        text = "⚠️ 계정 및 데이터 관리",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(
                        text = "기기에 저장된 모든 일기 기록과 서버의 자녀 프로필 데이터가 완전히 삭제되며 복구할 수 없습니다.",
                        fontSize = 13.sp,
                        color = Color(0xFFE53E3E),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = "※ 주의: 데이터 삭제 완료 시 기존에 자녀 프로필에 충전되어 있던 모든 마법이슬도 함께 영구 소멸되며, 환불이나 복구가 불가능합니다.",
                        fontSize = 12.sp,
                        color = Color(0xFF718096),
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("초기화 및 모든 데이터 삭제", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("⚠️ 전체 데이터 삭제 확인", fontWeight = FontWeight.Bold) },
            text = { 
                Text("정말로 모든 데이터를 삭제하고 탈퇴하시겠습니까?\n\n이 작업은 되돌릴 수 없으며, 보유 중인 모든 마법이슬(크레딧)과 자녀 정보, 연동 기록이 영구적으로 소멸됩니다.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        isDeleting = true
                        
                        val db = FirebaseFirestore.getInstance()
                        val childId = preferenceHelper.childId
                        
                        db.collection("children").document(childId).delete()
                            .addOnSuccessListener {
                                context.getSharedPreferences("ai_diary_prefs", Context.MODE_PRIVATE)
                                    .edit().clear().apply()
                                Toast.makeText(context, "모든 데이터가 삭제되고 초기화되었습니다.", Toast.LENGTH_LONG).show()
                                isDeleting = false
                                onRoleChanged()
                            }
                            .addOnFailureListener { e ->
                                // Fallback: clear local preferences to prevent getting locked out
                                context.getSharedPreferences("ai_diary_prefs", Context.MODE_PRIVATE)
                                    .edit().clear().apply()
                                Toast.makeText(context, "서버 데이터 삭제 실패: ${e.localizedMessage}. 로컬 초기화만 진행합니다.", Toast.LENGTH_LONG).show()
                                isDeleting = false
                                onRoleChanged()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
                ) {
                    Text("삭제하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}
