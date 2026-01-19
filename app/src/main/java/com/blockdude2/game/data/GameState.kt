package com.blockdude2.game.data

import com.blockdude2.game.game.Direction

data class Position(val x: Int, val y: Int)

data class Enemy(
    val position: Position,
    val facing: Direction = Direction.LEFT
)

data class GameState(
    val playerPosition: Position,
    val playerFacing: Direction = Direction.RIGHT,
    val holdingBlock: Boolean = false,
    val blocks: Set<Position>,
    val enemies: List<Enemy>,
    val squishedEnemyPositions: List<Position> = emptyList(),
    val levelCompleted: Boolean = false,
    val gameOver: Boolean = false,
    val moves: Int = 0
)

enum class CellType {
    EMPTY,
    WALL,
    BLOCK,
    DOOR,
    GRASS,
    ENEMY
}
