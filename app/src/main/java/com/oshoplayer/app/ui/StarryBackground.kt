package com.oshoplayer.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun StarryBackground(modifier: Modifier = Modifier) {
    val stars = remember { List(100) { Star() } }
    val infiniteTransition = rememberInfiniteTransition(label = "time")
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.oshoplayer.app.R.drawable.bgmode),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alpha = 0.40f,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            stars.forEach { star ->
                val brightness = (kotlin.math.sin(time * star.speed + star.phase) + 1f) / 2f
                val currentAlpha = (star.baseAlpha * brightness).coerceIn(0.05f, 0.85f)
                drawCircle(
                    color = Color.White.copy(alpha = currentAlpha),
                    radius = star.radius,
                    center = Offset(star.x * w, star.y * h)
                )
            }
        }
    }
}

private class Star {
    val x = Random.nextFloat()
    val y = Random.nextFloat()
    val radius = Random.nextFloat() * 2.5f + 0.5f 
    val phase = Random.nextFloat() * (Math.PI.toFloat() * 2f)
    val speed = Random.nextFloat() * 0.5f + 0.1f
    val baseAlpha = Random.nextFloat() * 0.5f + 0.5f
}
