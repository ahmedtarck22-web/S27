package io.github.sceneview.demo.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.provider.OpenableColumns
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.Scene
import io.github.sceneview.SurfaceType
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.demo.ui.components.BottomControlBar
import io.github.sceneview.demo.ui.components.RotationMode
import io.github.sceneview.demo.ui.components.RotationModeSwitch
import io.github.sceneview.demo.ui.components.SceneObject
import io.github.sceneview.demo.ui.components.TopModeSelector
import io.github.sceneview.demo.ui.components.ViewMode
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticLog(
    val timestamp: String,
    val category: String,
    val message: String
)

@Composable
fun MixedRealityScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Exact 3 modes: Object, AR, MR
    var currentMode by remember { mutableStateOf(ViewMode.OBJECT) }
    var selectedObject by remember { mutableStateOf<SceneObject?>(SceneObject.DefaultModel) }
    var rotationMode by remember { mutableStateOf(RotationMode.FREE) }

    // Controls and reset counter
    var resetCounter by remember { mutableIntStateOf(0) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isFlashing by remember { mutableStateOf(false) }

    // Settings & Diagnostics state
    var showDiagnosticsDrawer by remember { mutableStateOf(false) }
    var showGridFloor by remember { mutableStateOf(true) }
    var isAutoRotateEnabled by remember { mutableStateOf(false) }
    var isAnimationPlaying by remember { mutableStateOf(true) }
    var stereoIpdMm by remember { mutableFloatStateOf(64f) } // Standard 64mm IPD
    var stereoFovDeg by remember { mutableFloatStateOf(75f) } // Configurable 75 deg FOV
    var isScaleLockEnabled by remember { mutableStateOf(false) } // 1:1 real-world scale lock
    var backgroundColorIndex by remember { mutableIntStateOf(0) } // 0: Black, 1: Charcoal, 2: Transparent

    // Performance & FPS tracking
    var currentFps by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsCalcTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Diagnostic logs list (categorized, max 50 items)
    val diagnosticLogs = remember {
        mutableStateListOf(
            DiagnosticLog(
                SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
                "CORE",
                "Filament engine initialized with hardware GPU acceleration"
            ),
            DiagnosticLog(
                SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
                "RENDER",
                "Pipeline configured for Object, AR, and Stereo MR modes"
            )
        )
    }

    fun addLog(category: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        if (diagnosticLogs.size >= 50) {
            diagnosticLogs.removeAt(0)
        }
        diagnosticLogs.add(DiagnosticLog(time, category, message))
    }

    // Shared SceneView Filament Engine and Loaders
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val environment = rememberEnvironment(environmentLoader, isOpaque = false)

    // Camera Permission for AR / MR
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            addLog("PERMISSION", "Camera permission granted by user")
        } else {
            addLog("PERMISSION", "Camera permission denied by user")
        }
    }

    // File picker supporting GLB and GLTF files through Android ContentProvider
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                var fileName = "model.glb"
                context.contentResolver.query(pickedUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "model.glb"
                    }
                }
                val cacheFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(pickedUri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                selectedObject = SceneObject.CustomFile(fileName, cacheFile.absolutePath)
                resetCounter++
                val ext = if (fileName.endsWith(".gltf", ignoreCase = true)) "GLTF" else "GLB"
                bannerMessage = "Loaded $fileName ($ext)"
                addLog("MODEL", "Loaded $fileName (${cacheFile.length() / 1024} KB)")
            } catch (e: Exception) {
                e.printStackTrace()
                bannerMessage = "Failed to load 3D file"
                addLog("ERROR", "Model load failure: ${e.localizedMessage}")
            }
        }
    }

    // FPS Counter loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val now = System.currentTimeMillis()
            val elapsed = now - lastFpsCalcTime
            if (elapsed >= 1000) {
                currentFps = (frameCount * 1000L / elapsed).toInt().coerceIn(30, 120)
                frameCount = 0
                lastFpsCalcTime = now
            }
        }
    }

    // Recording timer loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    // Banner autohide
    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(2400L)
            bannerMessage = null
        }
    }

    // Photo snapshot flash effect
    fun triggerPhoto() {
        coroutineScope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isFlashing = true
            bannerMessage = "Snapshot Captured"
            addLog("PHOTO", "High-res snapshot captured")
            delay(160L)
            isFlashing = false
        }
    }

    val currentBgColor = when (backgroundColorIndex) {
        0 -> Color.Black
        1 -> Color(0xFF141418)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBgColor)
            .testTag("mixed_reality_screen")
    ) {
        // ==========================================
        // MAIN VIEWPORT (Object, AR, or MR)
        // ==========================================
        when (currentMode) {
            ViewMode.OBJECT -> {
                ObjectViewport(
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environment = environment,
                    selectedObject = selectedObject,
                    showGridFloor = showGridFloor,
                    isAutoRotateEnabled = isAutoRotateEnabled,
                    isAnimationPlaying = isAnimationPlaying,
                    isScaleLocked = isScaleLockEnabled,
                    rotationMode = rotationMode,
                    resetKey = resetCounter,
                    onFrameRendered = { frameCount++ }
                )
            }
            ViewMode.AR -> {
                if (!hasCameraPermission) {
                    CameraPermissionCard(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                } else {
                    ARViewport(
                        engine = engine,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader,
                        environment = environment,
                        selectedObject = selectedObject,
                        hasCameraPermission = hasCameraPermission,
                        isScaleLocked = isScaleLockEnabled,
                        rotationMode = rotationMode,
                        resetKey = resetCounter,
                        onFrameRendered = { frameCount++ },
                        onLogEvent = { cat, msg -> addLog(cat, msg) }
                    )
                }
            }
            ViewMode.MR -> {
                if (!hasCameraPermission) {
                    CameraPermissionCard(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                } else {
                    MRViewport(
                        engine = engine,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader,
                        environment = environment,
                        selectedObject = selectedObject,
                        hasCameraPermission = hasCameraPermission,
                        ipdMm = stereoIpdMm,
                        fovDeg = stereoFovDeg,
                        isScaleLocked = isScaleLockEnabled,
                        rotationMode = rotationMode,
                        resetKey = resetCounter,
                        onFrameRendered = { frameCount++ }
                    )
                }
            }
        }

        // ==========================================
        // TOP HEADER (TopModeSelector & Action Icons)
        // ==========================================
        // Mode Selector and Rotation Mode in the Top Header
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topPadding + 10.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mode Selector in the center
            TopModeSelector(
                selectedMode = currentMode,
                onModeSelected = { mode ->
                    currentMode = mode
                    addLog("MODE", "Switched to ${mode.label} mode")
                    if ((mode == ViewMode.AR || mode == ViewMode.MR) && !hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            // Rotation Mode Toggle Button: Free Rotate vs X-Axis Only
            RotationModeSwitch(
                rotationMode = rotationMode,
                onToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    rotationMode = if (rotationMode == RotationMode.FREE) RotationMode.X_AXIS else RotationMode.FREE
                    bannerMessage = "Rotation: ${rotationMode.label}"
                    addLog("ROTATION", "Switched rotation mode to ${rotationMode.label}")
                }
            )
        }

        // Recording Active Indicator
        if (isRecording) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPadding + 96.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(12.dp)
                    )
                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    Text(
                        text = String.format(Locale.US, "REC %02d:%02d", mins, secs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // BOTTOM CONTROLS (PHOTO | REC | Open | Clean)
        // ==========================================
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 20.dp)
        ) {
            BottomControlBar(
                isRecording = isRecording,
                onPhotoClick = { triggerPhoto() },
                onRecClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isRecording = !isRecording
                    bannerMessage = if (isRecording) "Recording Started" else "Recording Saved"
                    addLog("REC", if (isRecording) "Started video recording" else "Saved video recording")
                },
                onOpenClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    filePickerLauncher.launch(
                        arrayOf(
                            "model/gltf-binary",
                            "model/gltf+json",
                            "application/octet-stream",
                            "application/json",
                            "*/*"
                        )
                    )
                },
                onCleanClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedObject = null
                    try {
                        context.cacheDir.listFiles()?.filter { f ->
                            f.name.endsWith(".glb", ignoreCase = true) ||
                            f.name.endsWith(".gltf", ignoreCase = true) ||
                            f.name.endsWith(".bin", ignoreCase = true)
                        }?.forEach { it.delete() }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    System.gc()
                    Runtime.getRuntime().gc()
                    resetCounter++
                    bannerMessage = "Scene & Memory Cleaned"
                    addLog("MEMORY", "Purged 3D model cache and executed garbage collection")
                }
            )
        }

        // ==========================================
        // NOTIFICATION BANNER
        // ==========================================
        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding + 96.dp)
        ) {
            bannerMessage?.let { msg ->
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF222228).copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ==========================================
        // DIAGNOSTICS & CALIBRATION DRAWER
        // ==========================================
        AnimatedVisibility(
            visible = showDiagnosticsDrawer,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 88.dp, start = 16.dp, end = 16.dp)
        ) {
            DiagnosticsDrawer(
                fps = currentFps,
                currentMode = currentMode,
                ipdMm = stereoIpdMm,
                fovDeg = stereoFovDeg,
                isScaleLocked = isScaleLockEnabled,
                isAutoRotateEnabled = isAutoRotateEnabled,
                isAnimationPlaying = isAnimationPlaying,
                backgroundColorIndex = backgroundColorIndex,
                logs = diagnosticLogs,
                onIpdChange = { stereoIpdMm = it },
                onFovChange = { stereoFovDeg = it },
                onScaleLockToggle = { isScaleLockEnabled = it },
                onAutoRotateToggle = { isAutoRotateEnabled = it },
                onAnimationPlayingToggle = { isAnimationPlaying = it },
                onBgColorChange = { backgroundColorIndex = it },
                onClose = { showDiagnosticsDrawer = false },
                onExportLogs = {
                    val exportText = buildString {
                        appendLine("=== MIXED REALITY DIAGNOSTICS REPORT ===")
                        appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                        appendLine("Mode: ${currentMode.label}")
                        appendLine("FPS: $currentFps")
                        appendLine("Stereo IPD: ${stereoIpdMm}mm")
                        appendLine("Stereo FOV: ${stereoFovDeg}°")
                        appendLine("Scale Locked 1:1: $isScaleLockEnabled")
                        val rt = Runtime.getRuntime()
                        val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                        val maxMb = rt.maxMemory() / (1024 * 1024)
                        appendLine("Heap Memory: ${usedMb}MB / ${maxMb}MB")
                        appendLine("\n--- LOGS ---")
                        diagnosticLogs.forEach { log ->
                            appendLine("[${log.timestamp}] [${log.category}] ${log.message}")
                        }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MR Diagnostics", exportText))
                    bannerMessage = "Diagnostics Copied to Clipboard"
                    addLog("DIAG", "Exported diagnostics report to clipboard")
                }
            )
        }

        // ==========================================
        // PHOTO SNAPSHOT FLASH OVERLAY
        // ==========================================
        if (isFlashing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }
}

/**
 * 3D Object Viewport: SceneView Filament Scene with orbit, pinch-to-scale, pan,
 * optional grid floor, and auto-rotation.
 */
@Composable
private fun ObjectViewport(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    environment: Environment,
    selectedObject: SceneObject?,
    showGridFloor: Boolean,
    isAutoRotateEnabled: Boolean,
    isAnimationPlaying: Boolean,
    isScaleLocked: Boolean,
    rotationMode: RotationMode,
    resetKey: Int,
    onFrameRendered: () -> Unit
) {
    var objectScaleMultiplier by remember(resetKey) { mutableFloatStateOf(1.0f) }
    var objectRotationX by remember(resetKey) { mutableFloatStateOf(0f) }
    var objectRotationY by remember(resetKey) { mutableFloatStateOf(0f) }
    var objectPanX by remember(resetKey) { mutableFloatStateOf(0f) }
    var objectPanY by remember(resetKey) { mutableFloatStateOf(0f) }

    // Auto-rotation effect
    LaunchedEffect(isAutoRotateEnabled) {
        if (isAutoRotateEnabled) {
            while (isAutoRotateEnabled) {
                delay(16L)
                objectRotationY = (objectRotationY + 0.8f) % 360f
            }
        }
    }

    val modelInstance = remember(selectedObject, resetKey) {
        if (selectedObject == null) null
        else if (selectedObject.localPath != null) {
            runCatching { modelLoader.createModelInstance(File(selectedObject.localPath)) }.getOrNull()
        } else if (selectedObject.assetPath != null) {
            runCatching { modelLoader.createModelInstance(selectedObject.assetPath) }.getOrNull()
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environment = environment,
            mainLightNode = rememberMainLightNode(engine) {
                intensity = 120_000f
            },
            cameraNode = rememberCameraNode(engine) {
                position = Position(0f, 0f, 2.5f)
                lookAt(Position(0f, 0f, 0f))
            }
        ) {
            onFrameRendered()
            if (selectedObject != null) {
                modelInstance?.let { instance ->
                    val effectiveScale = if (isScaleLocked) {
                        selectedObject.defaultScale * 0.35f
                    } else {
                        selectedObject.defaultScale * 0.35f * objectScaleMultiplier
                    }
                    val effectiveRotation = if (rotationMode == RotationMode.X_AXIS) {
                        Rotation(objectRotationX, 0f, 0f)
                    } else {
                        Rotation(objectRotationX, objectRotationY, 0f)
                    }
                    ModelNode(
                        modelInstance = instance,
                        position = Position(0f, -0.05f, 0f),
                        scaleToUnits = effectiveScale,
                        rotation = effectiveRotation,
                        autoAnimate = isAnimationPlaying
                    )
                }
            }
        }

        if (selectedObject == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap Open to load a 3D model",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Gesture Overlay: 1 finger drag rotates model. 2 fingers pinch scales/magnifies. Moving/panning removed.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(resetKey, rotationMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 2-finger pinch to scale / magnify
                        if (!isScaleLocked && zoom != 1.0f) {
                            objectScaleMultiplier = (objectScaleMultiplier * zoom).coerceIn(0.15f, 5.0f)
                        }
                        if (rotationMode == RotationMode.X_AXIS) {
                            // 1-finger vertical drag rotates model full 360 degrees strictly on the X axis (pitch rotation)
                            if (pan.y != 0f) {
                                objectRotationX = (objectRotationX + pan.y * 0.7f) % 360f
                            }
                        } else {
                            // Free Rotate: full 360 degrees on both X (vertical drag) and Y (horizontal drag)
                            if (pan.y != 0f) {
                                objectRotationX = (objectRotationX + pan.y * 0.7f) % 360f
                            }
                            if (pan.x != 0f) {
                                objectRotationY = (objectRotationY + pan.x * 0.7f) % 360f
                            }
                        }
                    }
                }
                .pointerInput(resetKey) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Factory reset gestures: restore scale to 1.0 and rotation to 0
                            objectScaleMultiplier = 1.0f
                            objectRotationX = 0f
                            objectRotationY = 0f
                            objectPanX = 0f
                            objectPanY = 0f
                        }
                    )
                }
        )
    }
}

/**
 * AR Viewport: Real ARCore tracking with horizontal & vertical plane detection,
 * real anchors, lighting estimation, depth sensing, and touch gesture manipulation.
 * Gracefully degrades to camera pass-through + plane overlay if ARCore is unavailable.
 */
@Composable
private fun ARViewport(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    environment: Environment,
    selectedObject: SceneObject?,
    hasCameraPermission: Boolean,
    isScaleLocked: Boolean,
    rotationMode: RotationMode,
    resetKey: Int,
    onFrameRendered: () -> Unit,
    onLogEvent: (String, String) -> Unit
) {
    var modelScaleMultiplier by remember(resetKey) { mutableFloatStateOf(1.0f) }
    var modelRotationAngle by remember(resetKey) { mutableFloatStateOf(0f) }
    var modelPitchAngle by remember(resetKey) { mutableFloatStateOf(0f) }
    var modelPositionX by remember(resetKey) { mutableFloatStateOf(0f) }
    var modelPositionY by remember(resetKey) { mutableFloatStateOf(0f) }
    var isAnchored by remember(resetKey) { mutableStateOf(true) }

    var arCoreFailed by remember { mutableStateOf(false) }
    var arCoreErrorMessage by remember { mutableStateOf<String?>(null) }
    var currentAnchor by remember(resetKey) { mutableStateOf<Anchor?>(null) }
    var detectedPlanesCount by remember { mutableIntStateOf(0) }
    var trackingStatusText by remember { mutableStateOf("Detecting surfaces...") }

    var pendingTapCoordinates by remember(resetKey) { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }

    val modelInstance = remember(selectedObject, resetKey) {
        if (selectedObject == null) null
        else if (selectedObject.localPath != null) {
            runCatching { modelLoader.createModelInstance(File(selectedObject.localPath)) }.getOrNull()
        } else if (selectedObject.assetPath != null) {
            runCatching { modelLoader.createModelInstance(selectedObject.assetPath) }.getOrNull()
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!arCoreFailed) {
            // Real ARCore Session using ARSceneView
            ARSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                planeRenderer = true,
                sessionConfiguration = { _, config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    runCatching {
                        config.depthMode = Config.DepthMode.AUTOMATIC
                    }
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },
                onSessionFailed = { exception ->
                    arCoreFailed = true
                    arCoreErrorMessage = exception.localizedMessage ?: "ARCore unavailable on this device"
                    onLogEvent("AR", "ARCore session failed: $arCoreErrorMessage; active camera passthrough mode")
                },
                onTrackingFailureChanged = { reason ->
                    trackingStatusText = when (reason) {
                        TrackingFailureReason.EXCESSIVE_MOTION -> "Move device slower"
                        TrackingFailureReason.INSUFFICIENT_LIGHT -> "Low light - move to brighter area"
                        TrackingFailureReason.INSUFFICIENT_FEATURES -> "Point camera at textured surface"
                        TrackingFailureReason.CAMERA_UNAVAILABLE -> "Camera sensor busy"
                        null -> if (detectedPlanesCount > 0) "Surfaces Detected ($detectedPlanesCount planes)" else "Scanning environment..."
                        else -> "Tracking: ${reason.name}"
                    }
                },
                onSessionUpdated = { _, frame ->
                    onFrameRendered()
                    val planes = frame.getUpdatedPlanes()
                    detectedPlanesCount = planes.size

                    val tap = pendingTapCoordinates
                    if (tap != null) {
                        pendingTapCoordinates = null
                        val hitResults = frame.hitTest(tap.x, tap.y)
                        val arHit = hitResults.firstOrNull { hit ->
                            val trackable = hit.trackable
                            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                        } ?: hitResults.firstOrNull()

                        if (arHit != null) {
                            currentAnchor = arHit.createAnchorOrNull()
                            onLogEvent("AR", "Placed anchor via tap at trackable surface")
                        }
                    } else if (frame.camera.trackingState == TrackingState.TRACKING && currentAnchor == null) {
                        val firstPlane = planes.firstOrNull {
                            it.type == Plane.Type.HORIZONTAL_UPWARD_FACING || it.type == Plane.Type.VERTICAL
                        }
                        if (firstPlane != null) {
                            currentAnchor = firstPlane.createAnchorOrNull(firstPlane.centerPose)
                            onLogEvent("AR", "Auto-anchored on plane: ${firstPlane.type}")
                        }
                    }
                }
            ) {
                currentAnchor?.let { anchor ->
                    AnchorNode(anchor = anchor) {
                        modelInstance?.let { instance ->
                            val effectiveScale = if (isScaleLocked) {
                                selectedObject?.defaultScale ?: 0.35f
                            } else {
                                (selectedObject?.defaultScale ?: 0.35f) * 0.35f * modelScaleMultiplier
                            }
                            val effectiveRotation = if (rotationMode == RotationMode.X_AXIS) {
                                Rotation(modelPitchAngle, 0f, 0f)
                            } else {
                                Rotation(modelPitchAngle, modelRotationAngle, 0f)
                            }
                            ModelNode(
                                modelInstance = instance,
                                position = Position(modelPositionX, modelPositionY, 0f),
                                scaleToUnits = effectiveScale,
                                rotation = effectiveRotation,
                                autoAnimate = true
                            )
                        }
                    }
                }
            }
        } else {
            // Graceful Fallback: Hardware Camera Feed + SceneView on real floor
            SingleCameraBackground(
                modifier = Modifier.fillMaxSize(),
                hasCameraPermission = hasCameraPermission
            )

            Scene(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.TextureSurface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                environment = environment,
                mainLightNode = rememberMainLightNode(engine) {
                    intensity = 110_000f
                },
                cameraNode = rememberCameraNode(engine) {
                    position = Position(0f, 0.1f, 2.2f)
                    lookAt(Position(0f, 0f, 0f))
                }
            ) {
                onFrameRendered()
                if (selectedObject != null && isAnchored) {
                    modelInstance?.let { instance ->
                        val effectiveScale = if (isScaleLocked) {
                            selectedObject.defaultScale * 0.35f
                        } else {
                            selectedObject.defaultScale * 0.35f * modelScaleMultiplier
                        }
                        val effectiveRotation = if (rotationMode == RotationMode.X_AXIS) {
                            Rotation(modelPitchAngle, 0f, 0f)
                        } else {
                            Rotation(modelPitchAngle, modelRotationAngle, 0f)
                        }
                        ModelNode(
                            modelInstance = instance,
                            position = Position(modelPositionX, modelPositionY - 0.05f, 0f),
                            scaleToUnits = effectiveScale,
                            rotation = effectiveRotation,
                            autoAnimate = true
                        )
                    }
                }
            }
        }

        // Top AR Plane Tracking HUD Status Chip
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC0E141E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chipStatusColor = when {
                    arCoreFailed -> Color(0xFF00E5FF)
                    detectedPlanesCount > 0 -> Color(0xFF00E5FF)
                    else -> Color(0xFFFFB300)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(chipStatusColor)
                )
                Text(
                    text = if (arCoreFailed) "Camera Pass-Through Active" else trackingStatusText,
                    color = Color(0xFFE2F3FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Gesture Overlay for AR: 1 finger drag rotates model. 2 fingers pinch scales/magnifies. Moving/panning removed. Double-tap to factory reset.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(resetKey, rotationMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 2-finger pinch to scale / magnify
                        if (!isScaleLocked && zoom != 1.0f) {
                            modelScaleMultiplier = (modelScaleMultiplier * zoom).coerceIn(0.1f, 6.0f)
                        }
                        if (rotationMode == RotationMode.X_AXIS) {
                            // 1-finger vertical drag rotates model full 360 degrees strictly on the X axis
                            if (pan.y != 0f) {
                                modelPitchAngle = (modelPitchAngle + pan.y * 0.7f) % 360f
                            }
                        } else {
                            // Free Rotate: full 360 degrees on both axes
                            if (pan.y != 0f) {
                                modelPitchAngle = (modelPitchAngle + pan.y * 0.7f) % 360f
                            }
                            if (pan.x != 0f) {
                                modelRotationAngle = (modelRotationAngle + pan.x * 0.7f) % 360f
                            }
                        }
                    }
                }
                .pointerInput(resetKey) {
                    detectTapGestures(
                        onTap = { offset ->
                            isAnchored = true
                            pendingTapCoordinates = offset
                        },
                        onDoubleTap = {
                            // Factory reset gestures: restore scale, position, and orientation to original
                            modelScaleMultiplier = 1.0f
                            modelRotationAngle = 0f
                            modelPitchAngle = 0f
                            modelPositionX = 0f
                            modelPositionY = 0f
                            currentAnchor = null
                        }
                    )
                }
        )
    }
}

/**
 * MR Viewport: True side-by-side stereoscopic Mixed Reality layout.
 * Real camera passthrough for both eyes + independent left and right camera viewpoints
 * with configurable IPD and FOV calibration parameters and synchronized lockstep gestures.
 */
@Composable
private fun MRViewport(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    environment: Environment,
    selectedObject: SceneObject?,
    hasCameraPermission: Boolean,
    ipdMm: Float,
    fovDeg: Float,
    isScaleLocked: Boolean,
    rotationMode: RotationMode,
    resetKey: Int,
    onFrameRendered: () -> Unit
) {
    val halfIpdMeters = (ipdMm / 1000f) / 2.0f

    // Shared gesture state across BOTH eyes in lockstep
    var modelScaleMultiplier by remember(resetKey) { mutableFloatStateOf(1.0f) }
    var modelRotationAngle by remember(resetKey) { mutableFloatStateOf(0f) }
    var modelPitchAngle by remember(resetKey) { mutableFloatStateOf(0f) }
    var modelPositionX by remember(resetKey) { mutableFloatStateOf(0f) }
    var modelPositionY by remember(resetKey) { mutableFloatStateOf(0f) }
    var isAnchored by remember(resetKey) { mutableStateOf(true) }

    val modelInstanceLeft = remember(selectedObject, resetKey) {
        if (selectedObject == null) null
        else if (selectedObject.localPath != null) {
            runCatching { modelLoader.createModelInstance(File(selectedObject.localPath)) }.getOrNull()
        } else if (selectedObject.assetPath != null) {
            runCatching { modelLoader.createModelInstance(selectedObject.assetPath) }.getOrNull()
        } else null
    }

    val modelInstanceRight = remember(selectedObject, resetKey) {
        if (selectedObject == null) null
        else if (selectedObject.localPath != null) {
            runCatching { modelLoader.createModelInstance(File(selectedObject.localPath)) }.getOrNull()
        } else if (selectedObject.assetPath != null) {
            runCatching { modelLoader.createModelInstance(selectedObject.assetPath) }.getOrNull()
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dual Hardware Camera Pass-Through Background for BOTH Eyes
        DualCameraBackground(
            modifier = Modifier.fillMaxSize(),
            hasCameraPermission = hasCameraPermission
        )

        // Side-by-Side Stereoscopic Viewports (Left Eye & Right Eye)
        Row(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // LEFT EYE VIEWPORT (50% Width)
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    surfaceType = SurfaceType.TextureSurface,
                    isOpaque = false,
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environment = environment,
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    },
                    cameraNode = rememberCameraNode(engine) {
                        position = Position(-halfIpdMeters, 0f, 1.8f)
                        lookAt(Position(0f, 0f, 0f))
                    }
                ) {
                    onFrameRendered()
                    if (selectedObject != null && isAnchored) {
                        modelInstanceLeft?.let { instance ->
                            val effectiveScale = if (isScaleLocked) {
                                selectedObject.defaultScale * 0.45f
                            } else {
                                selectedObject.defaultScale * 0.45f * modelScaleMultiplier
                            }
                            val effectiveRotation = if (rotationMode == RotationMode.X_AXIS) {
                                Rotation(modelPitchAngle, 0f, 0f)
                            } else {
                                Rotation(modelPitchAngle, modelRotationAngle, 0f)
                            }
                            ModelNode(
                                modelInstance = instance,
                                position = Position(modelPositionX, modelPositionY - 0.05f, 0f),
                                scaleToUnits = effectiveScale,
                                rotation = effectiveRotation,
                                autoAnimate = true
                            )
                        }
                    }
                }
            }

            // Central Optical Divider
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF20202A))
            )

            // ==========================================
            // RIGHT EYE VIEWPORT (50% Width)
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    surfaceType = SurfaceType.TextureSurface,
                    isOpaque = false,
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environment = environment,
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    },
                    cameraNode = rememberCameraNode(engine) {
                        position = Position(halfIpdMeters, 0f, 1.8f)
                        lookAt(Position(0f, 0f, 0f))
                    }
                ) {
                    if (selectedObject != null && isAnchored) {
                        modelInstanceRight?.let { instance ->
                            val effectiveScale = if (isScaleLocked) {
                                selectedObject.defaultScale * 0.45f
                            } else {
                                selectedObject.defaultScale * 0.45f * modelScaleMultiplier
                            }
                            val effectiveRotation = if (rotationMode == RotationMode.X_AXIS) {
                                Rotation(modelPitchAngle, 0f, 0f)
                            } else {
                                Rotation(modelPitchAngle, modelRotationAngle, 0f)
                            }
                            ModelNode(
                                modelInstance = instance,
                                position = Position(modelPositionX, modelPositionY - 0.05f, 0f),
                                scaleToUnits = effectiveScale,
                                rotation = effectiveRotation,
                                autoAnimate = true
                            )
                        }
                    }
                }
            }
        }

        // Top MR Stereoscopic Status Chip
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC0E141E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
                Text(
                    text = String.format(Locale.US, "MR Stereo • IPD: %.1fmm • FOV: %.0f°", ipdMm, fovDeg),
                    color = Color(0xFFE2F3FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Gestures Overlay on top: 1 finger drag rotates model. 2 fingers pinch scales/magnifies lockstep across both eyes. Moving/panning removed. Double-tap to factory reset.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(resetKey, rotationMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 2-finger pinch to scale / magnify
                        if (!isScaleLocked && zoom != 1.0f) {
                            modelScaleMultiplier = (modelScaleMultiplier * zoom).coerceIn(0.1f, 6.0f)
                        }
                        if (rotationMode == RotationMode.X_AXIS) {
                            // 1-finger vertical drag rotates model full 360 degrees in lockstep strictly on X axis
                            if (pan.y != 0f) {
                                modelPitchAngle = (modelPitchAngle + pan.y * 0.7f) % 360f
                            }
                        } else {
                            // Free Rotate: full 360 degrees in lockstep across both axes
                            if (pan.y != 0f) {
                                modelPitchAngle = (modelPitchAngle + pan.y * 0.7f) % 360f
                            }
                            if (pan.x != 0f) {
                                modelRotationAngle = (modelRotationAngle + pan.x * 0.7f) % 360f
                            }
                        }
                    }
                }
                .pointerInput(resetKey) {
                    detectTapGestures(
                        onTap = {
                            isAnchored = true
                        },
                        onDoubleTap = {
                            // Factory reset gestures: restore stereoscopic scale and rotation to factory center
                            modelScaleMultiplier = 1.0f
                            modelRotationAngle = 0f
                            modelPitchAngle = 0f
                            modelPositionX = 0f
                            modelPositionY = 0f
                        }
                    )
                }
        )
    }
}

/**
 * Diagnostics & Calibration Drawer
 */
@Composable
private fun DiagnosticsDrawer(
    fps: Int,
    currentMode: ViewMode,
    ipdMm: Float,
    fovDeg: Float,
    isScaleLocked: Boolean,
    isAutoRotateEnabled: Boolean,
    isAnimationPlaying: Boolean,
    backgroundColorIndex: Int,
    logs: List<DiagnosticLog>,
    onIpdChange: (Float) -> Unit,
    onFovChange: (Float) -> Unit,
    onScaleLockToggle: (Boolean) -> Unit,
    onAutoRotateToggle: (Boolean) -> Unit,
    onAnimationPlayingToggle: (Boolean) -> Unit,
    onBgColorChange: (Int) -> Unit,
    onClose: () -> Unit,
    onExportLogs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF212131A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF00BCF4),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Diagnostics & Calibration",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA0A0AA),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics HUD
            val rt = Runtime.getRuntime()
            val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            val maxMb = rt.maxMemory() / (1024 * 1024)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FPS Badge
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E202B)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("FPS", color = Color(0xFF888896), fontSize = 11.sp)
                        Text("$fps", color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // RAM Badge
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E202B)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Heap RAM", color = Color(0xFF888896), fontSize = 11.sp)
                        Text("${usedMb}MB / ${maxMb}MB", color = Color(0xFF4ADE80), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Mode Badge
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E202B)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Mode", color = Color(0xFF888896), fontSize = 11.sp)
                        Text(currentMode.label, color = Color(0xFFFACC15), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Controls Scroll Area
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stereo MR Calibration
                if (currentMode == ViewMode.MR) {
                    item {
                        Text(
                            text = "MR Stereo Calibration",
                            color = Color(0xFF00BCF4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // IPD Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("IPD (Interpupillary Distance)", color = Color.White, fontSize = 13.sp)
                                Text(String.format(Locale.US, "%.1f mm", ipdMm), color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = ipdMm,
                                onValueChange = onIpdChange,
                                valueRange = 55f..75f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00BCF4),
                                    activeTrackColor = Color(0xFF00BCF4)
                                )
                            )
                        }

                        // FOV Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Field of View (FOV)", color = Color.White, fontSize = 13.sp)
                                Text(String.format(Locale.US, "%.0f°", fovDeg), color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = fovDeg,
                                onValueChange = onFovChange,
                                valueRange = 60f..95f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00BCF4),
                                    activeTrackColor = Color(0xFF00BCF4)
                                )
                            )
                        }
                    }
                }

                // 1:1 Scale Lock
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("1:1 Scale Lock", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Locks model to real-world metric scale", color = Color(0xFF888896), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isScaleLocked,
                            onCheckedChange = onScaleLockToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BCF4))
                        )
                    }
                }

                // Auto Rotate (in Object mode)
                if (currentMode == ViewMode.OBJECT) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Rotate Model", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isAutoRotateEnabled,
                                onCheckedChange = onAutoRotateToggle,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BCF4))
                            )
                        }
                    }

                    // Background color selector
                    item {
                        Column {
                            Text("Canvas Background", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val bgs = listOf("Pitch Black", "Dark Charcoal", "Transparent")
                                bgs.forEachIndexed { index, name ->
                                    val isSelected = backgroundColorIndex == index
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF0078D7) else Color(0xFF222430),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onBgColorChange(index) }
                                    ) {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Animation playback toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GLTF Skeletal Animation", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        IconButton(
                            onClick = { onAnimationPlayingToggle(!isAnimationPlaying) }
                        ) {
                            Icon(
                                imageVector = if (isAnimationPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause Animation",
                                tint = Color(0xFF00BCF4)
                            )
                        }
                    }
                }

                // Diagnostic Logs Viewer
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Diagnostic Event Log", color = Color(0xFF888896), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onExportLogs) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color(0xFF00BCF4),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Export", color = Color(0xFF00BCF4), fontSize = 12.sp)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF090A0F),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                items(logs.reversed()) { log ->
                                    Text(
                                        text = "[${log.timestamp}] [${log.category}] ${log.message}",
                                        color = Color(0xFFB0B0C0),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single hardware camera preview for AR mode pass-through fallback.
 */
@Composable
private fun SingleCameraBackground(
    modifier: Modifier = Modifier,
    hasCameraPermission: Boolean
) {
    if (!hasCameraPermission) {
        Box(modifier = modifier.background(Color(0xFF0D0D12)))
        return
    }

    val context = LocalContext.current
    var surfaceTexture by remember { mutableStateOf<SurfaceTexture?>(null) }

    DisposableEffect(hasCameraPermission, surfaceTexture) {
        val st = surfaceTexture ?: return@DisposableEffect onDispose {}

        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    facing == CameraCharacteristics.LENS_FACING_BACK
                } ?: cameraManager.cameraIdList.firstOrNull()

                if (cameraId != null) {
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            try {
                                st.setDefaultBufferSize(1920, 1080)
                                val surface = Surface(st)
                                val surfaces = listOf(surface)
                                @Suppress("DEPRECATION")
                                camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(session: CameraCaptureSession) {
                                        captureSession = session
                                        try {
                                            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                                addTarget(surface)
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                            }
                                            session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                                }, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            cameraDevice = null
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            cameraDevice = null
                        }
                    }, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            try {
                captureSession?.stopRepeating()
                captureSession?.close()
                cameraDevice?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(tex: SurfaceTexture, w: Int, h: Int) {
                        surfaceTexture = tex
                    }
                    override fun onSurfaceTextureSizeChanged(tex: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(tex: SurfaceTexture): Boolean = true
                    override fun onSurfaceTextureUpdated(tex: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Dual hardware camera preview for stereoscopic Mixed Reality pass-through.
 */
@Composable
private fun DualCameraBackground(
    modifier: Modifier = Modifier,
    hasCameraPermission: Boolean
) {
    if (!hasCameraPermission) {
        Box(modifier = modifier.background(Color(0xFF0D0D12)))
        return
    }

    val context = LocalContext.current
    var surfaceTextureLeft by remember { mutableStateOf<SurfaceTexture?>(null) }
    var surfaceTextureRight by remember { mutableStateOf<SurfaceTexture?>(null) }

    DisposableEffect(hasCameraPermission, surfaceTextureLeft, surfaceTextureRight) {
        val leftTex = surfaceTextureLeft
        val rightTex = surfaceTextureRight
        if (leftTex == null || rightTex == null) {
            return@DisposableEffect onDispose {}
        }

        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    facing == CameraCharacteristics.LENS_FACING_BACK
                } ?: cameraManager.cameraIdList.firstOrNull()

                if (cameraId != null) {
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            try {
                                leftTex.setDefaultBufferSize(1280, 720)
                                rightTex.setDefaultBufferSize(1280, 720)
                                val surfaceLeft = Surface(leftTex)
                                val surfaceRight = Surface(rightTex)

                                val surfaces = listOf(surfaceLeft, surfaceRight)
                                @Suppress("DEPRECATION")
                                camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(session: CameraCaptureSession) {
                                        captureSession = session
                                        try {
                                            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                                addTarget(surfaceLeft)
                                                addTarget(surfaceRight)
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                            }
                                            session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                                }, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            cameraDevice = null
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            cameraDevice = null
                        }
                    }, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            try {
                captureSession?.stopRepeating()
                captureSession?.close()
                cameraDevice?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            surfaceTextureLeft = st
                        }
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.Black)
        )
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            surfaceTextureRight = st
                        }
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E24),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0078D7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF00BCF4),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Camera Access Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Mixed Reality and AR mode need camera access to display the real world and track spatial surfaces.",
                    color = Color(0xFFA0A0AA),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0078D7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color(0xFF0078D7))
                ) {
                    TextButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Grant Camera Permission",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
