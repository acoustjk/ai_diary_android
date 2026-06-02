package com.example.aidiarycheomsak.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_diary_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var role: String
        get() = prefs.getString("user_role", "") ?: ""
        set(value) = prefs.edit().putString("user_role", value).apply()

    var serverUrl: String
        get() = prefs.getString("server_url", "https://ai-diary-cheomsak.onrender.com") ?: "https://ai-diary-cheomsak.onrender.com"
        set(value) = prefs.edit().putString("server_url", value).apply()

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var streakCount: Int
        get() = prefs.getInt("streak_count", 0)
        set(value) = prefs.edit().putInt("streak_count", value).apply()

    var lastWriteDate: String
        get() = prefs.getString("last_write_date", "") ?: ""
        set(value) = prefs.edit().putString("last_write_date", value).apply()

    var isRewriteModePending: Boolean
        get() = prefs.getBoolean("is_rewrite_mode_pending", false)
        set(value) = prefs.edit().putBoolean("is_rewrite_mode_pending", value).apply()

    var pendingOriginalContent: String
        get() = prefs.getString("pending_original_content", "") ?: ""
        set(value) = prefs.edit().putString("pending_original_content", value).apply()

    var pendingOriginalFeedback: String
        get() = prefs.getString("pending_original_feedback", "") ?: ""
        set(value) = prefs.edit().putString("pending_original_feedback", value).apply()

    var pendingSpellingScore: Int
        get() = prefs.getInt("pending_spelling_score", 0)
        set(value) = prefs.edit().putInt("pending_spelling_score", value).apply()

    var pendingExpressionScore: Int
        get() = prefs.getInt("pending_expression_score", 0)
        set(value) = prefs.edit().putInt("pending_expression_score", value).apply()

    var clearDiaryTextPending: Boolean
        get() = prefs.getBoolean("clear_diary_text_pending", false)
        set(value) = prefs.edit().putBoolean("clear_diary_text_pending", value).apply()

    var childName: String
        get() = prefs.getString("child_name", "") ?: ""
        set(value) = prefs.edit().putString("child_name", value).apply()

    var draftDiaryText: String
        get() = prefs.getString("draft_diary_text", "") ?: ""
        set(value) = prefs.edit().putString("draft_diary_text", value).apply()

    var draftIsRewriteMode: Boolean
        get() = prefs.getBoolean("draft_is_rewrite_mode", false)
        set(value) = prefs.edit().putBoolean("draft_is_rewrite_mode", value).apply()

    var draftOriginalContent: String
        get() = prefs.getString("draft_original_content", "") ?: ""
        set(value) = prefs.edit().putString("draft_original_content", value).apply()

    var draftOriginalFeedback: String
        get() = prefs.getString("draft_original_feedback", "") ?: ""
        set(value) = prefs.edit().putString("draft_original_feedback", value).apply()

    var draftPrevSpelling: Int
        get() = prefs.getInt("draft_prev_spelling", 0)
        set(value) = prefs.edit().putInt("draft_prev_spelling", value).apply()

    var draftPrevExpression: Int
        get() = prefs.getInt("draft_prev_expression", 0)
        set(value) = prefs.edit().putInt("draft_prev_expression", value).apply()

    fun getSavedReports(): List<DiaryReport> {
        val jsonStr = prefs.getString("saved_reports", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<DiaryReport>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReport(report: DiaryReport) {
        val current = getSavedReports().toMutableList()
        current.removeAll { it.id == report.id }
        current.add(0, report) // Newest first
        val jsonStr = json.encodeToString(current)
        prefs.edit().putString("saved_reports", jsonStr).apply()
    }

    fun deleteReport(id: String) {
        val current = getSavedReports().toMutableList()
        current.removeAll { it.id == id }
        val jsonStr = json.encodeToString(current)
        prefs.edit().putString("saved_reports", jsonStr).apply()
    }
}
