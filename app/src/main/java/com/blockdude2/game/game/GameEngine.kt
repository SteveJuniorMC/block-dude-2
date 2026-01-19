package com.blockdude2.game.game

import com.blockdude2.game.data.Enemy
import com.blockdude2.game.data.GameState
import com.blockdude2.game.data.Level
import com.blockdude2.game.data.Position
import com.blockdude2.game.data.TerrainType
import kotlin.math.abs

class GameEngine(private val level: Level) {

    fun createInitialState(): GameState {
        return GameState(
            playerPosition = level.playerStart,
            playerFacing = Direction.RIGHT,
            holdingBlock = false,
            blocks = level.initialBlocks.toMutableSet(),
            enemies = level.initialEnemies.map { Enemy(it, Direction.LEFT) },
            levelCompleted = false,
            gameOver = false,
            moves = 0
        )
    }

    fun moveLeft(state: GameState): GameState {
        if (state.levelCompleted || state.gameOver) return state
        val newState = state.copy(playerFacing = Direction.LEFT, moves = state.moves + 1)
        return tryMove(newState, -1).let { moveEnemies(it) }.let { checkEnemyCollision(it) }
    }

    fun moveRight(state: GameState): GameState {
        if (state.levelCompleted || state.gameOver) return state
        val newState = state.copy(playerFacing = Direction.RIGHT, moves = state.moves + 1)
        return tryMove(newState, 1).let { moveEnemies(it) }.let { checkEnemyCollision(it) }
    }

    fun moveUp(state: GameState): GameState {
        if (state.levelCompleted || state.gameOver) return state
        val dx = if (state.playerFacing == Direction.LEFT) -1 else 1
        val newState = state.copy(moves = state.moves + 1)
        return tryClimb(newState, dx).let { moveEnemies(it) }.let { checkEnemyCollision(it) }
    }

    private fun tryClimb(state: GameState, dx: Int): GameState {
        val currentPos = state.playerPosition
        val targetPos = Position(currentPos.x + dx, currentPos.y)
        val climbPos = Position(currentPos.x + dx, currentPos.y - 1)
        val aboveClimb = Position(climbPos.x, climbPos.y - 1)
        val aboveCurrent = Position(currentPos.x, currentPos.y - 1)

        // Can climb if: there's a solid/block at target level, climb position is free
        val canClimb = (isSolid(targetPos) || isBlock(targetPos, state.blocks)) &&
            !isSolid(climbPos) && !isBlock(climbPos, state.blocks) &&
            (!state.holdingBlock || (
                !isSolid(aboveClimb) && !isBlock(aboveClimb, state.blocks) &&
                !isSolid(aboveCurrent) && !isBlock(aboveCurrent, state.blocks)
            ))

        if (canClimb) {
            val movedState = state.copy(playerPosition = climbPos)
            return applyGravity(movedState).let { checkWin(it) }
        }
        return state.copy(moves = state.moves - 1)
    }

    private fun tryMove(state: GameState, dx: Int): GameState {
        val currentPos = state.playerPosition
        val targetPos = Position(currentPos.x + dx, currentPos.y)
        val aboveTarget = Position(targetPos.x, targetPos.y - 1)

        if (isSolid(targetPos)) {
            return state.copy(moves = state.moves - 1)
        }

        if (isBlock(targetPos, state.blocks)) {
            return state.copy(moves = state.moves - 1)
        }

        if (state.holdingBlock && (isSolid(aboveTarget) || isBlock(aboveTarget, state.blocks))) {
            return state.copy(moves = state.moves - 1)
        }

        val movedState = state.copy(playerPosition = targetPos)
        return applyGravity(movedState).let { checkWin(it) }
    }

    fun pickUpOrPlace(state: GameState): GameState {
        if (state.levelCompleted || state.gameOver) return state

        val result = if (state.holdingBlock) {
            placeBlock(state)
        } else {
            pickUpBlock(state)
        }
        return moveEnemies(result).let { checkEnemyCollision(it) }
    }

    private fun pickUpBlock(state: GameState): GameState {
        val dx = if (state.playerFacing == Direction.LEFT) -1 else 1
        val frontPos = Position(state.playerPosition.x + dx, state.playerPosition.y)
        val aboveFront = Position(frontPos.x, frontPos.y - 1)
        val abovePlayer = Position(state.playerPosition.x, state.playerPosition.y - 1)

        if (isBlock(frontPos, state.blocks) &&
            !isBlock(aboveFront, state.blocks) && !isSolid(aboveFront) &&
            !isBlock(abovePlayer, state.blocks) && !isSolid(abovePlayer)
        ) {
            val newBlocks = state.blocks.toMutableSet()
            newBlocks.remove(frontPos)
            return state.copy(
                holdingBlock = true,
                blocks = newBlocks,
                moves = state.moves + 1
            )
        }

        val belowFront = Position(frontPos.x, frontPos.y + 1)
        if (isBlock(belowFront, state.blocks) &&
            !isBlock(frontPos, state.blocks) && !isSolid(frontPos) &&
            !isBlock(abovePlayer, state.blocks) && !isSolid(abovePlayer)
        ) {
            val aboveBelowFront = frontPos
            if (!isBlock(aboveBelowFront, state.blocks) && !isSolid(aboveBelowFront)) {
                val newBlocks = state.blocks.toMutableSet()
                newBlocks.remove(belowFront)
                return state.copy(
                    holdingBlock = true,
                    blocks = newBlocks,
                    moves = state.moves + 1
                )
            }
        }

        return state
    }

    private fun placeBlock(state: GameState): GameState {
        val dx = if (state.playerFacing == Direction.LEFT) -1 else 1
        val frontPos = Position(state.playerPosition.x + dx, state.playerPosition.y)

        var placePos = frontPos

        if (isSolid(frontPos) || isBlock(frontPos, state.blocks)) {
            val frontAbove = Position(frontPos.x, frontPos.y - 1)
            if (!isSolid(frontAbove) && !isBlock(frontAbove, state.blocks)) {
                placePos = frontAbove
            } else {
                return state
            }
        }

        var finalPos = placePos
        while (true) {
            val below = Position(finalPos.x, finalPos.y + 1)
            if (isSolid(below) || isBlock(below, state.blocks) || finalPos.y >= level.height - 1) {
                break
            }
            finalPos = below
        }

        val newBlocks = state.blocks.toMutableSet()
        newBlocks.add(finalPos)
        return state.copy(
            holdingBlock = false,
            blocks = newBlocks,
            moves = state.moves + 1
        )
    }

    private fun applyGravity(state: GameState): GameState {
        var currentPos = state.playerPosition

        while (true) {
            val below = Position(currentPos.x, currentPos.y + 1)
            if (isSolid(below) || isBlock(below, state.blocks) || currentPos.y >= level.height - 1) {
                break
            }
            currentPos = below
        }

        return state.copy(playerPosition = currentPos)
    }

    // Enemy AI - move towards player when within 10 blocks
    private fun moveEnemies(state: GameState): GameState {
        val newEnemies = state.enemies.map { enemy ->
            val dx = state.playerPosition.x - enemy.position.x
            val dy = state.playerPosition.y - enemy.position.y
            val distance = abs(dx) + abs(dy)

            // Only move if within 10 blocks
            if (distance > 10) return@map enemy

            val moveDir = when {
                dx > 0 -> 1
                dx < 0 -> -1
                else -> 0
            }
            if (moveDir == 0) return@map enemy

            val newFacing = if (moveDir > 0) Direction.RIGHT else Direction.LEFT
            val newX = enemy.position.x + moveDir
            val targetPos = Position(newX, enemy.position.y)

            // Check bounds
            if (newX < 0 || newX >= level.width) {
                return@map enemy.copy(facing = newFacing)
            }

            // Check collision with solid terrain or block - try to climb
            if (isSolid(targetPos) || isBlock(targetPos, state.blocks)) {
                val climbY = enemy.position.y - 1
                val climbPos = Position(newX, climbY)
                if (!isSolid(climbPos) && !isBlock(climbPos, state.blocks) && !isEnemyAt(climbPos, state.enemies, enemy)) {
                    return@map enemy.copy(position = climbPos, facing = newFacing)
                }
                return@map enemy.copy(facing = newFacing)
            }

            // Check collision with other enemies
            if (isEnemyAt(targetPos, state.enemies, enemy)) {
                return@map enemy.copy(facing = newFacing)
            }

            // Move and apply gravity
            var newPos = targetPos
            while (true) {
                val below = Position(newPos.x, newPos.y + 1)
                if (isSolid(below) || isBlock(below, state.blocks) || newPos.y >= level.height - 1) {
                    break
                }
                newPos = below
            }

            enemy.copy(position = newPos, facing = newFacing)
        }

        return state.copy(enemies = newEnemies)
    }

    private fun isEnemyAt(pos: Position, enemies: List<Enemy>, exclude: Enemy): Boolean {
        return enemies.any { it != exclude && it.position == pos }
    }

    private fun checkEnemyCollision(state: GameState): GameState {
        val collision = state.enemies.any { it.position == state.playerPosition }
        return if (collision) {
            state.copy(gameOver = true)
        } else {
            state
        }
    }

    private fun checkWin(state: GameState): GameState {
        return if (state.playerPosition == level.doorPosition) {
            state.copy(levelCompleted = true)
        } else {
            state
        }
    }

    private fun isSolid(pos: Position): Boolean {
        if (pos.x < 0 || pos.x >= level.width || pos.y < 0) return true
        return level.isSolid(pos)
    }

    private fun isBlock(pos: Position, blocks: Set<Position>): Boolean {
        return blocks.contains(pos)
    }

    fun getCellAt(x: Int, y: Int, state: GameState): CellInfo {
        val pos = Position(x, y)
        return when {
            pos == level.doorPosition -> CellInfo.Door
            pos == state.playerPosition -> CellInfo.Player(state.playerFacing, state.holdingBlock)
            state.enemies.any { it.position == pos } -> {
                val enemy = state.enemies.first { it.position == pos }
                CellInfo.Enemy(enemy.facing)
            }
            state.blocks.contains(pos) -> CellInfo.Block
            level.walls.contains(pos) -> CellInfo.Wall
            level.terrain.containsKey(pos) -> {
                val terrainType = level.terrain[pos]!!
                CellInfo.Terrain(terrainType)
            }
            else -> CellInfo.Empty
        }
    }

    fun getViewportOffset(state: GameState): Int {
        val playerX = state.playerPosition.x
        val halfViewport = level.viewportWidth / 2

        var offset = playerX - halfViewport
        offset = offset.coerceIn(0, (level.width - level.viewportWidth).coerceAtLeast(0))

        return offset
    }
}

sealed class CellInfo {
    data object Empty : CellInfo()
    data object Wall : CellInfo()
    data object Block : CellInfo()
    data object Door : CellInfo()
    data class Terrain(val type: TerrainType) : CellInfo()
    data class Player(val facing: Direction, val holdingBlock: Boolean) : CellInfo()
    data class Enemy(val facing: Direction) : CellInfo()
}
