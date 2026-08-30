package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QualityTier0
import com.example.ui.theme.QualityTier1
import com.example.ui.theme.QualityTier2
import com.example.ui.theme.QualityTier3
import com.example.ui.theme.QualityTier4

@Composable
fun ItemQualityBadge(
    quality: Int,
    modifier: Modifier = Modifier,
    showStars: Boolean = true
) {
    val (tierColor, label) = when (quality) {
        4 -> Pair(QualityTier4, "Tier 4 • God Tier")
        3 -> Pair(QualityTier3, "Tier 3 • Great")
        2 -> Pair(QualityTier2, "Tier 2 • Decent")
        1 -> Pair(QualityTier1, "Tier 1 • Mediocre")
        else -> Pair(QualityTier0, "Tier 0 • Trash")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tierColor.copy(alpha = 0.15f))
            .border(1.dp, tierColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showStars) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    val count = if (quality == 0) 1 else quality
                    repeat(count) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Quality Star",
                            tint = tierColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Text(
                text = label,
                color = tierColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
