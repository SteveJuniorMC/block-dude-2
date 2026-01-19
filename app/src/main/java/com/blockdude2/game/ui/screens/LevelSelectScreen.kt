package com.blockdude2.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.blockdude2.game.data.Level
import com.blockdude2.game.ui.components.LevelCard
import com.blockdude2.game.ui.components.LevelStatus
import com.blockdude2.game.ui.components.ScaledContainer
import com.blockdude2.game.ui.components.scaledDp
import com.blockdude2.game.ui.components.scaledSp
import com.blockdude2.game.ui.theme.*

@Composable
fun LevelSelectScreen(
    levels: List<Level>,
    completedLevels: Set<Int>,
    onLevelClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    ScaledContainer(backgroundColor = DarkBackground) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceColor)
                    .padding(scaledDp(12)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(scaledDp(32))
                        .clip(RoundedCornerShape(scaledDp(6)))
                        .background(PrimaryBlue)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "<", color = Color.White, fontSize = scaledSp(14))
                }

                Spacer(modifier = Modifier.width(scaledDp(12)))

                Text(
                    text = "SELECT LEVEL",
                    color = TextWhite,
                    fontSize = scaledSp(16),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = scaledSp(1)
                )
            }

            // Progress info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scaledDp(12), vertical = scaledDp(8)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Completed: ${completedLevels.size}/${levels.size}",
                    color = CompletedColor,
                    fontSize = scaledSp(10)
                )
                Text(
                    text = "Tap to play",
                    color = TextWhite.copy(alpha = 0.5f),
                    fontSize = scaledSp(10)
                )
            }

            // Level grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(scaledDp(12)),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(10)),
                verticalArrangement = Arrangement.spacedBy(scaledDp(10)),
                modifier = Modifier.fillMaxSize()
            ) {
                items(levels) { level ->
                    val status = when {
                        completedLevels.contains(level.id) -> LevelStatus.COMPLETED
                        level.id == 1 || completedLevels.contains(level.id - 1) -> LevelStatus.UNLOCKED
                        else -> LevelStatus.LOCKED
                    }

                    LevelCard(
                        levelNumber = level.id,
                        status = status,
                        onClick = { onLevelClick(level.id) }
                    )
                }
            }
        }
    }
}
