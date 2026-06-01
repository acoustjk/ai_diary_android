package com.example.aidiarycheomsak

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.aidiarycheomsak.data.PreferenceHelper
import com.example.aidiarycheomsak.ui.*

@Composable
fun MainNavigation(initialReportId: State<String?>) {
  val context = LocalContext.current
  val preferenceHelper = PreferenceHelper(context)

  val startDestination = StudentHome

  val backStack = rememberNavBackStack(startDestination)

  val reportId = initialReportId.value
  LaunchedEffect(reportId) {
      if (reportId != null) {
          backStack.add(ParentDetail(reportId))
      }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
        entry<Landing> {
            LandingScreen(
                onRoleSelected = { role ->
                    backStack.removeLastOrNull()
                    if (role == "student") {
                        backStack.add(StudentHome)
                    } else {
                        backStack.add(ParentHome)
                    }
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
        entry<StudentHome> {
            StudentHomeScreen(
                onNavigateToResult = { content, feedback, spelling, expression, stamp, hasBonus, rewrittenContent, firstSpelling, firstExpression ->
                    backStack.add(
                        StudentResult(
                            originalContent = content,
                            feedback = feedback,
                            spellingScore = spelling,
                            expressionScore = expression,
                            stamp = stamp,
                            hasBonus = hasBonus,
                            rewrittenContent = rewrittenContent,
                            firstSpellingScore = firstSpelling,
                            firstExpressionScore = firstExpression
                        )
                    )
                },
                onNavigateToSettings = {
                    backStack.add(Settings)
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
        entry<StudentResult> { key ->
            StudentResultScreen(
                originalContent = key.originalContent,
                feedback = key.feedback,
                spellingScore = key.spellingScore,
                expressionScore = key.expressionScore,
                stamp = key.stamp,
                hasBonus = key.hasBonus,
                rewrittenContent = key.rewrittenContent,
                firstSpellingScore = key.firstSpellingScore,
                firstExpressionScore = key.firstExpressionScore,
                onBack = {
                    backStack.removeLastOrNull()
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
        entry<ParentHome> {
            ParentHomeScreen(
                onNavigateToDetail = { rId ->
                    backStack.add(ParentDetail(rId))
                },
                onNavigateToSettings = {
                    backStack.add(Settings)
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
        entry<ParentDetail> { key ->
            ParentDetailScreen(
                reportId = key.reportId,
                onBack = {
                    backStack.removeLastOrNull()
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
        entry<Settings> {
            SettingsScreen(
                onBack = {
                    backStack.removeLastOrNull()
                },
                onRoleChanged = {
                    backStack.removeLastOrNull()
                    backStack.add(Landing)
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
    }
  )
}
