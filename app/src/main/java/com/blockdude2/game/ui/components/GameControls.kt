package com.blockdude2.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import com.blockdude2.game.ui.theme.AccentOrange
import com.blockdude2.game.ui.theme.PrimaryBlue
import com.blockdude2.game.ui.theme.SurfaceColor

@Composable
fun GameControls(
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveUp: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = scaledDp(80)
    val buttonOffset = scaledDp(62)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonSize + buttonOffset * 2 + scaledDp(12)),
        contentAlignment = Alignment.Center
    ) {
        // D-Pad cross layout - all buttons same distance apart
        // Up button
        Box(modifier = Modifier.offset(y = -buttonOffset)) {
            ControlButton(
                onClick = onMoveUp,
                color = PrimaryBlue,
                size = buttonSize
            ) {
                ArrowIcon(direction = ArrowDirection.UP)
            }
        }

        // Left button
        Box(modifier = Modifier.offset(x = -buttonOffset)) {
            ControlButton(
                onClick = onMoveLeft,
                color = PrimaryBlue,
                size = buttonSize
            ) {
                ArrowIcon(direction = ArrowDirection.LEFT)
            }
        }

        // Right button
        Box(modifier = Modifier.offset(x = buttonOffset)) {
            ControlButton(
                onClick = onMoveRight,
                color = PrimaryBlue,
                size = buttonSize
            ) {
                ArrowIcon(direction = ArrowDirection.RIGHT)
            }
        }

        // Action button (down)
        Box(modifier = Modifier.offset(y = buttonOffset)) {
            ControlButton(
                onClick = onAction,
                color = AccentOrange,
                size = buttonSize
            ) {
                ActionIcon()
            }
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowOffset = scaledDp(5)

    val darkColor = Color(
        red = (color.red * 0.6f).coerceIn(0f, 1f),
        green = (color.green * 0.6f).coerceIn(0f, 1f),
        blue = (color.blue * 0.6f).coerceIn(0f, 1f)
    )

    val pressedColor = Color(
        red = (color.red * 0.4f).coerceIn(0f, 1f),
        green = (color.green * 0.4f).coerceIn(0f, 1f),
        blue = (color.blue * 0.4f).coerceIn(0f, 1f)
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                translationY = if (isPressed) shadowOffset.value else 0f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shadow/3D base (bottom layer)
        if (!isPressed) {
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(y = shadowOffset)
                    .clip(CircleShape)
                    .background(darkColor)
            )
        }

        // Main button face
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isPressed) pressedColor else color),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun GameHUD(
    levelNumber: Int,
    moves: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = scaledDp(12), vertical = scaledDp(8)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(scaledDp(32))
                .clip(RoundedCornerShape(scaledDp(6)))
                .background(PrimaryBlue)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            BackIcon()
        }

        // Level info
        Text(
            text = "Level $levelNumber",
            color = Color.White,
            fontSize = scaledSp(12)
        )

        // Moves counter
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Moves:", color = Color.White.copy(alpha = 0.7f), fontSize = scaledSp(10))
            Spacer(modifier = Modifier.width(scaledDp(4)))
            Text(text = "$moves", color = AccentOrange, fontSize = scaledSp(12))
        }

        // Restart button
        Box(
            modifier = Modifier
                .size(scaledDp(32))
                .clip(RoundedCornerShape(scaledDp(6)))
                .background(AccentOrange)
                .clickable(onClick = onRestart),
            contentAlignment = Alignment.Center
        ) {
            RestartIcon()
        }
    }
}

private enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT
}

@Composable
private fun ArrowIcon(direction: ArrowDirection, iconSize: Int = 62) {
    Canvas(modifier = Modifier.size(scaledDp(iconSize))) {
        val path = Path()
        val w = this.size.width
        val h = this.size.height
        val padding = w * 0.15f

        when (direction) {
            ArrowDirection.UP -> {
                path.moveTo(w / 2, padding)
                path.lineTo(w - padding, h - padding)
                path.lineTo(padding, h - padding)
                path.close()
            }
            ArrowDirection.DOWN -> {
                path.moveTo(padding, padding)
                path.lineTo(w - padding, padding)
                path.lineTo(w / 2, h - padding)
                path.close()
            }
            ArrowDirection.LEFT -> {
                path.moveTo(w - padding, padding)
                path.lineTo(w - padding, h - padding)
                path.lineTo(padding, h / 2)
                path.close()
            }
            ArrowDirection.RIGHT -> {
                path.moveTo(padding, padding)
                path.lineTo(w - padding, h / 2)
                path.lineTo(padding, h - padding)
                path.close()
            }
        }

        drawPath(path, Color.White, style = Fill)
    }
}

@Composable
private fun ActionIcon(iconSize: Int = 62) {
    Canvas(modifier = Modifier.size(scaledDp(iconSize))) {
        val w = this.size.width
        val h = this.size.height
        val padding = w * 0.2f
        val barHeight = h * 0.15f

        // Draw hand/grab icon - horizontal bar with two vertical bars
        // Top horizontal bar
        drawRect(
            color = Color.White,
            topLeft = Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(w - padding * 2, barHeight)
        )
        // Left vertical bar
        drawRect(
            color = Color.White,
            topLeft = Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(barHeight, h - padding * 2)
        )
        // Right vertical bar
        drawRect(
            color = Color.White,
            topLeft = Offset(w - padding - barHeight, padding),
            size = androidx.compose.ui.geometry.Size(barHeight, h - padding * 2)
        )
        // Bottom horizontal bar
        drawRect(
            color = Color.White,
            topLeft = Offset(padding, h - padding - barHeight),
            size = androidx.compose.ui.geometry.Size(w - padding * 2, barHeight)
        )
    }
}

@Composable
private fun RestartIcon(iconSize: Int = 20) {
    Canvas(modifier = Modifier.size(scaledDp(iconSize))) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.15f

        // Draw circular arrow (restart icon)
        drawArc(
            color = Color.White,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(strokeWidth, strokeWidth),
            size = androidx.compose.ui.geometry.Size(w - strokeWidth * 2, h - strokeWidth * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Arrow head
        val path = Path()
        val arrowSize = w * 0.3f
        path.moveTo(w * 0.65f, strokeWidth * 0.5f)
        path.lineTo(w * 0.65f + arrowSize, strokeWidth * 1.5f)
        path.lineTo(w * 0.65f, strokeWidth * 2.5f)
        path.close()
        drawPath(path, Color.White, style = Fill)
    }
}

@Composable
private fun BackIcon(iconSize: Int = 20) {
    Canvas(modifier = Modifier.size(scaledDp(iconSize))) {
        val w = this.size.width
        val h = this.size.height
        val padding = w * 0.2f

        val path = Path()
        path.moveTo(w - padding, padding)
        path.lineTo(padding, h / 2)
        path.lineTo(w - padding, h - padding)
        path.close()

        drawPath(path, Color.White, style = Fill)
    }
}
