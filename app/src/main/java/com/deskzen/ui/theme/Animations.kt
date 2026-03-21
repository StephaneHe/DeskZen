package com.deskzen.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically

object DeskZenAnimations {
    // Durées
    val durationQuick = 150
    val durationNormal = 250
    val durationMedium = 350
    val durationSlow = 500

    // Easing
    val easeOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val easeInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Transitions nommées
    val itemAppear = fadeIn(tween(durationNormal, easing = easeOut)) +
            scaleIn(tween(durationNormal, easing = easeOut), initialScale = 0.92f)

    val itemDisappear = fadeOut(tween(durationQuick)) +
            scaleOut(tween(durationQuick), targetScale = 0.92f)

    val folderOpen = expandVertically(tween(durationMedium, easing = easeInOut))
    val folderClose = shrinkVertically(tween(durationNormal, easing = easeOut))
}
