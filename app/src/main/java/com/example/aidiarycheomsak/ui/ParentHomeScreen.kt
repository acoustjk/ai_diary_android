package com.example.aidiarycheomsak.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidiarycheomsak.data.CompressionHelper
import com.example.aidiarycheomsak.data.DiaryReport
import com.example.aidiarycheomsak.data.PreferenceHelper
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = remember { PreferenceHelper(context) }
    var reports by remember { mutableStateOf(preferenceHelper.getSavedReports()) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    // Parse deep-link parameters if any are stored or passed in (Mocked trigger)
    // We will provide a helper dialogue to manually paste links if deep linking fails.
    
    fun refreshReports() {
        reports = preferenceHelper.getSavedReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👩‍👦 부모님 성장 보고서", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "보고서 링크 추가")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7FAFC))
            )
        },
        bottomBar = {
            // Requirement 2: Ads on Parent mode, none on child mode.
            // AdMob Mock Banner
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
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAFC))
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "받은 성장 보고서",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )

            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 받은 성장 보고서가 없습니다.\n우측 상단의 '+' 버튼을 눌러 링크를 직접 붙여넣거나\n자녀가 공유한 딥링크를 클릭해 앱을 열어보세요!",
                        color = Color(0xFF718096),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        ReportCard(
                            report = report,
                            onClick = { onNavigateToDetail(report.id) },
                            onDelete = {
                                preferenceHelper.deleteReport(report.id)
                                refreshReports()
                                Toast.makeText(context, "보고서가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Import Link Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("보고서 링크 직접 등록", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("아이가 카카오톡이나 메일로 공유한 전체 링크를 복사하여 아래에 붙여넣어 주세요.")
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("cheomsak://report?data=...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val queryIndicator = "?data="
                        val dataPart = if (importText.contains(queryIndicator)) {
                            importText.substringAfter(queryIndicator)
                        } else {
                            importText
                        }

                        val decodedJson = CompressionHelper.decompress(dataPart.trim())
                        if (decodedJson != null) {
                            try {
                                val report = Json.decodeFromString<DiaryReport>(decodedJson)
                                preferenceHelper.saveReport(report)
                                refreshReports()
                                showImportDialog = false
                                importText = ""
                                Toast.makeText(context, "성장 보고서가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "데이터 해석 실패: 잘못된 형식입니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "링크의 데이터 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("등록하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun ReportCard(
    report: DiaryReport,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(report.timestamp) {
        SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault()).format(Date(report.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👦 ${report.name} 어린이의 일기 보고서",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF2D3748)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color(0xFF718096)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "맞춤법: ${report.firstSpellingScore} ➡️ ${report.secondSpellingScore}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3182CE)
                    )
                    Text(
                        text = "표현력: ${report.firstExpressionScore} ➡️ ${report.secondExpressionScore}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF48BB78)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color(0xFFE53E3E)
                )
            }
        }
    }
}
