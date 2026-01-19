package com.blockdude2.game.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.blockdude2.game.data.GameState
import com.blockdude2.game.data.Level
import com.blockdude2.game.game.GameEngine
import com.blockdude2.game.ui.components.GameCanvas
import com.blockdude2.game.ui.components.GameControls
import com.blockdude2.game.ui.components.GameHUD
import com.blockdude2.game.ui.components.ScaledContainer
import com.blockdude2.game.ui.components.scaledDp
import com.blockdude2.game.ui.components.scaledSp
import com.blockdude2.game.ui.theme.*

@Composable
fun GameScreen(
    level: Level,
    gameState: GameState,
    gameEngine: GameEngine,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveUp: () -> Unit,
    onAction: () -> Unit,
    onRestart: () -> Unit,
    onEnemyTick: () -> Unit,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    hasNextLevel: Boolean
) {
    ScaledContainer(backgroundColor = DarkBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // HUD
                GameHUD(
                    levelNumber = level.id,
                    moves = gameState.moves,
                    onRestart = onRestart,
                    onBack = onBack
                )

                // Game area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(scaledDp(12)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    GameCanvas(
                        level = level,
                        gameState = gameState,
                        gameEngine = gameEngine,
                        onEnemyTick = onEnemyTick
                    )
                }

                // Controls
                GameControls(
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onMoveUp = onMoveUp,
                    onAction = onAction,
                    modifier = Modifier.padding(bottom = scaledDp(16))
                )
            }

            // Level complete overlay
            AnimatedVisibility(
                visible = gameState.levelCompleted,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                LevelCompleteOverlay(
                    moves = gameState.moves,
                    onRestart = onRestart,
                    onNextLevel = onNextLevel,
                    onBack = onBack,
                    hasNextLevel = hasNextLevel
                )
            }
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    moves: Int,
    onRestart: () -> Unit,
    onNextLevel: () -> Unit,
    onBack: () -> Unit,
    hasNextLevel: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(scaledDp(24))
                .clip(RoundedCornerShape(scaledDp(12)))
                .background(SurfaceColor)
                .padding(scaledDp(24))
        ) {
            Text(
                text = "LEVEL",
                color = TextWhite,
                fontSize = scaledSp(12),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(scaledDp(6)))
            Text(
                text = "COMPLETE!",
                color = CompletedColor,
                fontSize = scaledSp(16),
                fontWeight = FontWeight.Bold,
                letterSpacing = scaledSp(2)
            )

            Spacer(modifier = Modifier.height(scaledDp(12)))

            Text(
                text = "Moves: $moves",
                color = AccentOrange,
                fontSize = scaledSp(10)
            )

            Spacer(modifier = Modifier.height(scaledDp(18)))

            // Buttons
            if (hasNextLevel) {
                OverlayButton(
                    text = "NEXT",
                    color = CompletedColor,
                    onClick = onNextLevel
                )
                Spacer(modifier = Modifier.height(scaledDp(6)))
            }

            OverlayButton(
                text = "RETRY",
                color = AccentOrange,
                onClick = onRestart
            )

            Spacer(modifier = Modifier.height(scaledDp(6)))

            OverlayButton(
                text = "LEVELS",
                color = PrimaryBlue,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun OverlayButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(scaledDp(120))
            .height(scaledDp(36))
            .clip(RoundedCornerShape(scaledDp(6)))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = scaledSp(9),
            fontWeight = FontWeight.Bold
        )
    }
}
