package com.blockdude2.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.blockdude2.game.ui.theme.*

enum class LevelStatus {
    LOCKED,
    UNLOCKED,
    COMPLETED
}

@Composable
fun LevelCard(
    levelNumber: Int,
    status: LevelStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        LevelStatus.LOCKED -> LockedColor.copy(alpha = 0.3f)
        LevelStatus.UNLOCKED -> SurfaceColor
        LevelStatus.COMPLETED -> CompletedColor.copy(alpha = 0.2f)
    }

    val borderColor = when (status) {
        LevelStatus.LOCKED -> LockedColor
        LevelStatus.UNLOCKED -> UnlockedColor
        LevelStatus.COMPLETED -> CompletedColor
    }

    val textColor = when (status) {
        LevelStatus.LOCKED -> LockedColor
        LevelStatus.UNLOCKED -> TextWhite
        LevelStatus.COMPLETED -> CompletedColor
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(scaledDp(10)))
            .background(backgroundColor)
            .border(scaledDp(2), borderColor, RoundedCornerShape(scaledDp(10)))
            .then(
                if (status != LevelStatus.LOCKED) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (status == LevelStatus.LOCKED) "?" else "$levelNumber",
                color = textColor,
                fontSize = scaledSp(20),
                fontWeight = FontWeight.Bold
            )

            if (status == LevelStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(scaledDp(2)))
                Text(
                    text = "DONE",
                    color = CompletedColor,
                    fontSize = scaledSp(6),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
