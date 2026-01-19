package com.blockdude2.game.data

import android.content.Context
import org.json.JSONObject
import java.io.File

object LevelRepository {

    private var customLevels: List<Level> = emptyList()

    fun loadLevelsFromStorage(context: Context): List<Level> {
        val levelsDir = File(context.filesDir, "levels")
        if (!levelsDir.exists()) {
            return emptyList()
        }

        val levels = mutableListOf<Level>()
        levelsDir.listFiles()?.filter { it.extension == "json" }?.sortedBy { it.name }?.forEach { file ->
            try {
                val json = JSONObject(file.readText())
                val id = json.getInt("id")
                val name = json.optString("name", "Level $id")
                val grid = mutableListOf<String>()
                val gridArray = json.getJSONArray("grid")
                for (i in 0 until gridArray.length()) {
                    grid.add(gridArray.getString(i))
                }
                levels.add(Level.fromGrid(id, name, grid))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        customLevels = levels
        return levels
    }

    fun getAllLevels(context: Context): List<Level> {
        if (customLevels.isEmpty()) {
            loadLevelsFromStorage(context)
        }
        return customLevels.ifEmpty { getDefaultLevels() }
    }

    fun getDefaultLevels(): List<Level> = listOf(
        // Level 1 - intro with enemy
        Level.fromGrid(
            id = 1,
            name = "First Steps",
            grid = listOf(
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                 B              ",
                "         #     ####            D",
                "       ###                  ####",
                "   B P ###B  B#    #  E  B  ####",
                "GgggggGgGGGgGGGgGGGGGgggGGGGggGG"
            )
        ),
        // Level 2
        Level.fromGrid(
            id = 2,
            name = "Level 2",
            grid = listOf(
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                                ",
                "                              E ",
                "                          ######",
                " E                              ",
                "#####                           ",
                "                        # #     ",
                "             #         ## #     ",
                "  P       B  #       ######    D",
                "  B   #  BB  #   EBB ###########",
                "GgGGGGggGGGgGGGgGGGGGgGggGgGGggg"
            )
        )
    )
}
