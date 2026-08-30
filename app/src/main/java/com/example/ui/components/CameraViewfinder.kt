package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacGold
import com.example.ui.theme.IsaacOnBackground
import com.example.ui.theme.IsaacOnSurfaceVariant
import com.example.ui.theme.IsaacPrimaryContainer
import com.example.ui.theme.IsaacPrimaryCrimson
import com.example.ui.theme.IsaacSurfaceElevated
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Shared reticle geometry. Used both by the on-screen overlay [Canvas] and by
 * [cropToReticle], so the pixels handed to the scan engine are exactly the region
 * the user framed. The reticle is centered (no vertical nudge).
 */
object ScanReticle {
    /** Reticle width as a fraction of the viewfinder width. */
    const val WIDTH_FRACTION = 0.78f
    /** Reticle height as a fraction of its own width. */
    const val ASPECT = 0.95f
}

private const val CAPTURE_ERROR_MESSAGE =
    "Couldn't read the camera frame — hold steady and try again"

@Composable
fun CameraViewfinder(
    isScanning: Boolean,
    isAutoScanEnabled: Boolean,
    onCaptureFrame: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    onCaptureError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var maxZoom by remember { mutableFloatStateOf(8.0f) }
    var showZoomSlider by remember { mutableStateOf(false) }
    var tvMode by remember { mutableStateOf(false) }

    var isCapturing by remember { mutableStateOf(false) }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }

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

    // Sync zoom ratio with camera (true optical/digital ratio, so the %.1fx label is honest)
    LaunchedEffect(zoomLevel, maxZoom, camera) {
        camera?.cameraControl?.setZoomRatio(zoomLevel.coerceIn(1.0f, maxZoom))
    }

    // "TV mode" exposure compensation — dial exposure down ~2 EV for a bright emissive screen.
    LaunchedEffect(tvMode, camera) {
        val cam = camera ?: return@LaunchedEffect
        val exposure = cam.cameraInfo.exposureState
        if (!exposure.isExposureCompensationSupported) return@LaunchedEffect
        val target = if (tvMode) {
            val step = exposure.exposureCompensationStep.toFloat().let { if (it > 0f) it else 1f }
            (-2f / step).roundToInt()
                .coerceIn(
                    exposure.exposureCompensationRange.lower,
                    exposure.exposureCompensationRange.upper
                )
        } else {
            0
        }
        runCatching { cam.cameraControl.setExposureCompensationIndex(target) }
    }

    // Auto-dismiss the transient capture-error banner
    LaunchedEffect(captureErrorMessage) {
        if (captureErrorMessage != null) {
            delay(3500)
            captureErrorMessage = null
        }
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
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    // Bind after a layout pass so previewView.viewPort is available and the
                    // ImageCapture buffer is cropped to match exactly what the user sees.
                    previewView.post {
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            val groupBuilder = UseCaseGroup.Builder()
                                .addUseCase(preview)
                                .addUseCase(imageCapture)
                            previewView.viewPort?.let { groupBuilder.setViewPort(it) }
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                groupBuilder.build()
                            )
                            maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio
                                ?.coerceIn(1.0f, 8.0f) ?: 8.0f
                            // Meter the center of the frame once the camera is live.
                            runCatching {
                                val center = previewView.meteringPointFactory.createPoint(
                                    previewView.width / 2f,
                                    previewView.height / 2f
                                )
                                camera?.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(center).build()
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer: pinch-to-zoom + tap-to-focus/meter on the framed area.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxZoom) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomLevel = (zoomLevel * zoom).coerceIn(1.0f, maxZoom)
                    }
                }
                .pointerInput(camera, previewViewRef) {
                    detectTapGestures { offset ->
                        val pv = previewViewRef ?: return@detectTapGestures
                        val cam = camera ?: return@detectTapGestures
                        runCatching {
                            val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                            cam.cameraControl.startFocusAndMetering(
                                FocusMeteringAction.Builder(point).build()
                            )
                        }
                    }
                }
        )

        // Custom Scanner Overlay Canvas (TV / Pedestal Frame Reticle)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Reticle box — shared geometry, centered
            val boxWidth = canvasWidth * ScanReticle.WIDTH_FRACTION
            val boxHeight = boxWidth * ScanReticle.ASPECT
            val left = (canvasWidth - boxWidth) / 2f
            val top = (canvasHeight - boxHeight) / 2f
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

        // Top Controls: Target Badge, TV mode, Zoom
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

            // Quick Tool Icons (TV mode exposure, Zoom)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // TV mode: exposure compensation for a bright emissive screen
                FilledIconButton(
                    onClick = { tvMode = !tvMode },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (tvMode) IsaacPrimaryCrimson else IsaacSurfaceElevated.copy(alpha = 0.85f),
                        contentColor = if (tvMode) IsaacPrimaryContainer else IsaacOnSurfaceVariant
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, IsaacBorder, CircleShape)
                        .testTag("tv_mode_button")
                ) {
                    Icon(
                        imageVector = if (tvMode) Icons.Default.Tv else Icons.Default.WbSunny,
                        contentDescription = "TV Mode (dim exposure)"
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
            val sliderMax = if (maxZoom > 1.05f) maxZoom else 1.05f
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
                        value = zoomLevel.coerceIn(1.0f, sliderMax),
                        onValueChange = { zoomLevel = it },
                        valueRange = 1.0f..sliderMax,
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

        // Transient capture-error banner
        AnimatedVisibility(
            visible = captureErrorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 140.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IsaacPrimaryCrimson.copy(alpha = 0.92f)
            ) {
                Text(
                    text = captureErrorMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
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
                val busy = isScanning || isCapturing
                FloatingActionButton(
                    onClick = {
                        if (!busy) {
                            isCapturing = true
                            capturePhoto(
                                imageCapture = imageCapture,
                                context = context,
                                executor = cameraExecutor,
                                onResult = { bitmap ->
                                    isCapturing = false
                                    if (bitmap != null) {
                                        onCaptureFrame(bitmap)
                                    } else {
                                        captureErrorMessage = CAPTURE_ERROR_MESSAGE
                                        onCaptureError(CAPTURE_ERROR_MESSAGE)
                                    }
                                },
                                onError = { message ->
                                    isCapturing = false
                                    captureErrorMessage = message
                                    onCaptureError(message)
                                }
                            )
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
                    if (busy) {
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
                    text = when {
                        isScanning -> "AI IDENTIFYING ITEM..."
                        isCapturing -> "CAPTURING FRAME..."
                        else -> "TAP TO SCAN ITEM"
                    },
                    color = IsaacOnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Take a single frame. Delivers results on the main thread.
 *  - [onResult] with a cropped [Bitmap], or `null` when the frame could not be decoded.
 *  - [onError] with a user-facing string on capture failure / a dead executor.
 */
private fun capturePhoto(
    imageCapture: ImageCapture,
    context: Context,
    executor: ExecutorService,
    onResult: (Bitmap?) -> Unit,
    onError: (String) -> Unit
) {
    val main = ContextCompat.getMainExecutor(context)
    if (executor.isShutdown) {
        main.execute { onError(CAPTURE_ERROR_MESSAGE) }
        return
    }
    try {
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = try {
                        imageProxyToBitmap(image)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        null
                    } finally {
                        image.close()
                    }
                    main.execute { onResult(bitmap) }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    main.execute { onError(CAPTURE_ERROR_MESSAGE) }
                }
            }
        )
    } catch (t: Throwable) {
        t.printStackTrace()
        main.execute { onError(CAPTURE_ERROR_MESSAGE) }
    }
}

/** Decode → un-rotate → crop to the reticle. Returns null on any failure. */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    var bmp: Bitmap = try {
        image.toBitmap()
    } catch (t: Throwable) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    }

    // Pixel 10 Pro / Android 16 frequently hands back HARDWARE bitmaps, which cannot
    // be read back or cropped — copy to a software config first.
    if (bmp.config == Bitmap.Config.HARDWARE) {
        bmp = bmp.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    }

    val rotation = image.imageInfo.rotationDegrees
    if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    return cropToReticle(bmp)
}

/** Centered sub-rectangle matching the [ScanReticle] overlay. */
fun cropToReticle(src: Bitmap): Bitmap {
    val cropW = (src.width * ScanReticle.WIDTH_FRACTION).roundToInt().coerceIn(1, src.width)
    val cropH = (cropW * ScanReticle.ASPECT).roundToInt().coerceIn(1, src.height)
    val x = ((src.width - cropW) / 2).coerceAtLeast(0)
    val y = ((src.height - cropH) / 2).coerceAtLeast(0)
    return runCatching { Bitmap.createBitmap(src, x, y, cropW, cropH) }.getOrDefault(src)
}
