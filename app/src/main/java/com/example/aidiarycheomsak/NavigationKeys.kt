package com.example.aidiarycheomsak

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Landing : NavKey
@Serializable data object StudentHome : NavKey
@Serializable data class StudentResult(
    val originalContent: String,
    val feedback: String,
    val spellingScore: Int,
    val expressionScore: Int,
    val stamp: String,
    val hasBonus: Boolean,
    val rewrittenContent: String = "",
    val firstSpellingScore: Int = -1,
    val firstExpressionScore: Int = -1
) : NavKey
@Serializable data object ParentHome : NavKey
@Serializable data class ParentDetail(val reportId: String) : NavKey
@Serializable data object Settings : NavKey
