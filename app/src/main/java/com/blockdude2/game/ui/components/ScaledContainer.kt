package com.blockdude2.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Reference dimensions (design target)
private const val REF_WIDTH = 360f
private const val REF_HEIGHT = 640f

// CompositionLocal to provide scale throughout the tree
val LocalScale = staticCompositionLocalOf { 1f }

@Composable
fun ScaledContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Calculate scale based on available space while maintaining aspect ratio
        val aspectRatio = REF_WIDTH / REF_HEIGHT
        val screenAspect = maxWidth / maxHeight

        val (containerWidth, containerHeight) = if (screenAspect > aspectRatio) {
            // Screen is wider than our aspect ratio - fit to height
            val h = maxHeight
            val w = h * aspectRatio
            w to h
        } else {
            // Screen is taller than our aspect ratio - fit to width
            val w = maxWidth
            val h = w / aspectRatio
            w to h
        }

        val scale = containerWidth.value / REF_WIDTH

        CompositionLocalProvider(LocalScale provides scale) {
            Box(
                modifier = Modifier
                    .width(containerWidth)
                    .height(containerHeight)
            ) {
                content()
            }
        }
    }
}

// Extension functions for scaled dimensions
@Composable
fun scaledDp(value: Number): Dp {
    return (value.toFloat() * LocalScale.current).dp
}

@Composable
fun scaledSp(value: Number): TextUnit {
    return (value.toFloat() * LocalScale.current).sp
}
