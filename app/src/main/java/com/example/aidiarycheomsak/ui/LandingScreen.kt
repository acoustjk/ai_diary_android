package com.example.aidiarycheomsak.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.aidiarycheomsak.data.PreferenceHelper

@Composable
fun LandingScreen(
    onRoleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEBF8FF), // Light blue
                        Color(0xFFF7FAFC)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "👾 AI고치",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B6CB0),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "AI고치와 함께 글쓰기 실력을 키워보자!",
                fontSize = 16.sp,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Role Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Student Card
                RoleCard(
                    title = "어린이 작가",
                    icon = "👦",
                    description = "일기를 쓰고\nAI고치의 도장을 받아봐!",
                    backgroundColor = Color(0xFFEDF2F7),
                    borderColor = Color(0xFF3182CE),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        preferenceHelper.role = "student"
                        onRoleSelected("student")
                    }
                )

                // Parent Card
                RoleCard(
                    title = "부모님",
                    icon = "👩",
                    description = "성장 보고서를 보고\n칭찬 답장을 보내요!",
                    backgroundColor = Color(0xFFFFF5F5),
                    borderColor = Color(0xFFE53E3E),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        preferenceHelper.role = "parent"
                        onRoleSelected("parent")
                    }
                )
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    icon: String,
    description: String,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .height(220.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
