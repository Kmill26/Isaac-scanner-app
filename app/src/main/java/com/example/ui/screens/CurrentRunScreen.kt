package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.SynergyRating
import com.example.ui.components.ItemQualityBadge
import com.example.ui.components.SynergyCard
import com.example.ui.components.TransformationProgressCard
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
import com.example.ui.theme.SynergyAntiSynergy
import com.example.ui.theme.SynergyGodTier
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentRunScreen(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    modifier: Modifier = Modifier
) {
    val activeSynergies by viewModel.activeRunSynergies.collectAsStateWithLifecycle()
    val activeTransformations by viewModel.activeTransformations.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var runTitleInput by remember { mutableStateOf("") }
    var characterInput by remember { mutableStateOf("Isaac") }
    var notesInput by remember { mutableStateOf("") }
    var isWinRun by remember { mutableStateOf(false) }

    var testCandidateQuery by remember { mutableStateOf("") }
    var showCandidatePicker by remember { mutableStateOf(false) }
    var selectedCandidateToTest by remember { mutableStateOf<IsaacItem?>(null) }

    val characters = listOf("Isaac", "Azazel", "Eden", "The Lost", "Judas", "Cain", "Tainted Lost", "Bethany")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IsaacBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 100.dp)
    ) {
        // Header Row: Title, Run Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Xbox Run",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.currentRunItems.size} Items Collected • ${activeSynergies.size} Active Synergies",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IsaacPrimaryContainer)
                            .border(1.dp, IsaacBorder, CircleShape)
                            .testTag("save_run_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Save Run",
                            tint = IsaacPrimaryCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearRun() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IsaacSurfaceElevated)
                            .border(1.dp, IsaacBorder, CircleShape)
                            .testTag("clear_run_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Run",
                            tint = IsaacOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Resume-a-persisted-run banner (cold start with a saved current run)
        if (uiState.resumableRunCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, IsaacPrimaryCrimson.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("resume_run_banner"),
                    colors = CardDefaults.cardColors(containerColor = IsaacPrimaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Resume last run?",
                            color = IsaacPrimaryCrimson,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You had ${uiState.resumableRunCount} item${if (uiState.resumableRunCount == 1) "" else "s"} in progress when the app last closed.",
                            color = IsaacOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.resumePersistedRun() },
                                modifier = Modifier.weight(1f).testTag("resume_run_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IsaacPrimaryCrimson
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Resume", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.discardPersistedRun() },
                                modifier = Modifier.weight(1f).testTag("start_fresh_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IsaacOnSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Start fresh", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Active Run Inventory Horizontal / Grid List
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IsaacBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🎒 INVENTORY ITEMS",
                        color = IsaacGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.currentRunItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items yet. Scan Xbox TV items with the camera or use the simulator below!",
                                color = IsaacOnSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.currentRunItems, key = { it.id }) { item ->
                                RunItemChip(
                                    item = item,
                                    onRemove = { viewModel.removeItemFromRun(item) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transformation Progress
        if (activeTransformations.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🧬 TRANSFORMATIONS PROGRESS",
                        color = IsaacGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    activeTransformations.forEach { trans ->
                        TransformationProgressCard(trans = trans)
                    }
                }
            }
        }

        // "Simulate Item Synergy Before Grabbing on Xbox" Tool
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IsaacBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Test Synergy",
                            tint = IsaacPrimaryCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "🔮 Simulate Next Pedestal Item",
                            color = IsaacPrimaryCrimson,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Looking at an item on Xbox? Test its synergies with your current build before picking it up:",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Candidate Item Picker Row
                    OutlinedTextField(
                        value = testCandidateQuery,
                        onValueChange = {
                            testCandidateQuery = it
                            showCandidatePicker = true
                        },
                        placeholder = { Text("Search item to test (e.g. Brimstone, Ipecac)...", fontSize = 12.sp, color = IsaacOnSurfaceVariant) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_item_search"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IsaacPrimaryCrimson,
                            unfocusedBorderColor = IsaacBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Suggested Candidate Chips
                    if (showCandidatePicker || testCandidateQuery.isNotBlank()) {
                        val candidates = IsaacItemDatabase.items.filter {
                            testCandidateQuery.isBlank() || it.name.contains(testCandidateQuery, ignoreCase = true)
                        }.take(6)

                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(candidates, key = { it.id }) { cand ->
                                Surface(
                                    onClick = {
                                        selectedCandidateToTest = cand
                                        testCandidateQuery = cand.name
                                        showCandidatePicker = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = IsaacPrimaryContainer.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(cand.iconEmoji, fontSize = 12.sp)
                                        Text(cand.name, color = IsaacPrimaryCrimson, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Test Evaluation Results Card
                    selectedCandidateToTest?.let { candidate ->
                        val testSynergies = IsaacItemDatabase.calculateSynergies(candidate, uiState.currentRunItems)
                        val hasAnti = testSynergies.any { it.rating == SynergyRating.ANTI_SYNERGY }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(IsaacBackground)
                                .border(1.dp, if (hasAnti) IsaacAlertContainer else IsaacBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(candidate.iconEmoji, fontSize = 20.sp)
                                        Text(
                                            text = candidate.name,
                                            color = IsaacPrimaryCrimson,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    ItemQualityBadge(quality = candidate.quality)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = candidate.description,
                                    color = IsaacOnSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                if (hasAnti) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "⚠️ WARNING: Picking this up causes dangerous anti-synergies with your run!",
                                        color = IsaacAlertContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                if (testSynergies.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    testSynergies.forEach { syn ->
                                        SynergyCard(synergy = syn, modifier = Modifier.padding(bottom = 4.dp))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No direct special synergies with current items, but safe to pick up!",
                                        color = IsaacOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.addItemToRun(candidate)
                                        selectedCandidateToTest = null
                                        testCandidateQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = IsaacPrimaryCrimson
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add ${candidate.name} to Current Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Synergies Matrix List
        item {
            Text(
                text = "⚡ Active Build Synergies (${activeSynergies.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeSynergies.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add more items to your inventory to trigger synergies!",
                            color = IsaacOnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeSynergies, key = { it.itemA + "+" + it.itemB }) { synergy ->
                SynergyCard(synergy = synergy)
            }
        }
    }

    // Save Run Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Save Xbox Run",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Character:",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(characters, key = { it }) { char ->
                            Surface(
                                onClick = { characterInput = char },
                                shape = RoundedCornerShape(8.dp),
                                color = if (characterInput == char) IsaacPrimaryContainer else IsaacSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (characterInput == char) IsaacPrimaryCrimson else IsaacBorder)
                            ) {
                                Text(
                                    text = char,
                                    color = if (characterInput == char) IsaacPrimaryCrimson else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = runTitleInput,
                        onValueChange = { runTitleInput = it },
                        label = { Text("Run Title (Optional)") },
                        placeholder = { Text("e.g. Hard Mode Eden Victory") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IsaacPrimaryCrimson,
                            unfocusedBorderColor = IsaacBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Run Notes (Seed / Final Boss)") },
                        placeholder = { Text("e.g. Defeated Mother with Brimstone") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IsaacPrimaryCrimson,
                            unfocusedBorderColor = IsaacBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentRun(
                            title = runTitleInput,
                            character = characterInput,
                            notes = notesInput,
                            isWin = isWinRun
                        )
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IsaacPrimaryCrimson
                    )
                ) {
                    Text("Save to History", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = IsaacOnSurfaceVariant)
                }
            },
            containerColor = IsaacSurfaceElevated
        )
    }
}

@Composable
private fun RunItemChip(
    item: IsaacItem,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = IsaacPrimaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
        modifier = Modifier.width(130.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemQualityBadge(quality = item.quality, showStars = false)
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${item.name} from run",
                        tint = IsaacOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.iconEmoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                color = IsaacPrimaryCrimson,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = item.quote,
                color = IsaacOnSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
