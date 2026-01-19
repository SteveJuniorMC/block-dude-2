package com.blockdude2.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.blockdude2.game.data.GameState
import com.blockdude2.game.data.Level
import com.blockdude2.game.game.CellInfo
import com.blockdude2.game.game.Direction
import com.blockdude2.game.game.GameEngine
import com.blockdude2.game.ui.theme.*

@Composable
fun GameCanvas(
    level: Level,
    gameState: GameState,
    gameEngine: GameEngine,
    modifier: Modifier = Modifier
) {
    val animatedPlayerX = remember { Animatable(gameState.playerPosition.x.toFloat()) }
    val animatedPlayerY = remember { Animatable(gameState.playerPosition.y.toFloat()) }
    val animatedViewportOffset = remember { Animatable(gameEngine.getViewportOffset(gameState).toFloat()) }

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
        animatedViewportOffset.animateTo(
            targetValue = gameEngine.getViewportOffset(gameState).toFloat(),
            animationSpec = tween(150)
        )
    }

    // Use viewport width for aspect ratio calculation, not full level width
    val viewportWidth = minOf(level.viewportWidth, level.width)
    val aspectRatio = viewportWidth.toFloat() / level.height.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    ) {
        val cellWidth = size.width / viewportWidth
        val cellHeight = size.height / level.height
        val cellSize = minOf(cellWidth, cellHeight)

        val offsetX = (size.width - cellSize * viewportWidth) / 2
        val offsetY = (size.height - cellSize * level.height) / 2

        // Draw background
        drawRect(
            color = GroundColor,
            topLeft = Offset(offsetX, offsetY),
            size = Size(cellSize * viewportWidth, cellSize * level.height)
        )

        val viewportStart = animatedViewportOffset.value.toInt()
        val viewportEnd = viewportStart + viewportWidth

        // Draw all visible cells (within viewport)
        for (y in 0 until level.height) {
            for (x in viewportStart until minOf(viewportEnd, level.width)) {
                val cellInfo = gameEngine.getCellAt(x, y, gameState)
                val cellX = offsetX + (x - animatedViewportOffset.value) * cellSize
                val cellY = offsetY + y * cellSize

                when (cellInfo) {
                    is CellInfo.Wall -> drawWall(cellX, cellY, cellSize)
                    is CellInfo.Block -> drawBlock(cellX, cellY, cellSize)
                    is CellInfo.Door -> drawDoor(cellX, cellY, cellSize)
                    is CellInfo.Empty -> {} // Already have background
                    is CellInfo.Player -> {} // Draw separately for animation
                }
            }
        }

        // Draw player with animation (adjusted for viewport)
        val playerX = offsetX + (animatedPlayerX.value - animatedViewportOffset.value) * cellSize
        val playerY = offsetY + animatedPlayerY.value * cellSize
        drawPlayer(playerX, playerY, cellSize, gameState.playerFacing, gameState.holdingBlock)

        // Draw door on top if player is at door (for win effect)
        if (gameState.levelCompleted) {
            val doorX = offsetX + (level.doorPosition.x - animatedViewportOffset.value) * cellSize
            val doorY = offsetY + level.doorPosition.y * cellSize
            drawDoorOpen(doorX, doorY, cellSize)
        }
    }
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

    // Draw horizontal mortar lines (including top and bottom edges for seamless vertical tiling)
    for (row in 0..4) {
        drawLine(
            color = mortarColor,
            start = Offset(x, y + row * brickHeight),
            end = Offset(x + size, y + row * brickHeight),
            strokeWidth = mortarWidth
        )
    }

    // Draw vertical mortar lines (staggered pattern)
    // Row 0 and 2: center line
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

    // Row 1 and 3: edge lines (seamless with adjacent tiles)
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

    // Main block
    val blockX = x + margin
    val blockY = y + margin
    val blockW = size - margin * 2
    val blockH = size - margin * 2

    // Flat block face
    drawRect(
        color = BlockColor,
        topLeft = Offset(blockX, blockY),
        size = Size(blockW, blockH)
    )

    // Wood grain lines
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

    // Thin border
    drawRect(
        color = Color(0xFF805020),
        topLeft = Offset(blockX, blockY),
        size = Size(blockW, blockH),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = size * 0.04f)
    )
}

private fun DrawScope.drawDoor(x: Float, y: Float, size: Float) {
    val padding = size * 0.1f

    // Door frame
    drawRect(
        color = DoorFrame,
        topLeft = Offset(x + padding, y),
        size = Size(size - padding * 2, size)
    )

    // Door
    drawRect(
        color = DoorColor,
        topLeft = Offset(x + padding * 2, y + padding),
        size = Size(size - padding * 4, size - padding)
    )

    // Door handle
    drawCircle(
        color = Color(0xFF5A9E6A),
        radius = size * 0.08f,
        center = Offset(x + size * 0.7f, y + size * 0.5f)
    )
}

private fun DrawScope.drawDoorOpen(x: Float, y: Float, size: Float) {
    val padding = size * 0.1f

    // Door frame
    drawRect(
        color = DoorFrame,
        topLeft = Offset(x + padding, y),
        size = Size(size - padding * 2, size)
    )

    // Open door (dark inside)
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

    // Held block (drawn first, behind player, above head)
    if (holdingBlock) {
        val blockSize = size * 0.85f
        val blockX = x + size / 2 - blockSize / 2
        val blockY = y - blockSize + size * 0.1f
        drawBlock(blockX, blockY, blockSize)
    }

    // Draw player sprite pixel by pixel
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
