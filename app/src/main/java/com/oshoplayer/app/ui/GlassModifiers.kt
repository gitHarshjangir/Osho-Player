package com.oshoplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GlassShapeRadius = 28.dp
private val EdgeLight = Color.White.copy(alpha = 0.20f)
private val InnerDark = Color.Black

@Stable
fun Modifier.darkGlassSurface(
    isDarkTheme: Boolean = true,
    cornerRadius: Dp = GlassShapeRadius,
    shadowElevation: Dp = 18.dp,
    overlayAlpha: Float = 0.58f,
    borderAlpha: Float = 0.14f
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)

    this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.42f),
            spotColor = Color.Black.copy(alpha = 0.52f)
        )
        .clip(shape)
        .drawWithCache {
            val radius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            val overlay = if (isDarkTheme) InnerDark.copy(alpha = overlayAlpha) else Color.White.copy(alpha = overlayAlpha * 1.2f)
            val border = if (isDarkTheme) EdgeLight.copy(alpha = borderAlpha) else Color.Black.copy(alpha = 0.08f)

            onDrawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.035f),
                            Color.Black.copy(alpha = 0.20f)
                        )
                    ),
                    cornerRadius = radius,
                    size = size
                )
                drawRoundRect(
                    color = overlay,
                    cornerRadius = radius,
                    size = size
                )
                drawRoundRect(
                    color = border,
                    cornerRadius = radius,
                    size = size,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        endY = size.height * 0.22f
                    ),
                    cornerRadius = radius,
                    size = size
                )
            }
        }
        .background(Color.Transparent)
}

@Stable
fun Modifier.edgeLightBorder(
    isDarkTheme: Boolean = true,
    cornerRadius: Dp = GlassShapeRadius,
    alpha: Float = 0.14f
): Modifier = border(
    width = 1.5.dp,
    color = if (isDarkTheme) Color.White.copy(alpha = alpha) else Color.Black.copy(alpha = 0.08f),
    shape = RoundedCornerShape(cornerRadius)
)
