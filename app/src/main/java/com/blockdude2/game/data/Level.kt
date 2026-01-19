package com.blockdude2.game.data

data class Level(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val walls: Set<Position>,
    val initialBlocks: Set<Position>,
    val playerStart: Position,
    val doorPosition: Position
) {
    // Viewport width in cells (what's visible on screen at once)
    val viewportWidth: Int = 16

    companion object {
        fun fromString(id: Int, name: String, levelString: String): Level {
            val lines = levelString.trimIndent().lines()
            val height = lines.size
            val width = lines.maxOfOrNull { it.length } ?: 0

            val walls = mutableSetOf<Position>()
            val blocks = mutableSetOf<Position>()
            var playerStart = Position(0, 0)
            var doorPosition = Position(0, 0)

            lines.forEachIndexed { y, line ->
                line.forEachIndexed { x, char ->
                    when (char) {
                        '#' -> walls.add(Position(x, y))
                        'B' -> blocks.add(Position(x, y))
                        'P' -> playerStart = Position(x, y)
                        'D' -> doorPosition = Position(x, y)
                    }
                }
            }

            return Level(
                id = id,
                name = name,
                width = width,
                height = height,
                walls = walls,
                initialBlocks = blocks,
                playerStart = playerStart,
                doorPosition = doorPosition
            )
        }

        fun fromGrid(id: Int, name: String, grid: List<String>): Level {
            val height = grid.size
            val width = grid.maxOfOrNull { it.length } ?: 0

            val walls = mutableSetOf<Position>()
            val blocks = mutableSetOf<Position>()
            var playerStart = Position(0, 0)
            var doorPosition = Position(0, 0)

            grid.forEachIndexed { y, line ->
                line.forEachIndexed { x, char ->
                    when (char) {
                        '#' -> walls.add(Position(x, y))
                        'B' -> blocks.add(Position(x, y))
                        'P' -> playerStart = Position(x, y)
                        'D' -> doorPosition = Position(x, y)
                    }
                }
            }

            return Level(
                id = id,
                name = name,
                width = width,
                height = height,
                walls = walls,
                initialBlocks = blocks,
                playerStart = playerStart,
                doorPosition = doorPosition
            )
        }
    }
}
