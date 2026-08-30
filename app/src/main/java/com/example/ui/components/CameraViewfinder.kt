package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.IsaacAlertContainer
import com.example.ui.theme.IsaacAlertOnContainer
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacGold
import com.example.ui.theme.IsaacOnBackground
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacSurface
import com.example.ui.theme.IsaacSurfaceElevated
import com.example.ui.theme.IsaacTertiaryGlow
import com.example.ui.theme.IsaacVioletSurface
import java.util.concurrent.Executors

@Composable
fun CameraViewfinder(
    isScanning: Boolean,
    isAutoScanEnabled: Boolean,
    torchEnabled: Boolean,
    onTorchToggle: (Boolean) -> Unit,
    onCaptureFrame: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var showZoomSlider by remember { mutableStateOf(false) }

    // Animated Scan Line
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    // Sync torch with camera
    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // Sync zoom with camera
    LaunchedEffect(zoomLevel, camera) {
        camera?.cameraControl?.setLinearZoom((zoomLevel - 1.0f) / 4.0f)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // CameraX Preview View
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Scanner Overlay Canvas (TV / Pedestal Frame Reticle)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate viewfinder box dimensions (optimized for TV screen / pedestals)
            val boxWidth = canvasWidth * 0.78f
            val boxHeight = boxWidth * 0.95f
            val left = (canvasWidth - boxWidth) / 2f
            val top = (canvasHeight - boxHeight) / 2f - 40f
            val right = left + boxWidth
            val bottom = top + boxHeight
            val centerX = left + boxWidth / 2f
            val centerY = top + boxHeight / 2f

            // Dark semi-transparent scrim with rounded cutout
            drawRect(
                color = Color(0xC40E0A0D),
                size = size
            )

            // Clear the viewport area
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Radial gradient glow in the center of reticle (Isaac Item Pedestal Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IsaacGold.copy(alpha = 0.25f),
                        IsaacPrimaryCrimson.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = boxWidth * 0.5f
                ),
                radius = boxWidth * 0.5f,
                center = Offset(centerX, centerY)
            )

            // Inner subtle border (Isaac Basement Pedestal Frame)
            drawRoundRect(
                color = IsaacGold.copy(alpha = 0.4f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Corner bracket guides (Isaac Gold & Brimstone Crimson)
            val cornerLength = 36.dp.toPx()
            val strokeW = 3.5.dp.toPx()
            val bracketColor = IsaacGold

            // Top-Left
            drawLine(bracketColor, Offset(left, top + cornerLength), Offset(left, top + 12.dp.toPx()), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(left, top + 12.dp.toPx()), Offset(left + 12.dp.toPx(), top), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(left + 12.dp.toPx(), top), Offset(left + cornerLength, top), strokeW, StrokeCap.Round)

            // Top-Right
            drawLine(bracketColor, Offset(right - cornerLength, top), Offset(right - 12.dp.toPx(), top), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(right - 12.dp.toPx(), top), Offset(right, top + 12.dp.toPx()), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(right, top + 12.dp.toPx()), Offset(right, top + cornerLength), strokeW, StrokeCap.Round)

            // Bottom-Left
            drawLine(bracketColor, Offset(left, bottom - cornerLength), Offset(left, bottom - 12.dp.toPx()), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(left, bottom - 12.dp.toPx()), Offset(left + 12.dp.toPx(), bottom), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(left + 12.dp.toPx(), bottom), Offset(left + cornerLength, bottom), strokeW, StrokeCap.Round)

            // Bottom-Right
            drawLine(bracketColor, Offset(right - cornerLength, bottom), Offset(right - 12.dp.toPx(), bottom), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(right - 12.dp.toPx(), bottom), Offset(right, bottom - 12.dp.toPx()), strokeW, StrokeCap.Round)
            drawLine(bracketColor, Offset(right, bottom - 12.dp.toPx()), Offset(right, bottom - cornerLength), strokeW, StrokeCap.Round)

            // Center Pedestal Crosshair
            val crossSize = 12.dp.toPx()
            drawLine(IsaacGold.copy(alpha = 0.7f), Offset(centerX - crossSize, centerY), Offset(centerX + crossSize, centerY), 1.5.dp.toPx())
            drawLine(IsaacGold.copy(alpha = 0.7f), Offset(centerX, centerY - crossSize), Offset(centerX, centerY + crossSize), 1.5.dp.toPx())

            // Animated Brimstone Laser Scan Line (when scanning or active)
            val currentLineY = top + (boxHeight * scanLineYRatio)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        IsaacPrimaryCrimson.copy(alpha = 0.5f),
                        IsaacGold,
                        IsaacPrimaryCrimson,
                        IsaacGold,
                        IsaacPrimaryCrimson.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    startX = left,
                    endX = right
                ),
                start = Offset(left + 8.dp.toPx(), currentLineY),
                end = Offset(right - 8.dp.toPx(), currentLineY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Top Controls: Target Badge, Torch, Zoom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target HUD Indicator Header (Sophisticated Dark styled)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(IsaacSurfaceElevated.copy(alpha = 0.9f))
                    .border(1.dp, IsaacBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(IsaacPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Scan",
                            tint = IsaacPrimaryCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Isaac Scan",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAutoScanEnabled) "Live: Auto Tracking" else "Live: Xbox HDMI Input",
                            color = IsaacPrimaryCrimson,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Quick Tool Icons (Flash, Zoom)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle
                FilledIconButton(
                    onClick = { onTorchToggle(!torchEnabled) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (torchEnabled) IsaacPrimaryCrimson else IsaacSurfaceElevated.copy(alpha = 0.85f),
                        contentColor = if (torchEnabled) IsaacPrimaryContainer else IsaacOnSurfaceVariant
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, IsaacBorder, CircleShape)
                        .testTag("torch_button")
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch"
                    )
                }

                // Zoom Toggle
                FilledIconButton(
                    onClick = { showZoomSlider = !showZoomSlider },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (showZoomSlider) IsaacPrimaryContainer else IsaacSurfaceElevated.copy(alpha = 0.85f),
                        contentColor = if (showZoomSlider) IsaacPrimaryCrimson else IsaacOnSurfaceVariant
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, IsaacBorder, CircleShape)
                        .testTag("zoom_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom Controls"
                    )
                }
            }
        }

        // Floating Zoom Level Slider (when opened)
        if (showZoomSlider) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 80.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(16.dp),
                color = IsaacSurfaceElevated.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "1x", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = zoomLevel,
                        onValueChange = { zoomLevel = it },
                        valueRange = 1.0f..5.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = IsaacPrimaryCrimson,
                            activeTrackColor = IsaacPrimaryCrimson,
                            inactiveTrackColor = IsaacBorder
                        )
                    )
                    Text(
                        text = "%.1fx".format(zoomLevel),
                        color = IsaacPrimaryCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Capture / Scan Button HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Shutter / Scan Button with Isaac Pedestal & Crimson Styling
                FloatingActionButton(
                    onClick = {
                        if (!isScanning) {
                            capturePhoto(imageCapture, context, cameraExecutor) { bitmap ->
                                onCaptureFrame(bitmap)
                            }
                        }
                    },
                    containerColor = IsaacPrimaryCrimson,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(72.dp)
                        .border(2.dp, IsaacGold, CircleShape)
                        .testTag("scan_capture_button"),
                    shape = CircleShape
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = IsaacGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Scan Item On Screen",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isScanning) "AI IDENTIFYING ITEM..." else "TAP TO SCAN ITEM",
                    color = IsaacOnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    context: Context,
    executor: java.util.concurrent.Executor,
    onSuccess: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                ContextCompat.getMainExecutor(context).execute {
                    onSuccess(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Handle rotation degrees
    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
