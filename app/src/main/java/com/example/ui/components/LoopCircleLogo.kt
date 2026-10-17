package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoopCircleLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    animated: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loop_rotation")
    val angle by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val strokeWidth = canvasSize * 0.12f
            val radius = (canvasSize - strokeWidth) / 2f

            // Outer subtle track
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Main circular loop ring
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    primaryColor,
                    accentColor,
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor
                ),
                center = center
            )

            drawCircle(
                brush = gradientBrush,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Orbiting accent dot on the circle loop
            val rad = Math.toRadians((angle - 90.0)).toFloat()
            val dotX = center.x + radius * cos(rad)
            val dotY = center.y + radius * sin(rad)
            drawCircle(
                color = Color.White,
                radius = strokeWidth * 0.7f,
                center = Offset(dotX, dotY)
            )
        }
    }
}
