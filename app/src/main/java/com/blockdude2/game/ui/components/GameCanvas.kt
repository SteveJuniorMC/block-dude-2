package com.blockdude2.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.blockdude2.game.data.GameState
import com.blockdude2.game.data.Level
import com.blockdude2.game.data.TerrainType
import com.blockdude2.game.game.CellInfo
import com.blockdude2.game.game.Direction
import com.blockdude2.game.game.GameEngine
import com.blockdude2.game.ui.theme.*

data class AnimatedEnemy(
    val x: Animatable<Float, *>,
    val y: Animatable<Float, *>
)

@Composable
fun GameCanvas(
    level: Level,
    gameState: GameState,
    gameEngine: GameEngine,
    onEnemyTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedPlayerX = remember { Animatable(gameState.playerPosition.x.toFloat()) }
    val animatedPlayerY = remember { Animatable(gameState.playerPosition.y.toFloat()) }
    val animatedViewportOffset = remember { Animatable(gameEngine.getViewportOffset(gameState).toFloat()) }

    // Animated enemy positions
    val animatedEnemies = remember(gameState.enemies.size) {
        gameState.enemies.map { enemy ->
            AnimatedEnemy(
                x = Animatable(enemy.position.x.toFloat()),
                y = Animatable(enemy.position.y.toFloat())
            )
        }
    }

    LaunchedEffect(gameState.playerPosition) {
        animatedPlayerX.animateTo(
            targetValue = gameState.playerPosition.x.toFloat(),
            animationSpec = tween(100)
        )
    }

    LaunchedEffect(gameState.playerPosition) {
        animatedPlayerY.animateTo(
            targetValue = gameState.playerPosition.y.toFloat(),
            animationSpec = tween(100)
        )
    }

    LaunchedEffect(gameState.playerPosition) {
        // Smooth viewport scroll synced with player movement
        animatedViewportOffset.animateTo(
            targetValue = gameEngine.getViewportOffset(gameState).toFloat(),
            animationSpec = tween(100)  // Same as player animation
        )
    }

    // Continuous enemy movement - runs as long as game is active
    LaunchedEffect(Unit) {
        while (true) {
            // Wait a frame for state to be ready
            kotlinx.coroutines.delay(16)

            // Trigger enemy movement logic
            onEnemyTick()

            // Animate to new positions (300ms smooth movement)
            kotlinx.coroutines.delay(300)
        }
    }

    // Animate enemies when their positions change
    LaunchedEffect(gameState.enemies) {
        gameState.enemies.forEachIndexed { index, enemy ->
            if (index < animatedEnemies.size) {
                launch {
                    animatedEnemies[index].x.animateTo(
                        targetValue = enemy.position.x.toFloat(),
                        animationSpec = tween(280, easing = LinearEasing)
                    )
                }
                launch {
                    animatedEnemies[index].y.animateTo(
                        targetValue = enemy.position.y.toFloat(),
                        animationSpec = tween(280, easing = LinearEasing)
                    )
                }
            }
        }
    }

    // Use viewport width for aspect ratio calculation, not full level width
    val viewportWidth = minOf(level.viewportWidth, level.width)
    val aspectRatio = viewportWidth.toFloat() / level.height.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    ) {
        // Fill entire canvas - no borders
        val cellSize = size.width / viewportWidth
        val offsetX = 0f
        val offsetY = (size.height - cellSize * level.height) / 2

        // Draw background across full width
        drawRect(
            color = GroundColor,
            topLeft = Offset(0f, offsetY),
            size = Size(size.width, cellSize * level.height)
        )

        val viewportStartFloat = animatedViewportOffset.value
        // Render 2 extra cells on each side as buffer for smooth scrolling
        val bufferCells = 2
        val viewportStartInt = (viewportStartFloat - bufferCells).toInt()
        val viewportEndInt = viewportStartFloat.toInt() + viewportWidth + bufferCells + 1

        // Draw all cells including buffer zone
        for (y in 0 until level.height) {
            for (x in maxOf(0, viewportStartInt) until minOf(viewportEndInt, level.width)) {
                val cellInfo = gameEngine.getCellAt(x, y, gameState)
                val cellX = offsetX + (x - viewportStartFloat) * cellSize
                val cellY = offsetY + y * cellSize

                when (cellInfo) {
                    is CellInfo.Wall -> drawWall(cellX, cellY, cellSize)
                    is CellInfo.Block -> drawBlock(cellX, cellY, cellSize)
                    is CellInfo.Door -> drawDoor(cellX, cellY, cellSize)
                    is CellInfo.Terrain -> drawTerrain(cellX, cellY, cellSize, cellInfo.type)
                    is CellInfo.Enemy -> {} // Draw separately for animation
                    is CellInfo.Empty -> {} // Already have background
                    is CellInfo.Player -> {} // Draw separately for animation
                }
            }
        }

        // Draw enemies with smooth animation
        gameState.enemies.forEachIndexed { index, enemy ->
            if (index < animatedEnemies.size) {
                val enemyX = offsetX + (animatedEnemies[index].x.value - viewportStartFloat) * cellSize
                val enemyY = offsetY + animatedEnemies[index].y.value * cellSize
                drawEnemy(enemyX, enemyY, cellSize, enemy.facing)
            }
        }

        // Draw player with animation (adjusted for viewport)
        val playerX = offsetX + (animatedPlayerX.value - viewportStartFloat) * cellSize
        val playerY = offsetY + animatedPlayerY.value * cellSize
        drawPlayer(playerX, playerY, cellSize, gameState.playerFacing, gameState.holdingBlock)

        // Draw door on top if player is at door (for win effect)
        if (gameState.levelCompleted) {
            val doorX = offsetX + (level.doorPosition.x - viewportStartFloat) * cellSize
            val doorY = offsetY + level.doorPosition.y * cellSize
            drawDoorOpen(doorX, doorY, cellSize)
        }

        // Draw game over overlay
        if (gameState.gameOver) {
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(0f, offsetY),
                size = Size(size.width, cellSize * level.height)
            )
        }
    }
}

private fun DrawScope.drawTerrain(x: Float, y: Float, size: Float, type: TerrainType) {
    val grassColor = if (type == TerrainType.GRASS) GrassColor else GrassVariantColor
    val dirtColor = if (type == TerrainType.GRASS) DirtColor else DirtVariantColor

    // Grass top (about 35% of cell)
    val grassHeight = size * 0.35f
    drawRect(
        color = grassColor,
        topLeft = Offset(x, y),
        size = Size(size, grassHeight)
    )

    // Dirt bottom (65% of cell)
    drawRect(
        color = dirtColor,
        topLeft = Offset(x, y + grassHeight),
        size = Size(size, size - grassHeight)
    )

    // Grass blade details
    val bladeColor = if (type == TerrainType.GRASS) Color(0xFF5A9C2A) else Color(0xFF6AAC3A)
    val bladeWidth = size * 0.08f
    val bladePositions = if (type == TerrainType.GRASS) listOf(0.2f, 0.5f, 0.8f) else listOf(0.3f, 0.7f)

    for (pos in bladePositions) {
        drawRect(
            color = bladeColor,
            topLeft = Offset(x + size * pos - bladeWidth / 2, y),
            size = Size(bladeWidth, grassHeight * 0.6f)
        )
    }
}

private fun DrawScope.drawEnemy(x: Float, y: Float, size: Float, facing: Direction) {
    val margin = size * 0.08f

    // Slug body
    val bodyX = x + margin
    val bodyY = y + margin * 2
    val bodyW = size - margin * 2
    val bodyH = size - margin * 3

    // Main body (purple slug)
    drawRoundRect(
        color = EnemyColor,
        topLeft = Offset(bodyX, bodyY),
        size = Size(bodyW, bodyH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyH / 2, bodyH / 2)
    )

    // Darker underside
    drawRoundRect(
        color = EnemyDarkColor,
        topLeft = Offset(bodyX, bodyY + bodyH * 0.6f),
        size = Size(bodyW, bodyH * 0.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyH / 4, bodyH / 4)
    )

    // Eyes
    val eyeSize = size * 0.15f
    val eyeY = bodyY + bodyH * 0.25f
    val eyeOffset = if (facing == Direction.LEFT) 0.2f else 0.5f

    // Left eye
    drawCircle(
        color = Color.White,
        radius = eyeSize,
        center = Offset(bodyX + bodyW * eyeOffset, eyeY)
    )
    drawCircle(
        color = Color.Black,
        radius = eyeSize * 0.5f,
        center = Offset(bodyX + bodyW * eyeOffset, eyeY)
    )

    // Right eye
    drawCircle(
        color = Color.White,
        radius = eyeSize,
        center = Offset(bodyX + bodyW * (eyeOffset + 0.25f), eyeY)
    )
    drawCircle(
        color = Color.Black,
        radius = eyeSize * 0.5f,
        center = Offset(bodyX + bodyW * (eyeOffset + 0.25f), eyeY)
    )
}

private fun DrawScope.drawWall(x: Float, y: Float, size: Float) {
    // Main wall block with base color
    drawRect(
        color = WallColor,
        topLeft = Offset(x, y),
        size = Size(size, size)
    )

    // Brick pattern - 4 rows for seamless tiling
    val brickHeight = size / 4
    val brickWidth = size / 2
    val mortarColor = Color(0xFF252525)
    val mortarWidth = size * 0.05f

    // Draw horizontal mortar lines
    for (row in 0..4) {
        drawLine(
            color = mortarColor,
            start = Offset(x, y + row * brickHeight),
            end = Offset(x + size, y + row * brickHeight),
            strokeWidth = mortarWidth
        )
    }

    // Draw vertical mortar lines (staggered pattern)
    drawLine(
        color = mortarColor,
        start = Offset(x + brickWidth, y),
        end = Offset(x + brickWidth, y + brickHeight),
        strokeWidth = mortarWidth
    )
    drawLine(
        color = mortarColor,
        start = Offset(x + brickWidth, y + 2 * brickHeight),
        end = Offset(x + brickWidth, y + 3 * brickHeight),
        strokeWidth = mortarWidth
    )

    drawLine(
        color = mortarColor,
        start = Offset(x, y + brickHeight),
        end = Offset(x, y + 2 * brickHeight),
        strokeWidth = mortarWidth
    )
    drawLine(
        color = mortarColor,
        start = Offset(x + size, y + brickHeight),
        end = Offset(x + size, y + 2 * brickHeight),
        strokeWidth = mortarWidth
    )
    drawLine(
        color = mortarColor,
        start = Offset(x, y + 3 * brickHeight),
        end = Offset(x, y + size),
        strokeWidth = mortarWidth
    )
    drawLine(
        color = mortarColor,
        start = Offset(x + size, y + 3 * brickHeight),
        end = Offset(x + size, y + size),
        strokeWidth = mortarWidth
    )
}

private fun DrawScope.drawBlock(x: Float, y: Float, size: Float) {
    val margin = size * 0.05f

    val blockX = x + margin
    val blockY = y + margin
    val blockW = size - margin * 2
    val blockH = size - margin * 2

    drawRect(
        color = BlockColor,
        topLeft = Offset(blockX, blockY),
        size = Size(blockW, blockH)
    )

    val grainColor = Color(0xFFA07040)
    val grainWidth = size * 0.03f

    drawLine(
        color = grainColor,
        start = Offset(blockX + blockW * 0.08f, blockY + blockH * 0.22f),
        end = Offset(blockX + blockW * 0.92f, blockY + blockH * 0.22f),
        strokeWidth = grainWidth
    )
    drawLine(
        color = grainColor,
        start = Offset(blockX + blockW * 0.12f, blockY + blockH * 0.48f),
        end = Offset(blockX + blockW * 0.88f, blockY + blockH * 0.48f),
        strokeWidth = grainWidth
    )
    drawLine(
        color = grainColor,
        start = Offset(blockX + blockW * 0.08f, blockY + blockH * 0.75f),
        end = Offset(blockX + blockW * 0.92f, blockY + blockH * 0.75f),
        strokeWidth = grainWidth
    )

    drawRect(
        color = Color(0xFF805020),
        topLeft = Offset(blockX, blockY),
        size = Size(blockW, blockH),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = size * 0.04f)
    )
}

private fun DrawScope.drawDoor(x: Float, y: Float, size: Float) {
    val padding = size * 0.1f

    drawRect(
        color = DoorFrame,
        topLeft = Offset(x + padding, y),
        size = Size(size - padding * 2, size)
    )

    drawRect(
        color = DoorColor,
        topLeft = Offset(x + padding * 2, y + padding),
        size = Size(size - padding * 4, size - padding)
    )

    drawCircle(
        color = Color(0xFF5A9E6A),
        radius = size * 0.08f,
        center = Offset(x + size * 0.7f, y + size * 0.5f)
    )
}

private fun DrawScope.drawDoorOpen(x: Float, y: Float, size: Float) {
    val padding = size * 0.1f

    drawRect(
        color = DoorFrame,
        topLeft = Offset(x + padding, y),
        size = Size(size - padding * 2, size)
    )

    drawRect(
        color = Color(0xFF1A3A1A),
        topLeft = Offset(x + padding * 2, y + padding),
        size = Size(size - padding * 4, size - padding)
    )
}

// Player sprite data (8x8 pixels)
private val SPRITE_LEFT = arrayOf(
    intArrayOf(0, 0, 0, 1, 1, 1, 0, 0),
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(0, 0, 0, 1, 0, 0, 1, 0),
    intArrayOf(0, 0, 1, 0, 0, 0, 1, 0),
    intArrayOf(0, 0, 0, 1, 0, 1, 0, 0),
    intArrayOf(0, 1, 0, 1, 0, 1, 0, 0),
    intArrayOf(0, 0, 0, 0, 1, 0, 0, 0),
    intArrayOf(0, 0, 1, 1, 0, 1, 1, 0)
)

private val SPRITE_RIGHT = arrayOf(
    intArrayOf(0, 0, 1, 1, 1, 0, 0, 0),
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(0, 1, 0, 0, 1, 0, 0, 0),
    intArrayOf(0, 1, 0, 0, 0, 1, 0, 0),
    intArrayOf(0, 0, 1, 0, 1, 0, 0, 0),
    intArrayOf(0, 0, 1, 0, 1, 0, 1, 0),
    intArrayOf(0, 0, 0, 1, 0, 0, 0, 0),
    intArrayOf(0, 1, 1, 0, 1, 1, 0, 0)
)

private fun DrawScope.drawPlayer(x: Float, y: Float, size: Float, facing: Direction, holdingBlock: Boolean) {
    val pixelSize = size / 8f
    val sprite = if (facing == Direction.LEFT) SPRITE_LEFT else SPRITE_RIGHT

    if (holdingBlock) {
        val blockSize = size * 0.85f
        val blockX = x + size / 2 - blockSize / 2
        val blockY = y - blockSize + size * 0.1f
        drawBlock(blockX, blockY, blockSize)
    }

    for (row in 0 until 8) {
        for (col in 0 until 8) {
            if (sprite[row][col] == 1) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(x + col * pixelSize, y + row * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}
