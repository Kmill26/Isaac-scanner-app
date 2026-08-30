package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import com.example.data.model.XboxPresetScreen
import com.example.ui.components.CameraViewfinder
import com.example.ui.components.ItemQualityBadge
import com.example.ui.components.SynergyCard
import com.example.ui.theme.IsaacAlertContainer
import com.example.ui.theme.IsaacAlertDarkContainer
import com.example.ui.theme.IsaacAlertOnContainer
import com.example.ui.theme.IsaacBackground
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacGold
import com.example.ui.theme.IsaacOnSurface
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacStarGold
import com.example.ui.theme.IsaacSurface
import com.example.ui.theme.IsaacSurfaceElevated
import com.example.ui.theme.IsaacSurfaceVariant
import com.example.ui.theme.IsaacTertiaryGlow
import com.example.ui.theme.IsaacVioletSurface
import com.example.ui.theme.SynergyAntiSynergy
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraScannerScreen(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    onNavigateToRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    fun cameraGranted() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember { mutableStateOf(cameraGranted()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        permanentlyDenied = !isGranted && activity != null &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
    }

    // Re-check on resume so a grant made in Settings is picked up without a restart.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && cameraGranted()) {
                hasCameraPermission = true
                permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val presets = remember { IsaacItemDatabase.getXboxPresets() }
    var showPresetsDrawer by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(IsaacBackground)) {
        if (hasCameraPermission) {
            // Live Camera Viewfinder with TV reticle and AI controls
            CameraViewfinder(
                isScanning = uiState.isScanning,
                onCaptureFrame = { bitmap ->
                    viewModel.scanBitmap(bitmap)
                },
                onCaptureError = { message ->
                    viewModel.onCaptureError(message)
                }
            )
        } else {
            // Permission Placeholder & Preset Scanner fallback
            CameraPermissionFallback(
                permanentlyDenied = permanentlyDenied,
                onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onSelectPreset = { preset ->
                    viewModel.scanXboxPreset(preset)
                }
            )
        }

        // Top Xbox Console Presets Bar — debug-only scaffolding for testing without a real TV.
        if (BuildConfig.DEBUG) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = if (hasCameraPermission) 64.dp else 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = IsaacSurfaceElevated.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Xbox Presets",
                                tint = IsaacPrimaryCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Xbox Screen Sample Pedestals",
                                color = IsaacPrimaryCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (showPresetsDrawer) "Hide" else "Show",
                            color = IsaacPrimaryCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { showPresetsDrawer = !showPresetsDrawer }
                                .padding(4.dp)
                        )
                    }

                    if (showPresetsDrawer) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(presets, key = { it.title }) { preset ->
                                XboxPresetChip(
                                    preset = preset,
                                    onClick = { viewModel.scanXboxPreset(preset) }
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // Active Run Quick Summary Pill (Floating on top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
        ) {
            Surface(
                onClick = onNavigateToRun,
                shape = RoundedCornerShape(20.dp),
                color = IsaacPrimaryContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacPrimaryCrimson.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("current_run_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎒 Run: ${uiState.currentRunItems.size} items",
                        color = IsaacPrimaryCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Scan Result Bottom Card / Sheet
        AnimatedVisibility(
            visible = uiState.latestScanResult != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.latestScanResult?.let { result ->
                ScanResultCard(
                    result = result,
                    aiAvailable = uiState.aiAvailable,
                    isLoadingVerdict = uiState.isLoadingVerdict,
                    onAddToRun = {
                        result.matchedItem?.let { viewModel.addItemToRun(it) }
                    },
                    onGetAiVerdict = { viewModel.requestAiVerdict() },
                    onRescan = { viewModel.rescan() },
                    onDismiss = { viewModel.dismissScanResult() }
                )
            }
        }

        // Scan Error / "need a closer look" banner — dismissible, auto-clears on the next scan.
        AnimatedVisibility(
            visible = uiState.latestScanResult == null && uiState.scanErrorMessage != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ScanErrorBanner(
                message = uiState.scanErrorMessage.orEmpty(),
                onRescan = { viewModel.rescan() },
                onDismiss = { viewModel.dismissScanError() }
            )
        }
    }
}

@Composable
private fun ScanErrorBanner(
    message: String,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp)
            .border(1.dp, IsaacAlertContainer, RoundedCornerShape(20.dp))
            .testTag("scan_error_banner"),
        colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = IsaacAlertContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = message,
                        color = IsaacOnSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(IsaacBorder.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = IsaacOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRescan,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rescan_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsaacPrimaryCrimson
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rescan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ScanResultCard(
    result: ScanDetectionResult,
    aiAvailable: Boolean,
    isLoadingVerdict: Boolean,
    onAddToRun: () -> Unit,
    onGetAiVerdict: () -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = result.matchedItem
    val inCompendium = item != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp)
            .border(1.dp, IsaacBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = IsaacSurfaceElevated
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Item Name, Quality, Close button
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
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IsaacPrimaryContainer.copy(alpha = 0.6f))
                            .border(1.dp, IsaacPrimaryCrimson.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item?.iconEmoji ?: "📦",
                            fontSize = 24.sp
                        )
                    }

                    Column {
                        Text(
                            text = result.detectedName,
                            color = IsaacPrimaryCrimson,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (item != null) {
                            Text(
                                text = "'${item.quote}'",
                                color = IsaacOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(IsaacBorder.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = IsaacOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quality Badge & Item Pools Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item != null) {
                    ItemQualityBadge(quality = item.quality)
                }

                item?.itemType?.let { type ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IsaacPrimaryContainer.copy(alpha = 0.5f))
                            .border(0.5.dp, IsaacPrimaryCrimson.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = type.label,
                            color = IsaacPrimaryCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                item?.recharge?.let { charge ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IsaacPrimaryContainer.copy(alpha = 0.5f))
                            .border(0.5.dp, IsaacPrimaryCrimson.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "⚡ $charge",
                            color = IsaacPrimaryCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Real-Time Description & Verdict
            if (!inCompendium) {
                Text(
                    text = "\"${result.detectedName}\" isn't in the compendium — can't add it to a run " +
                        "or check synergies. Try reframing so the item-name banner fills the box.",
                    color = IsaacOnSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            } else {
                Text(
                    text = result.rawGeminiVerdict,
                    color = IsaacOnSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // On-demand AI "should I take this?" verdict — only when a key is configured and we
            // have not already fetched one for this result.
            val verdictAlreadyAI = result.source == com.example.data.model.ScanSource.GEMINI ||
                result.source == com.example.data.model.ScanSource.GEMINI_VERDICT
            if (aiAvailable && inCompendium && !verdictAlreadyAI) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGetAiVerdict,
                    enabled = !isLoadingVerdict,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("get_ai_verdict_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IsaacGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoadingVerdict) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = IsaacGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Asking AI…", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get AI verdict", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Anti-Synergy Alert Banner (if hazardous to current build)
            if (result.isAntiSynergyDetected) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(IsaacAlertDarkContainer.copy(alpha = 0.4f))
                        .border(1.dp, IsaacAlertContainer, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Anti Synergy Alert",
                            tint = IsaacAlertContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ANTI-SYNERGY WARNING: Hinders active run inventory!",
                            color = IsaacAlertContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Synergies with Active Run
            if (result.activeSynergiesWithRun.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SYNERGIES",
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
                Spacer(modifier = Modifier.height(6.dp))
                result.activeSynergiesWithRun.take(2).forEach { syn ->
                    SynergyCard(synergy = syn, modifier = Modifier.padding(bottom = 6.dp))
                }
            }

            // Stat Modifications Row
            if (item != null && item.stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.stats.entries.take(3).forEach { (stat, value) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IsaacPrimaryContainer.copy(alpha = 0.4f))
                                .border(0.5.dp, IsaacBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$stat: $value",
                                color = IsaacPrimaryCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Add to Run & Re-Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rescan_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = IsaacOnSurfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rescan", fontSize = 13.sp)
                }

                Button(
                    onClick = { onAddToRun() },
                    enabled = inCompendium,
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("add_item_to_run_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IsaacPrimaryCrimson
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Run",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (inCompendium) "Take Item (Add)" else "Not in compendium",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun XboxPresetChip(
    preset: XboxPresetScreen,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = IsaacPrimaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = preset.iconEmoji, fontSize = 16.sp)
            Column {
                Text(
                    text = preset.itemName,
                    color = IsaacOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = preset.roomType,
                    color = IsaacOnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectPreset: (XboxPresetScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(IsaacPrimaryContainer)
                .border(2.dp, IsaacPrimaryCrimson, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera Required",
                tint = IsaacPrimaryCrimson,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Camera Access for Xbox Scanning",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (permanentlyDenied) {
                "Camera access is turned off for this app. Enable it in Settings, then return here."
            } else {
                "Point your phone camera directly at your Xbox TV screen or monitor to automatically identify pedestal items and calculate real-time synergies."
            },
            color = IsaacOnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { if (permanentlyDenied) onOpenSettings() else onRequestPermission() },
            colors = ButtonDefaults.buttonColors(
                containerColor = IsaacPrimaryCrimson
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("request_camera_permission_button")
        ) {
            Text(
                text = if (permanentlyDenied) "Open Settings" else "Enable Camera Scanner",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Or Test with Sample Xbox Screen Pedestals:",
            color = IsaacPrimaryCrimson,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(IsaacItemDatabase.getXboxPresets().take(4), key = { it.title }) { preset ->
                Card(
                    onClick = { onSelectPreset(preset) },
                    colors = CardDefaults.cardColors(containerColor = IsaacSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = preset.iconEmoji, fontSize = 24.sp)
                            Column {
                                Text(
                                    text = preset.itemName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${preset.roomType} • ${preset.hint}",
                                    color = IsaacOnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Test",
                            tint = IsaacPrimaryCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
