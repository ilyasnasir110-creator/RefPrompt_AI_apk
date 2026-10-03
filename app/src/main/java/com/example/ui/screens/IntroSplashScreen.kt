package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.VioletNeon
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun IntroSplashScreen(
    onFinished: () -> Unit,
    isReturningUser: Boolean = false
) {
    var step by remember { mutableStateOf(0) }
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_rotate")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Particle nodes for cinematic AI ambiance
    val particleOffsets = remember {
        List(28) {
            Triple(
                Random.nextFloat(), // x ratio
                Random.nextFloat(), // y ratio
                Random.nextFloat() * 3f + 1.5f // radius
            )
        }
    }

    LaunchedEffect(Unit) {
        val baseDelay = if (isReturningUser) 250L else 450L

        // Step 1: Glowing logo emerges
        step = 1
        logoAlpha.animateTo(1f, animationSpec = tween((baseDelay * 1.5).toInt()))
        logoScale.animateTo(1f, animationSpec = tween((baseDelay * 1.8).toInt(), easing = FastOutSlowInEasing))

        // Step 2: Title fades and scales in
        step = 2
        delay(baseDelay / 2)
        titleAlpha.animateTo(1f, animationSpec = tween(baseDelay.toInt()))

        // Step 3: Tagline fades in
        step = 3
        delay(baseDelay / 2)
        taglineAlpha.animateTo(1f, animationSpec = tween(baseDelay.toInt()))

        // Step 4: Finish and smoothly transition
        delay(if (isReturningUser) 800L else 1400L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap to skip
                onFinished()
            }
            .testTag("intro_splash_screen")
    ) {
        // Floating subtle particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            particleOffsets.forEachIndexed { i, (rx, ry, radius) ->
                val x = (rx * width + cos((rotationAngle + i * 20) * Math.PI / 180).toFloat() * 18f)
                    .coerceIn(0f, width)
                val y = (ry * height + sin((rotationAngle + i * 20) * Math.PI / 180).toFloat() * 18f)
                    .coerceIn(0f, height)
                val color = if (i % 2 == 0) CyanNeon else VioletNeon
                drawCircle(
                    color = color.copy(alpha = 0.25f + (i % 5) * 0.08f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Top right Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, end = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier.testTag("skip_intro_button")
            ) {
                Text(
                    text = "SKIP",
                    color = CyanNeon.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Center Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing AI Ring & Icon
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing pulse ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyanNeon.copy(alpha = 0.35f),
                                VioletNeon.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        radius = (size.minDimension / 2f) * pulseScale
                    )
                    drawCircle(
                        color = CyanNeon.copy(alpha = 0.4f),
                        radius = (size.minDimension / 2f) * (pulseScale * 0.9f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Inner core with Logo image
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF101524))
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(listOf(CyanNeon, VioletNeon, CyanNeon)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_refprompt_logo_1790792415476),
                        contentDescription = "RefPrompt AI Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name with gradient shimmer
            Text(
                text = "RefPrompt AI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .testTag("intro_app_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline
            Text(
                text = "Turn Any Image Into a Perfect AI Prompt.",
                color = CyanNeon,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .testTag("intro_app_tagline")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subtle indicator dots
            Row(
                modifier = Modifier.alpha(taglineAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 1) CyanNeon else Color(0xFF334155)
                            )
                    )
                }
            }
        }
    }
}
