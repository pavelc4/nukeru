package com.nukeru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A highly expressive, custom-drawn linear wavy progress indicator that follows the Material 3 Expressive design.
 */
@Composable
fun WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 4.dp,
    waveLength: Dp = 24.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val amplitudePx = with(density) { amplitude.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "WavyLinearTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyLinearPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(amplitude * 2 + strokeWidth)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val currentProgress = progress().coerceIn(0f, 1f)
        val activeWidth = width * currentProgress

        // 1. Draw inactive track
        if (currentProgress < 1f) {
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(activeWidth, centerY),
                end = androidx.compose.ui.geometry.Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // 2. Draw active wavy track
        if (activeWidth > 0f) {
            val path = Path()
            path.moveTo(0f, centerY)

            // Step through pixels and plot the sine wave
            val step = 2f // calculate every 2 pixels
            var x = 0f
            while (x <= activeWidth) {
                // Fade amplitude to 0 near the start and end of the active track for a natural look
                val startFade = (x / 20f).coerceIn(0f, 1f)
                val endFade = ((activeWidth - x) / 20f).coerceIn(0f, 1f)
                val currentAmplitude = amplitudePx * startFade * endFade

                val y = centerY + currentAmplitude * sin((2 * PI * x / waveLengthPx) - phaseShift).toFloat()
                path.lineTo(x, y)
                x += step
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * A highly expressive, custom-drawn circular wavy progress indicator.
 */
@Composable
fun WavyCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 3.dp,
    numWaves: Int = 12
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val amplitudePx = with(density) { amplitude.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "WavyCircularTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavyCircularPhase"
    )

    Canvas(
        modifier = modifier
            .size(48.dp)
            .padding(amplitude + strokeWidth / 2)
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = (width.coerceAtMost(height) / 2f) - (amplitudePx / 2f)
        val currentProgress = progress().coerceIn(0f, 1f)

        // Draw background track (a full clean circle)
        drawCircle(
            color = trackColor,
            radius = baseRadius,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = Stroke(width = strokeWidthPx)
        )

        // Draw active wavy track (partial or full circle depending on progress)
        if (currentProgress > 0f) {
            val path = Path()
            val totalAngle = 2f * PI * currentProgress
            val stepAngle = PI / 180f // 1 degree steps

            var angle = -PI / 2.0 // Start from the top (-90 degrees)
            val endAngle = angle + totalAngle

            // Set initial point
            val initialRadius = baseRadius + amplitudePx * sin(numWaves * angle - phaseShift)
            val startX = (centerX + initialRadius * kotlin.math.cos(angle)).toFloat()
            val startY = (centerY + initialRadius * sin(angle)).toFloat()
            path.moveTo(startX, startY)

            while (angle <= endAngle) {
                // Calculate wavy radius
                val waveRadius = baseRadius + amplitudePx * sin(numWaves * angle - phaseShift)
                val px = (centerX + waveRadius * kotlin.math.cos(angle)).toFloat()
                val py = (centerY + waveRadius * sin(angle)).toFloat()
                path.lineTo(px, py)
                angle += stepAngle
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}
