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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle

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

    var showAddChildDialog by remember { mutableStateOf(false) }
    var childPairingCodeInput by remember { mutableStateOf("") }
    var parentNicknameInput by remember { mutableStateOf("") }
    var isAddingChild by remember { mutableStateOf(false) }

    var pairedChildIds by remember { mutableStateOf(preferenceHelper.pairedChildIds) }
    var childrenMap by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    LaunchedEffect(pairedChildIds) {
        pairedChildIds.forEach { childId ->
            if (childId.isNotEmpty()) {
                FirebaseMessaging.getInstance().subscribeToTopic("child_$childId")
            }
        }
    }

    fun refreshReports() {
        reports = preferenceHelper.getSavedReports()
    }

    // 1. Listen to real-time children info updates
    DisposableEffect(pairedChildIds) {
        val listeners = pairedChildIds.map { childId ->
            FirebaseFirestore.getInstance().collection("children").document(childId)
                .addSnapshotListener { snapshot, e ->
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        if (data != null) {
                            childrenMap = childrenMap.toMutableMap().apply {
                                put(childId, data)
                            }
                        }
                    }
                }
        }
        onDispose {
            listeners.forEach { it.remove() }
        }
    }

    // 2. Listen to real-time diaries updates and auto-sync to local report list
    DisposableEffect(pairedChildIds) {
        val listeners = pairedChildIds.map { childId ->
            FirebaseFirestore.getInstance().collection("children").document(childId)
                .collection("diaries")
                .addSnapshotListener { snapshot, e ->
                    if (snapshot != null) {
                        snapshot.documents.forEach { doc ->
                            try {
                                val id = doc.getString("diaryId") ?: doc.id
                                val name = doc.getString("name") ?: "\ubb34\ubb85\u0020\uc5b4\ub9b0\uc774"
                                val timestamp = doc.getLong("timestamp") ?: 0L
                                val originalContent = doc.getString("originalContent") ?: ""
                                val originalFeedback = doc.getString("originalFeedback") ?: ""
                                val rewrittenContent = doc.getString("rewrittenContent") ?: ""
                                val firstSpelling = doc.getLong("firstSpellingScore")?.toInt() ?: 0
                                val firstExpression = doc.getLong("firstExpressionScore")?.toInt() ?: 0
                                val secondSpelling = doc.getLong("secondSpellingScore")?.toInt() ?: 0
                                val secondExpression = doc.getLong("secondExpressionScore")?.toInt() ?: 0
                                val stamp = doc.getString("stamp") ?: ""
                                val improved = doc.getBoolean("improved") ?: false
                                val typingSpeed = doc.getLong("wpm")?.toInt() ?: 0
                                val originalLength = doc.getLong("originalLength")?.toInt() ?: 0
                                val rewrittenLength = doc.getLong("rewrittenLength")?.toInt() ?: 0

                                val report = DiaryReport(
                                    id = id,
                                    name = name,
                                    timestamp = timestamp,
                                    originalContent = originalContent,
                                    originalFeedback = originalFeedback,
                                    rewrittenContent = rewrittenContent,
                                    firstSpellingScore = firstSpelling,
                                    firstExpressionScore = firstExpression,
                                    secondSpellingScore = secondSpelling,
                                    secondExpressionScore = secondExpression,
                                    stamp = stamp,
                                    improved = improved,
                                    typingSpeed = typingSpeed,
                                    originalLength = originalLength,
                                    rewrittenLength = rewrittenLength
                                )
                                preferenceHelper.saveReport(report)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        refreshReports()
                    }
                }
        }
        onDispose {
            listeners.forEach { it.remove() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\ud68c\uc6d0\u0020\uc131\uc7a5\u0020\ubcf4\uace0\uc11c", fontWeight = FontWeight.Bold) },
                actions = {
                    Button(
                        onClick = { showAddChildDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF48BB78)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("\uc790\ub140\u0020\ucd94\uac00\u0020\ud83d\udd17", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFE2E8F0))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\ud83d\udce2\u0020\u005b\uac2d\uace0\u005d\u0020\u0047\u006f\u006f\u0067\u006c\u0065\u0020\u004d\u006f\u0062\u0069\u006c\u0065\u0020\u0041\u0064\u0073\u0020\u0028\u0041\u0064\u004d\u006f\u0062\u0020\ubc30\ub108\u0029",
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
            // --- Paired Children List Section ---
            if (pairedChildIds.isNotEmpty()) {
                Text(
                    text = "\ud83d\udc67\u0020\uc5f0\ub3d9\ub41c\u0020\uc790\ub140\u0020\ubaa9\ub85d",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pairedChildIds.forEach { childId ->
                        val childData = childrenMap[childId]
                        val childName = childData?.get("childName") as? String ?: "불러오는 중..."
                        val creditsVal = childData?.get("credits") as? Long ?: 0L
                        val myRole = preferenceHelper.getChildRole(childId)
                        val myRoleName = if (myRole == "main") "주보호자" else "서브보호자"
                        
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = childName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (myRole == "main") Color(0xFFEBF8FF) else Color(0xFFEDF2F7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = myRoleName, 
                                                fontSize = 10.sp, 
                                                fontWeight = FontWeight.Bold,
                                                color = if (myRole == "main") Color(0xFF2B6CB0) else Color(0xFF4A5568)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "남은 마법이슬: ${creditsVal}개", fontSize = 12.sp, color = Color(0xFF718096))
                                }
                                
                                if (myRole == "main") {
                                    Button(
                                        onClick = {
                                            FirebaseFirestore.getInstance().collection("children").document(childId)
                                                .update("credits", creditsVal + 10)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "${childName}의 마법이슬이 10개 충전되었습니다! 🪙", Toast.LENGTH_SHORT).show()
                                                }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7791F)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("🪙 추가 마법이슬", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "\uc11c\ube0c\ubcf4\ud638\uc790\u0020\u0028\uad8c\ud55c\u0020\uc5c6\uc74c\u0029", 
                                        fontSize = 11.sp, 
                                        color = Color(0xFFE53E3E),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = Color(0xFFE2E8F0))
            }

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

    // Add Child (Pairing) Dialog
    if (showAddChildDialog) {
        AlertDialog(
            onDismissRequest = { showAddChildDialog = false },
            title = { Text("\uc790\ub140\u0020\uc5f0\ub3d9\u0020\ucd94\uac00", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\uc544\uc774\uac05\u0020\ubc1c\uae09\ubc1b\uc740\u0020\u0036\uc790\ub9ac\u0020\uc5f0\uacb0\u0020\ucf54\ub4dc\ub97c\u0020\uc785\ub825\ud574\u0020\uc8fc\uc138\uc694\u002e\u0020\ucd5c\ucd18\u0020\ub4f1\ub85d\u0020\uc2dc\u0020\ucd94\ubcf4\ud638\uc790\ub85c\u0020\uc5f0\ub3d9\ub418\uba70\u002c\u0020\uc774\ud6c4\u0020\ucd94\uac00\u0020\ub4f1\ub85d\u0020\uc2dc\u0020\uc11c\ube0c\ubcf4\ud638\uc790\u0028\ud06c\ub808\ub5a7\u0020\uad8c\ud55c\u0020\uc5c6\uc74c\u0029\ub85c\u0020\uc5f0\ub3d9\ub429\ub2c8\ub2e4\u002e")
                    
                    OutlinedTextField(
                        value = parentNicknameInput,
                        onValueChange = { parentNicknameInput = it },
                        placeholder = { Text("예: 엄마, 아빠, 삼촌") },
                        label = { Text("\ubcf4\ud638\uc790\u0020\ud638\uce6d\u0020\u0028\uc774\ub98d\u0029") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = childPairingCodeInput,
                        onValueChange = { childPairingCodeInput = it },
                        placeholder = { Text("6자리 숫자 코드 입력") },
                        label = { Text("\uc5f0\uacb0\u0020\ucf54\ub4dc") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanCode = childPairingCodeInput.trim()
                        val nickname = parentNicknameInput.trim()
                        if (nickname.isEmpty()) {
                            Toast.makeText(context, "\ubcf4\ud638\uc790\u0020\uc774\ub98d\uc744\u0020\uc785\ub825\ud574\uc8fc\uc138\uc694\u002e", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (cleanCode.length != 6 || cleanCode.toIntOrNull() == null) {
                            Toast.makeText(context, "\uc62c\ubc14\ub978\u0020\u0036\uc790\ub9ac\u0020\uc22b\uc790\ub97c\u0020\uc785\ub825\ud574\uc8fc\uc138\uc694\u002e", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isAddingChild = true
                        val db = FirebaseFirestore.getInstance()
                        db.collection("children")
                            .whereEqualTo("pairingCode", cleanCode)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                isAddingChild = false
                                if (querySnapshot.isEmpty) {
                                    Toast.makeText(context, "\uc77c\uce58\ud558\ub294\u0020\uc5f0\uacb0\u0020\ucf54\ub4dc\uac00\u0020\uc5c6\uc2b5\ub2c8\ub2e4\u002e", Toast.LENGTH_LONG).show()
                                } else {
                                    val doc = querySnapshot.documents[0]
                                    val expires = doc.getLong("pairingCodeExpires") ?: 0L
                                    if (expires < System.currentTimeMillis() / 1000) {
                                        Toast.makeText(context, "\ub9cc\ub8cc\ub41c\u0020\uc5f0\uacb0\u0020\ucf54\ub4dc\uc785\ub2c8\ub2e4\u002e", Toast.LENGTH_LONG).show()
                                        return@addOnSuccessListener
                                    }

                                    val childId = doc.id
                                    val childName = doc.getString("childName") ?: "\ubb34\ubb85\u0020\uc5b4\ub9b0\uc774"

                                    val reviewers = doc.get("pairedReviewers") as? List<Map<String, String>> ?: emptyList()
                                    val myRole = if (reviewers.isEmpty()) "main" else "sub"

                                    val newReviewer = mapOf(
                                        "name" to nickname,
                                        "fcmToken" to "",
                                        "role" to myRole
                                    )

                                    db.collection("children").document(childId)
                                        .update("pairedReviewers", FieldValue.arrayUnion(newReviewer))
                                        .addOnSuccessListener {
                                            FirebaseMessaging.getInstance().subscribeToTopic("child_$childId")
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        Toast.makeText(context, "\uc2e4\uc2dc\uac04\u0020\uc54c\ub9bc\u0020\uad6c\ub3c5\u0020\uc131\uacf5\u0021", Toast.LENGTH_SHORT).show()
                                                    }
                                                }

                                            val currentIds = preferenceHelper.pairedChildIds.toMutableSet()
                                            currentIds.add(childId)
                                            preferenceHelper.pairedChildIds = currentIds
                                            preferenceHelper.setChildRole(childId, myRole)

                                            pairedChildIds = currentIds

                                            showAddChildDialog = false
                                            childPairingCodeInput = ""
                                            parentNicknameInput = ""
                                            Toast.makeText(context, "${childName}\u0020\uc790\ub140\u0020\uc5f0\ub3d9\u0020\uc131\uacf5\u0021\u0020\u0028${myRole}\u0029", Toast.LENGTH_LONG).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "\ubcf4\ud638\uc790\u0020\ub4f1\ub85d\u0020\uc2e4\ud328: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                isAddingChild = false
                                Toast.makeText(context, "\uc790\ub140\u0020\uc870\ud68c\u0020\uc2e4\ud328: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    },
                    enabled = !isAddingChild
                ) {
                    if (isAddingChild) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("\uc5f0\ub3d9\ud558\uae30")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChildDialog = false }, enabled = !isAddingChild) {
                    Text("\ucde8\uc18c")
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
