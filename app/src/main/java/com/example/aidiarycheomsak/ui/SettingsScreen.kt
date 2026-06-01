package com.example.aidiarycheomsak.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidiarycheomsak.data.GeminiService
import com.example.aidiarycheomsak.data.PreferenceHelper
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
                .padding(20.dp),
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
                                            content = "안녕"
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


        }
    }
}
