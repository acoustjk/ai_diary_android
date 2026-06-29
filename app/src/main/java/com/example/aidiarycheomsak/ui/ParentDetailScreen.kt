package com.example.aidiarycheomsak.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidiarycheomsak.data.PreferenceHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDetailScreen(
    reportId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    val report = remember(reportId) {
        preferenceHelper.getSavedReports().firstOrNull { it.id == reportId }
    }

    var replyText by remember { mutableStateOf("") }

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("보고서를 찾을 수 없습니다.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 ${report.name}의 성장 보고서", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7FAFC))
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7FAFC)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔒 아이용 앱에는 광고가 없습니다.",
                    color = Color(0xFF718096),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFFE2E8F0))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📢 [광고] Google Mobile Ads (AdMob 배너)",
                        color = Color(0xFF4A5568),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Score comparisons card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF8FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // First Row: Spelling & Expression
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ScoreChangeItem(
                                title = "✨ 맞춤법 점수",
                                before = report.firstSpellingScore,
                                after = report.secondSpellingScore,
                                suffix = "점"
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = Color(0xFFBEE3F8)
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ScoreChangeItem(
                                title = "💡 표현력 점수",
                                before = report.firstExpressionScore,
                                after = report.secondExpressionScore,
                                suffix = "점"
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = Color(0xFFBEE3F8)
                    )

                    // Second Row: Character Count & Typing Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ScoreChangeItem(
                                title = "📝 글자 수",
                                before = report.displayOriginalLength,
                                after = report.displayRewrittenLength,
                                suffix = "자"
                            )
                        }
                        if (report.typingSpeed > 0) {
                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp),
                                color = Color(0xFFBEE3F8)
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "⏱️ 평균 타수",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3182CE)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${report.typingSpeed}타/분",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2D3748)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 2. Original Diary Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✍️ 처음 쓴 일기",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF718096),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = report.originalContent,
                        fontSize = 14.sp,
                        color = Color(0xFF4A5568),
                        lineHeight = 20.sp
                    )
                }
            }

            // 3. AI Teacher Feedback Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFADF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👾 AI고치의 다정한 피드백",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDD6B20),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = report.originalFeedback,
                        fontSize = 13.sp,
                        color = Color(0xFF7B341E),
                        lineHeight = 18.sp
                    )
                }
            }

            // 4. Rewritten Diary Card
            if (report.rewrittenContent.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color(0xFFB7791F)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "👑 스스로 고쳐서 다시 쓴 일기",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB7791F),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = report.rewrittenContent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3748),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 5. Parent Reply Box
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💬 아이에게 칭찬 한마디 답장 보내기",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("예) 우리 ${report.name} 정말 대단해! 스스로 일기를 고쳐 쓰다니 멋지다~ 오늘 맛있는 거 먹자!") },
                        textStyle = TextStyle(color = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            if (replyText.isBlank()) {
                                Toast.makeText(context, "답장을 입력해주세요.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Share text back to child
                            val replyMsg = "💌 [보호자님의 칭찬 편지]\n$replyText"
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, replyMsg)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "아이에게 칭찬 전달하기")
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "전송")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("카톡으로 칭찬 메시지 답장 보내기")
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreChangeItem(
    title: String,
    before: Int,
    after: Int,
    suffix: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3182CE)
        )
        Spacer(modifier = Modifier.height(4.dp))
        val displayText = if (after == 0) {
            "$before$suffix"
        } else {
            val diff = after - before
            val diffText = if (diff > 0) " (+${diff}) 🎉" else if (diff < 0) " (${diff})" else " (동일)"
            "$before$suffix ➡️ $after$suffix$diffText"
        }
        Text(
            text = displayText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3748)
        )
    }
}
