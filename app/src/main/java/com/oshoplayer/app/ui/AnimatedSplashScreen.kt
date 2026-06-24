package com.oshoplayer.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(onSplashComplete: () -> Unit) {
    // Animation states
    val revealFraction = remember { Animatable(0f) }
    val fadeAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Handwriting / Wipe effect (0 to 1.5s)
        revealFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
        
        // 2. Pause to let the user read it
        delay(500)
        
        // 3. Fade out
        fadeAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
        
        // Transition to main app
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                alpha = fadeAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "अप्प दीपो भव.",
            color = Color(0xFFE6D3A8), // Light gold accent
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .drawWithContent {
                    val width = size.width * revealFraction.value
                    clipRect(right = width) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
    }
}
