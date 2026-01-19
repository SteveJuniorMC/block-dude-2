package com.blockdude2.game.data

enum class TerrainType {
    GRASS,
    GRASS_VARIANT
}

data class Level(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val walls: Set<Position>,
    val terrain: Map<Position, TerrainType>,
    val initialBlocks: Set<Position>,
    val initialEnemies: Set<Position>,
    val playerStart: Position,
    val doorPosition: Position
) {
    // Viewport width in cells (what's visible on screen at once)
    val viewportWidth: Int = 16

    // Check if position is solid (wall or terrain)
    fun isSolid(pos: Position): Boolean {
        return walls.contains(pos) || terrain.containsKey(pos)
    }

    companion object {
        fun fromString(id: Int, name: String, levelString: String): Level {
            val lines = levelString.trimIndent().lines()
            return parseGrid(id, name, lines)
        }

        fun fromGrid(id: Int, name: String, grid: List<String>): Level {
            return parseGrid(id, name, grid)
        }

        private fun parseGrid(id: Int, name: String, grid: List<String>): Level {
            val height = grid.size
            val width = grid.maxOfOrNull { it.length } ?: 0

            val walls = mutableSetOf<Position>()
            val terrain = mutableMapOf<Position, TerrainType>()
            val blocks = mutableSetOf<Position>()
            val enemies = mutableSetOf<Position>()
            var playerStart = Position(0, 0)
            var doorPosition = Position(0, 0)

            grid.forEachIndexed { y, line ->
                line.forEachIndexed { x, char ->
                    val pos = Position(x, y)
                    when (char) {
                        '#' -> walls.add(pos)
                        'G' -> terrain[pos] = TerrainType.GRASS
                        'g' -> terrain[pos] = TerrainType.GRASS_VARIANT
                        'B' -> blocks.add(pos)
                        'P' -> playerStart = pos
                        'D' -> doorPosition = pos
                        'E' -> enemies.add(pos)
                    }
                }
            }

            return Level(
                id = id,
                name = name,
                width = width,
                height = height,
                walls = walls,
                terrain = terrain,
                initialBlocks = blocks,
                initialEnemies = enemies,
                playerStart = playerStart,
                doorPosition = doorPosition
            )
        }
    }
}
