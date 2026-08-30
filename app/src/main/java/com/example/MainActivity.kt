package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CompendiumScreen
import com.example.ui.screens.CurrentRunScreen
import com.example.ui.screens.RunHistoryScreen
import com.example.ui.theme.IsaacAlertContainer
import com.example.ui.theme.IsaacAlertOnContainer
import com.example.ui.theme.IsaacBackground
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacOnPrimaryContainer
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacSurface
import com.example.ui.theme.IsaacSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScannerViewModel

enum class AppDestination(val title: String, val icon: ImageVector, val tag: String) {
    CAMERA_SCANNER("TV Scanner", Icons.Default.CameraAlt, "nav_camera_scanner"),
    CURRENT_RUN("Active Run", Icons.Default.Shield, "nav_current_run"),
    COMPENDIUM("Compendium", Icons.Default.AutoStories, "nav_compendium"),
    HISTORY("History", Icons.Default.History, "nav_history")
}

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                IsaacAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun IsaacAppRoot(viewModel: ScannerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedScreenIndex by rememberSaveable { mutableIntStateOf(0) }

    // System back returns to the scanner instead of exiting the app.
    BackHandler(enabled = selectedScreenIndex != 0) { selectedScreenIndex = 0 }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = IsaacBackground,
        bottomBar = {
            NavigationBar(
                containerColor = IsaacSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, IsaacBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AppDestination.entries.forEachIndexed { index, destination ->
                    val isSelected = selectedScreenIndex == index

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedScreenIndex = index },
                        icon = {
                            if (destination == AppDestination.CURRENT_RUN && uiState.currentRunItems.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = IsaacAlertContainer,
                                            contentColor = IsaacAlertOnContainer
                                        ) {
                                            Text("${uiState.currentRunItems.size}", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            // Light pink on the deep-maroon indicator pill — crimson-on-maroon was ~3:1.
                            selectedIconColor = IsaacOnPrimaryContainer,
                            selectedTextColor = IsaacOnPrimaryContainer,
                            indicatorColor = IsaacPrimaryContainer,
                            unselectedIconColor = IsaacOnSurfaceVariant,
                            unselectedTextColor = IsaacOnSurfaceVariant
                        ),
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedScreenIndex) {
                0 -> CameraScannerScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onNavigateToRun = { selectedScreenIndex = 1 }
                )
                1 -> CurrentRunScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                2 -> CompendiumScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                3 -> RunHistoryScreen(
                    viewModel = viewModel,
                    onLoadRun = { selectedScreenIndex = 1 }
                )
            }
        }
    }
}
