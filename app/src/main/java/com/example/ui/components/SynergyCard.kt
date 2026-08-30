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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.ActiveRunSynergy
import com.example.data.model.SynergyInfo
import com.example.data.model.SynergyRating
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacSurfaceElevated
import com.example.ui.theme.SynergyAntiSynergy
import com.example.ui.theme.SynergyExcellent
import com.example.ui.theme.SynergyGodTier
import com.example.ui.theme.SynergyGood
import com.example.ui.theme.SynergySituational

@Composable
fun SynergyCard(
    synergy: ActiveRunSynergy,
    modifier: Modifier = Modifier
) {
    val ratingColor = getSynergyColor(synergy.rating)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ratingColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${synergy.itemA} + ${synergy.itemB}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ratingColor.copy(alpha = 0.15f))
                        .border(1.dp, ratingColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = synergy.rating.emoji,
                            fontSize = 11.sp
                        )
                        Text(
                            text = synergy.rating.title,
                            color = ratingColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = synergy.title,
                color = ratingColor,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = synergy.description,
                color = IsaacOnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ItemSynergyDetailRow(
    synergy: SynergyInfo,
    modifier: Modifier = Modifier
) {
    val ratingColor = getSynergyColor(synergy.rating)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IsaacPrimaryContainer.copy(alpha = 0.35f))
            .border(0.5.dp, ratingColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+ ${synergy.partnerItemName}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                text = "${synergy.rating.emoji} ${synergy.rating.title}",
                color = ratingColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = synergy.title,
            color = ratingColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
        Text(
            text = synergy.description,
            color = IsaacOnSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

fun getSynergyColor(rating: SynergyRating): Color {
    return when (rating) {
        SynergyRating.GOD_TIER -> SynergyGodTier
        SynergyRating.EXCELLENT -> SynergyExcellent
        SynergyRating.GOOD -> SynergyGood
        SynergyRating.SITUATIONAL -> SynergySituational
        SynergyRating.ANTI_SYNERGY -> SynergyAntiSynergy
    }
}
