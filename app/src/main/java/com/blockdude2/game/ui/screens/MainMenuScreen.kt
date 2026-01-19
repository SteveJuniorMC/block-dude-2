package com.blockdude2.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import com.blockdude2.game.ui.components.ScaledContainer
import com.blockdude2.game.ui.components.scaledDp
import com.blockdude2.game.ui.components.scaledSp
import com.blockdude2.game.ui.theme.*

@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit,
    onLevelSelectClick: () -> Unit
) {
    ScaledContainer(backgroundColor = DarkBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaledDp(24)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // Title
            Text(
                text = "BLOCK",
                color = PlayerColor,
                fontSize = scaledSp(42),
                fontWeight = FontWeight.Bold,
                letterSpacing = scaledSp(4)
            )
            Text(
                text = "DUDE 2",
                color = AccentOrange,
                fontSize = scaledSp(42),
                fontWeight = FontWeight.Bold,
                letterSpacing = scaledSp(4)
            )

            Spacer(modifier = Modifier.height(scaledDp(12)))

            // Subtitle
            Text(
                text = "Scrolling Puzzle Adventure",
                color = TextWhite.copy(alpha = 0.6f),
                fontSize = scaledSp(10)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Play button
            MenuButton(
                text = "PLAY",
                color = AccentOrange,
                onClick = onPlayClick
            )

            Spacer(modifier = Modifier.height(scaledDp(12)))

            // Level Select button
            MenuButton(
                text = "LEVELS",
                color = PrimaryBlue,
                onClick = onLevelSelectClick
            )

            Spacer(modifier = Modifier.weight(1f))

            // Instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = scaledDp(16))
            ) {
                Text(
                    text = "HOW TO PLAY",
                    color = TextWhite,
                    fontSize = scaledSp(10),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(scaledDp(10)))

                // Move left/right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(6))
                ) {
                    MiniButton(color = PrimaryBlue) { MiniArrow(Direction.LEFT) }
                    MiniButton(color = PrimaryBlue) { MiniArrow(Direction.RIGHT) }
                    Text(
                        text = "Move",
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = scaledSp(8)
                    )
                }

                Spacer(modifier = Modifier.height(scaledDp(6)))

                // Climb up
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(6))
                ) {
                    MiniButton(color = PrimaryBlue) { MiniArrow(Direction.UP) }
                    Text(
                        text = "Climb",
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = scaledSp(8)
                    )
                }

                Spacer(modifier = Modifier.height(scaledDp(6)))

                // Pick up / place
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(6))
                ) {
                    MiniButton(color = AccentOrange) { MiniActionIcon() }
                    Text(
                        text = "Pick up / Place",
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = scaledSp(8)
                    )
                }

                Spacer(modifier = Modifier.height(scaledDp(10)))

                Text(
                    text = "Reach the door!",
                    color = DoorColor,
                    fontSize = scaledSp(8)
                )
            }

            Spacer(modifier = Modifier.height(scaledDp(16)))
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(scaledDp(180))
            .height(scaledDp(48))
            .clip(RoundedCornerShape(scaledDp(10)))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = scaledSp(14),
            fontWeight = FontWeight.Bold,
            letterSpacing = scaledSp(2)
        )
    }
}

private enum class Direction { UP, LEFT, RIGHT }

@Composable
private fun MiniButton(
    color: Color,
    content: @Composable () -> Unit
) {
    val size = scaledDp(24)
    val darkColor = Color(
        red = (color.red * 0.6f).coerceIn(0f, 1f),
        green = (color.green * 0.6f).coerceIn(0f, 1f),
        blue = (color.blue * 0.6f).coerceIn(0f, 1f)
    )

    Box(contentAlignment = Alignment.Center) {
        // Shadow
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = scaledDp(2))
                .clip(CircleShape)
                .background(darkColor)
        )
        // Button face
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun MiniArrow(direction: Direction) {
    val iconSize = scaledDp(12)
    Canvas(modifier = Modifier.size(iconSize)) {
        val path = Path()
        val w = size.width
        val h = size.height
        val padding = w * 0.15f

        when (direction) {
            Direction.UP -> {
                path.moveTo(w / 2, padding)
                path.lineTo(w - padding, h - padding)
                path.lineTo(padding, h - padding)
                path.close()
            }
            Direction.LEFT -> {
                path.moveTo(w - padding, padding)
                path.lineTo(w - padding, h - padding)
                path.lineTo(padding, h / 2)
                path.close()
            }
            Direction.RIGHT -> {
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
private fun MiniActionIcon() {
    val iconSize = scaledDp(12)
    Canvas(modifier = Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val padding = w * 0.2f
        val barHeight = h * 0.15f

        // Draw grab icon
        drawRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(w - padding * 2, barHeight)
        )
        drawRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(barHeight, h - padding * 2)
        )
        drawRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(w - padding - barHeight, padding),
            size = androidx.compose.ui.geometry.Size(barHeight, h - padding * 2)
        )
        drawRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(padding, h - padding - barHeight),
            size = androidx.compose.ui.geometry.Size(w - padding * 2, barHeight)
        )
    }
}
