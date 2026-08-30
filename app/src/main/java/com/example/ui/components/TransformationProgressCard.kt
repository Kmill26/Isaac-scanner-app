package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.TransformationProgress
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacGold
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacSurfaceElevated

@Composable
fun TransformationProgressCard(
    trans: TransformationProgress,
    modifier: Modifier = Modifier
) {
    val isComplete = trans.currentCount >= trans.requiredCount
    val accentColor = if (isComplete) IsaacPrimaryCrimson else IsaacPrimaryCrimson.copy(alpha = 0.7f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isComplete) IsaacPrimaryCrimson else IsaacBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = IsaacSurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = trans.emoji, fontSize = 20.sp)
                    Column {
                        Text(
                            text = "${trans.name} Transformation",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isComplete) "TRANSFORMATION ACTIVE!" else "Items: ${trans.itemsOwned.joinToString(", ")}",
                            color = if (isComplete) IsaacPrimaryCrimson else IsaacOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Progress indicators (3 pips)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..trans.requiredCount) {
                        val filled = i <= trans.currentCount
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (filled) IsaacPrimaryCrimson else IsaacPrimaryContainer)
                                .border(1.dp, if (filled) IsaacPrimaryCrimson else IsaacBorder, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = trans.rewardEffect,
                color = IsaacOnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    }
}
