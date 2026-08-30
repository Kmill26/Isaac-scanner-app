package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IsaacItem
import com.example.ui.components.ItemQualityBadge
import com.example.ui.components.ItemSynergyDetailRow
import com.example.ui.theme.IsaacAlertContainer
import com.example.ui.theme.IsaacAlertOnContainer
import com.example.ui.theme.IsaacBackground
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacGold
import com.example.ui.theme.IsaacOnSurface
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacSurface
import com.example.ui.theme.IsaacSurfaceElevated
import com.example.ui.theme.IsaacSurfaceVariant
import com.example.ui.theme.IsaacTertiaryGlow
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompendiumScreen(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    modifier: Modifier = Modifier
) {
    val items = viewModel.getFilteredCompendiumItems()
    var selectedItemForDetail by remember { mutableStateOf<IsaacItem?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Debounced search: type into local state, push to the ViewModel after a pause.
    var queryInput by remember { mutableStateOf(uiState.compendiumQuery) }
    LaunchedEffect(queryInput) {
        kotlinx.coroutines.delay(250)
        if (queryInput != uiState.compendiumQuery) {
            viewModel.updateCompendiumFilters(query = queryInput)
        }
    }

    val qualityFilters = listOf(null, 4, 3, 2, 1, 0)
    val poolFilters = listOf(null, "Treasure", "Devil", "Angel", "Secret", "Shop", "Boss")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IsaacBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Row
        Text(
            text = "Isaac Item Compendium",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${items.size} Items Available with Real-Time Synergy Data",
            color = IsaacOnSurfaceVariant,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = queryInput,
            onValueChange = { queryInput = it },
            placeholder = { Text("Search by item name, quote, or effect...", color = IsaacOnSurfaceVariant, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = IsaacPrimaryCrimson
                )
            },
            trailingIcon = {
                if (queryInput.isNotEmpty()) {
                    IconButton(onClick = {
                        queryInput = ""
                        viewModel.updateCompendiumFilters(query = "")
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = IsaacOnSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compendium_search_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IsaacPrimaryCrimson,
                unfocusedBorderColor = IsaacBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quality Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(qualityFilters, key = { it?.toString() ?: "all" }) { q ->
                val selected = uiState.compendiumQualityFilter == q
                Surface(
                    onClick = { viewModel.updateCompendiumFilters(quality = q) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) IsaacPrimaryContainer else IsaacSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) IsaacPrimaryCrimson else IsaacBorder)
                ) {
                    Text(
                        text = if (q == null) "All Tiers" else "Tier $q",
                        color = if (selected) IsaacPrimaryCrimson else IsaacOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pool Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(poolFilters, key = { it ?: "all" }) { pool ->
                val selected = uiState.compendiumPoolFilter == pool
                Surface(
                    onClick = { viewModel.updateCompendiumFilters(pool = pool) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) IsaacPrimaryContainer.copy(alpha = 0.7f) else IsaacSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) IsaacPrimaryCrimson else IsaacBorder)
                ) {
                    Text(
                        text = pool ?: "All Pools",
                        color = if (selected) IsaacPrimaryCrimson else IsaacOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Items List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (items.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items match these filters. Clear the search or filter chips to see the full compendium.",
                            color = IsaacOnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(items, key = { it.id }, contentType = { "item" }) { item ->
                    CompendiumItemCard(
                        item = item,
                        isInRun = uiState.currentRunItems.any { it.id == item.id },
                        onClick = { selectedItemForDetail = item },
                        onAddToRun = { viewModel.addItemToRun(item) }
                    )
                }
            }
        }
    }

    // Item Detail Bottom Sheet
    selectedItemForDetail?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selectedItemForDetail = null },
            sheetState = sheetState,
            containerColor = IsaacSurfaceElevated
        ) {
            ItemDetailSheetContent(
                item = item,
                isInRun = uiState.currentRunItems.any { it.id == item.id },
                onAddToRun = {
                    viewModel.addItemToRun(item)
                    selectedItemForDetail = null
                },
                onDismiss = { selectedItemForDetail = null }
            )
        }
    }
}

@Composable
private fun CompendiumItemCard(
    item: IsaacItem,
    isInRun: Boolean,
    onClick: () -> Unit,
    onAddToRun: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, IsaacBorder, RoundedCornerShape(16.dp))
            .testTag("compendium_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IsaacPrimaryContainer.copy(alpha = 0.5f))
                        .border(0.5.dp, IsaacBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.iconEmoji, fontSize = 24.sp)
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = IsaacPrimaryCrimson,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        ItemQualityBadge(quality = item.quality, showStars = false)
                    }

                    Text(
                        text = "'${item.quote}'",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${item.itemPools.joinToString(", ")} • ${item.itemType.label}",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Add to Run Action
            if (isInRun) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IsaacPrimaryContainer)
                        .border(1.dp, IsaacPrimaryCrimson.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "In Run",
                        color = IsaacPrimaryCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(
                    onClick = onAddToRun,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(IsaacPrimaryCrimson)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Run",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemDetailSheetContent(
    item: IsaacItem,
    isInRun: Boolean,
    onAddToRun: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IsaacPrimaryContainer)
                        .border(1.dp, IsaacPrimaryCrimson.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.iconEmoji, fontSize = 32.sp)
                }
                Column {
                    Text(
                        text = item.name,
                        color = IsaacPrimaryCrimson,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "'${item.quote}'",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            ItemQualityBadge(quality = item.quality)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tags Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(IsaacPrimaryContainer.copy(alpha = 0.5f))
                    .border(0.5.dp, IsaacBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = item.itemType.label, color = IsaacPrimaryCrimson, fontSize = 11.sp)
            }

            item.recharge?.let { charge ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(IsaacPrimaryContainer.copy(alpha = 0.5f))
                        .border(0.5.dp, IsaacBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "⚡ $charge", color = IsaacPrimaryCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            item.transformations.forEach { trans ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(IsaacPrimaryContainer.copy(alpha = 0.5f))
                        .border(0.5.dp, IsaacBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "🧬 $trans", color = IsaacPrimaryCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = item.description,
            color = IsaacOnSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        // Synergies List
        if (item.synergies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SYNERGIES & INTERACTIONS (${item.synergies.size})",
                    color = IsaacPrimaryCrimson,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                HorizontalDivider(
                    color = IsaacBorder,
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(item.synergies, key = { it.partnerItemName }) { syn ->
                    ItemSynergyDetailRow(synergy = syn)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Button
        Button(
            onClick = onAddToRun,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInRun) IsaacSurfaceVariant else IsaacPrimaryCrimson,
                contentColor = if (isInRun) IsaacOnSurfaceVariant else Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isInRun) "Item Already in Active Run" else "Add to Active Xbox Run",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
