package me.bogle.geomock.util

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

object AnimationUtils {

    val fadeInAndOutInfinitelyAsFloat: Float
        @Composable
        get() {
            val infiniteTransition = rememberInfiniteTransition(label = "infinite")
            val alpha by infiniteTransition.animateFloat(
                initialValue = .5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750),
                    repeatMode = RepeatMode.Reverse
                ),
                label = ""
            )

            return alpha
        }
}