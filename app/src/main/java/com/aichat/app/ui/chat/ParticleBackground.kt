package com.aichat.app.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var alpha: Float,
    var color: Color
)

@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 26,
    particleSize: Dp = Dp(3f)
) {
    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<Particle>() }

    // 主题感知：粒子从主题主色 + 三色取色，自动适配深浅模式
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(primaryColor, tertiaryColor) {
        if (particles.isEmpty()) {
            val sizePx = with(density) { particleSize.toPx() }
            repeat(particleCount) { i ->
                particles.add(
                    Particle(
                        x = Random.nextFloat() * 1000f,
                        y = Random.nextFloat() * 2000f,
                        vx = (Random.nextFloat() - 0.5f) * 0.25f,
                        vy = (Random.nextFloat()) * 0.45f + 0.18f,
                        radius = sizePx * (Random.nextFloat() * 0.6f + 0.4f),
                        alpha = Random.nextFloat() * 0.45f + 0.15f,
                        color = if (i % 4 == 0) tertiaryColor else primaryColor
                    )
                )
            }
        }

        while (true) {
            delay(32)
            particles.forEachIndexed { index, particle ->
                particle.x += particle.vx
                particle.y += particle.vy

                if (particle.y > 2000f) {
                    particle.y = -10f
                    particle.x = Random.nextFloat() * 1000f
                }
                if (particle.x < -10) particle.x = 1000f
                if (particle.x > 1000f) particle.x = -10f

                particles[index] = particle.copy()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxLinkDistance = (minOf(width, height) * 0.22f).coerceAtLeast(80f)

        particles.forEach { particle ->
            val x = (particle.x / 1000f) * width
            val y = (particle.y / 2000f) * height

            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(x, y)
            )

            particles.forEach { other ->
                if (other !== particle) {
                    val otherX = (other.x / 1000f) * width
                    val otherY = (other.y / 2000f) * height
                    val distance = sqrt(
                        (x - otherX) * (x - otherX) +
                            (y - otherY) * (y - otherY)
                    )

                    if (distance < maxLinkDistance) {
                        val linkAlpha = (1 - distance / maxLinkDistance) * 0.12f
                        drawLine(
                            color = particle.color.copy(alpha = linkAlpha),
                            start = Offset(x, y),
                            end = Offset(otherX, otherY),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}
