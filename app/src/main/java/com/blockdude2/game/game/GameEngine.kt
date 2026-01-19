package com.blockdude2.game.game

import com.blockdude2.game.data.GameState
import com.blockdude2.game.data.Level
import com.blockdude2.game.data.Position

class GameEngine(private val level: Level) {

    fun createInitialState(): GameState {
        return GameState(
            playerPosition = level.playerStart,
            playerFacing = Direction.RIGHT,
            holdingBlock = false,
            blocks = level.initialBlocks.toMutableSet(),
            levelCompleted = false,
            moves = 0
        )
    }

    fun moveLeft(state: GameState): GameState {
        if (state.levelCompleted) return state
        val newState = state.copy(playerFacing = Direction.LEFT, moves = state.moves + 1)
        return tryMove(newState, -1)
    }

    fun moveRight(state: GameState): GameState {
        if (state.levelCompleted) return state
        val newState = state.copy(playerFacing = Direction.RIGHT, moves = state.moves + 1)
        return tryMove(newState, 1)
    }

    fun moveUp(state: GameState): GameState {
        if (state.levelCompleted) return state
        val dx = if (state.playerFacing == Direction.LEFT) -1 else 1
        val newState = state.copy(moves = state.moves + 1)
        return tryClimb(newState, dx)
    }

    private fun tryClimb(state: GameState, dx: Int): GameState {
        val currentPos = state.playerPosition
        val targetPos = Position(currentPos.x + dx, currentPos.y)
        val climbPos = Position(currentPos.x + dx, currentPos.y - 1)
        val aboveClimb = Position(climbPos.x, climbPos.y - 1)
        val aboveCurrent = Position(currentPos.x, currentPos.y - 1)

        // Can climb if: there's a wall/block at target level, climb position is free
        // If holding block: also need space above climb position and above current position
        val canClimb = (isWall(targetPos) || isBlock(targetPos, state.blocks)) &&
            !isWall(climbPos) && !isBlock(climbPos, state.blocks) &&
            (!state.holdingBlock || (
                !isWall(aboveClimb) && !isBlock(aboveClimb, state.blocks) &&
                !isWall(aboveCurrent) && !isBlock(aboveCurrent, state.blocks)
            ))

        if (canClimb) {
            val movedState = state.copy(playerPosition = climbPos)
            return applyGravity(movedState).let { checkWin(it) }
        }
        return state.copy(moves = state.moves - 1) // Undo move increment if can't climb
    }

    private fun tryMove(state: GameState, dx: Int): GameState {
        val currentPos = state.playerPosition
        val targetPos = Position(currentPos.x + dx, currentPos.y)
        val aboveTarget = Position(targetPos.x, targetPos.y - 1)

        // Check if target is blocked by wall - can't move horizontally into walls
        if (isWall(targetPos)) {
            return state.copy(moves = state.moves - 1) // Undo move increment if can't move
        }

        // Check if target has a block - can't move horizontally into blocks
        if (isBlock(targetPos, state.blocks)) {
            return state.copy(moves = state.moves - 1)
        }

        // If holding a block, check if there's space above the target position
        if (state.holdingBlock && (isWall(aboveTarget) || isBlock(aboveTarget, state.blocks))) {
            return state.copy(moves = state.moves - 1)
        }

        // Move horizontally and apply gravity (may fall)
        val movedState = state.copy(playerPosition = targetPos)
        return applyGravity(movedState).let { checkWin(it) }
    }

    fun pickUpOrPlace(state: GameState): GameState {
        if (state.levelCompleted) return state

        return if (state.holdingBlock) {
            placeBlock(state)
        } else {
            pickUpBlock(state)
        }
    }

    private fun pickUpBlock(state: GameState): GameState {
        val dx = if (state.playerFacing == Direction.LEFT) -1 else 1
        val frontPos = Position(state.playerPosition.x + dx, state.playerPosition.y)
        val aboveFront = Position(frontPos.x, frontPos.y - 1)
        val abovePlayer = Position(state.playerPosition.x, state.playerPosition.y - 1)

        // Can pick up if: block is in front, nothing above it, nothing above player
        if (isBlock(frontPos, state.blocks) &&
            !isBlock(aboveFront, state.blocks) && !isWall(aboveFront) &&
            !isBlock(abovePlayer, state.blocks) && !isWall(abovePlayer)
        ) {
            val newBlocks = state.blocks.toMutableSet()
            newBlocks.remove(frontPos)
            return state.copy(
                holdingBlock = true,
                blocks = newBlocks,
                moves = state.moves + 1
            )
        }

        // Try to pick up block below in front (if standing on edge)
        val belowFront = Position(frontPos.x, frontPos.y + 1)
        if (isBlock(belowFront, state.blocks) &&
            !isBlock(frontPos, state.blocks) && !isWall(frontPos) &&
            !isBlock(abovePlayer, state.blocks) && !isWall(abovePlayer)
        ) {
            val aboveBelowFront = frontPos
            if (!isBlock(aboveBelowFront, state.blocks) && !isWall(aboveBelowFront)) {
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

        // Find where the block would land
        var placePos = frontPos

        // If front position is blocked, try placing on top of it
        if (isWall(frontPos) || isBlock(frontPos, state.blocks)) {
            val frontAbove = Position(frontPos.x, frontPos.y - 1)
            // Only need the placement spot to be clear
            if (!isWall(frontAbove) && !isBlock(frontAbove, state.blocks)) {
                placePos = frontAbove
            } else {
                return state
            }
        }

        // Apply gravity to placed block
        var finalPos = placePos
        while (true) {
            val below = Position(finalPos.x, finalPos.y + 1)
            if (isWall(below) || isBlock(below, state.blocks) || finalPos.y >= level.height - 1) {
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
            if (isWall(below) || isBlock(below, state.blocks) || currentPos.y >= level.height - 1) {
                break
            }
            currentPos = below
        }

        return state.copy(playerPosition = currentPos)
    }

    private fun checkWin(state: GameState): GameState {
        return if (state.playerPosition == level.doorPosition) {
            state.copy(levelCompleted = true)
        } else {
            state
        }
    }

    private fun isWall(pos: Position): Boolean {
        return level.walls.contains(pos) || pos.x < 0 || pos.x >= level.width || pos.y < 0
    }

    private fun isBlock(pos: Position, blocks: Set<Position>): Boolean {
        return blocks.contains(pos)
    }

    fun getCellAt(x: Int, y: Int, state: GameState): CellInfo {
        val pos = Position(x, y)
        return when {
            pos == level.doorPosition -> CellInfo.Door
            pos == state.playerPosition -> CellInfo.Player(state.playerFacing, state.holdingBlock)
            state.blocks.contains(pos) -> CellInfo.Block
            level.walls.contains(pos) -> CellInfo.Wall
            else -> CellInfo.Empty
        }
    }

    // Calculate viewport offset for scrolling - keeps player centered when possible
    fun getViewportOffset(state: GameState): Int {
        val playerX = state.playerPosition.x
        val halfViewport = level.viewportWidth / 2

        // Center the player in the viewport
        var offset = playerX - halfViewport

        // Clamp to level bounds
        offset = offset.coerceIn(0, (level.width - level.viewportWidth).coerceAtLeast(0))

        return offset
    }
}

sealed class CellInfo {
    data object Empty : CellInfo()
    data object Wall : CellInfo()
    data object Block : CellInfo()
    data object Door : CellInfo()
    data class Player(val facing: Direction, val holdingBlock: Boolean) : CellInfo()
}
