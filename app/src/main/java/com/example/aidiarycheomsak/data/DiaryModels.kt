package com.example.aidiarycheomsak.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GeminiDiaryResponse(
    val feedback: String,
    val spelling_score: Int,
    val expression_score: Int,
    val stamp: String,
    val improved: Boolean = false
)

@Serializable
data class DiaryReport(
    @SerialName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("name") val name: String = "",
    @SerialName("c1") val originalContent: String,
    @SerialName("fb") val originalFeedback: String = "",
    @SerialName("c2") val rewrittenContent: String = "",
    @SerialName("s1") val firstSpellingScore: Int = 0,
    @SerialName("e1") val firstExpressionScore: Int = 0,
    @SerialName("s2") val secondSpellingScore: Int = 0,
    @SerialName("e2") val secondExpressionScore: Int = 0,
    @SerialName("st") val stamp: String = "",
    @SerialName("improved") val improved: Boolean = false,
    @SerialName("wpm") val typingSpeed: Int = 0,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("originalLength") val originalLength: Int = 0,
    @SerialName("rewrittenLength") val rewrittenLength: Int = 0
) {
    val displayOriginalLength: Int
        get() = if (originalLength > 0) originalLength else originalContent.length

    val displayRewrittenLength: Int
        get() = if (rewrittenLength > 0) rewrittenLength else rewrittenContent.length
}

