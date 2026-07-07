package com.aichat.app.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    particleCount: Int = 30,
    particleSize: Dp = Dp(3f)
) {
    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(Unit) {
        val sizePx = with(density) { particleSize.toPx() }
        repeat(particleCount) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * 1000,
                    y = Random.nextFloat() * 2000,
                    vx = (Random.nextFloat() - 0.5f) * 0.3f,
                    vy = (Random.nextFloat()) * 0.5f + 0.2f,
                    radius = sizePx * (Random.nextFloat() * 0.5f + 0.5f),
                    alpha = Random.nextFloat() * 0.5f + 0.2f,
                    color = Color(0xFF4A90D9)
                )
            )
        }

        while (true) {
            delay(16)
            particles.forEachIndexed { index, particle ->
                particle.x += particle.vx
                particle.y += particle.vy

                if (particle.y > 2000) {
                    particle.y = -10f
                    particle.x = Random.nextFloat() * 1000
                }
                if (particle.x < -10) particle.x = 1000f
                if (particle.x > 1000) particle.x = -10f

                particles[index] = particle.copy()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            val x = (particle.x / 1000f) * width
            val y = (particle.y / 2000f) * height

            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )

            particles.forEach { other ->
                if (other !== particle) {
                    val otherX = (other.x / 1000f) * width
                    val otherY = (other.y / 2000f) * height
                    val distance = sqrt(
                        (x - otherX) * (x - otherX) +
                            (y - otherY) * (y - otherY)
                    )

                    if (distance < 150) {
                        drawLine(
                            color = particle.color.copy(alpha = (1 - distance / 150) * 0.15f),
                            start = androidx.compose.ui.geometry.Offset(x, y),
                            end = androidx.compose.ui.geometry.Offset(otherX, otherY),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}
