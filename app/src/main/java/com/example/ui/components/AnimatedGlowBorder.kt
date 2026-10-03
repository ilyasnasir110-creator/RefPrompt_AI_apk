package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.VioletNeon

@Composable
fun AnimatedGlowCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.5.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color(0xFF141A2B), shape)
        ) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glow_angle"
    )

    val glowBrush = Brush.sweepGradient(
        colors = listOf(
            CyanNeon.copy(alpha = 0.8f),
            VioletNeon.copy(alpha = 0.7f),
            Color(0xFF00E5FF).copy(alpha = 0.2f),
            VioletNeon.copy(alpha = 0.9f),
            CyanNeon.copy(alpha = 0.8f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glowBrush)
            .padding(borderWidth)
            .clip(shape)
            .background(Color(0xFF101524))
    ) {
        content()
    }
}
