package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.RunEntity
import com.example.data.db.ScanEntity
import com.example.ui.components.ItemQualityBadge
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
import com.example.ui.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunHistoryScreen(
    viewModel: ScannerViewModel,
    onLoadRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedRuns by viewModel.savedRuns.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Saved Runs (${savedRuns.size})", "Camera Scans (${scanHistory.size})")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IsaacBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "History & Archives",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Past Xbox Run Builds and AI Camera Telemetry",
                    color = IsaacOnSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (selectedTab == 1 && scanHistory.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear History", tint = IsaacOnSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = IsaacSurfaceElevated,
            contentColor = IsaacGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = IsaacGold
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) IsaacGold else IsaacOnSurfaceVariant,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Saved Runs List
            if (savedRuns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved runs yet. Build a run and tap the bookmark icon on the Current Run tab!",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(savedRuns, key = { it.id }) { run ->
                        SavedRunCard(
                            run = run,
                            onLoad = {
                                viewModel.loadRun(run)
                                onLoadRun()
                            },
                            onDelete = { viewModel.deleteRun(run.id) }
                        )
                    }
                }
            }
        } else {
            // Camera Scan Telemetry List
            if (scanHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No camera scans recorded yet. Aim at your TV and scan items!",
                        color = IsaacOnSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(scanHistory, key = { it.id }) { scan ->
                        ScanHistoryCard(scan = scan)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRunCard(
    run: RunEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val itemsList = run.itemsCsv.split(",").filter { it.isNotBlank() }
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(run.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, IsaacBorder, RoundedCornerShape(16.dp))
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IsaacPrimaryContainer)
                            .border(0.5.dp, IsaacPrimaryCrimson.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = run.character,
                            color = IsaacPrimaryCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = run.runTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = IsaacOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Items (${itemsList.size}): ${itemsList.joinToString(", ")}",
                color = IsaacOnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (run.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${run.notes}",
                    color = IsaacPrimaryCrimson,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    color = IsaacOnSurfaceVariant,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onLoad,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IsaacPrimaryCrimson
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Load",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Load Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryCard(
    scan: ScanEntity
) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(scan.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, IsaacBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = scan.itemName,
                        color = IsaacPrimaryCrimson,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    ItemQualityBadge(quality = scan.itemQuality, showStars = false)
                }

                Text(
                    text = "${(scan.confidence * 100).toInt()}% Conf",
                    color = IsaacPrimaryCrimson,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = scan.verdict,
                color = IsaacOnSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = dateStr,
                color = IsaacOnSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}
