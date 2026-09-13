package io.github.sceneview.demo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener

enum class SampleModel(val title: String, val assetPath: String, val initialScale: Float) {
    HELMET("Damaged Helmet", "models/damaged_helmet.glb", 1.0f),
    DUCK("Duck", "models/duck.glb", 1.2f),
    AVOCADO("Avocado", "models/avocado.glb", 1.5f)
}

@Composable
fun ModelViewerScreen(
    modifier: Modifier = Modifier
) {
    var selectedModel by remember { mutableStateOf(SampleModel.HELMET) }
    var lightIntensity by remember { mutableFloatStateOf(120_000.0f) }
    var showControls by remember { mutableStateOf(true) }
    var cameraDistance by remember { mutableFloatStateOf(2.5f) }
    var modelScaleMultiplier by remember { mutableFloatStateOf(1.0f) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createKTX1Environment(
            "environments/neutral/neutral_ibl.ktx",
            "environments/neutral/neutral_skybox.ktx"
        )
    }

    val modelInstance = rememberModelInstance(
        modelLoader = modelLoader,
        assetFileLocation = selectedModel.assetPath
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("model_viewer_screen")
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            environment = environment,
            mainLightNode = rememberMainLightNode(engine) {
                intensity = lightIntensity
            },
            cameraNode = rememberCameraNode(engine) {
                position = Position(x = 0f, y = 0f, z = cameraDistance)
            },
            cameraManipulator = rememberCameraManipulator(),
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, _ ->
                    modelScaleMultiplier = if (modelScaleMultiplier > 1.2f) 1.0f else 1.5f
                }
            )
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = selectedModel.initialScale * modelScaleMultiplier,
                    autoAnimate = true
                )
            }
        }

        // Loading indicator when switching models
        if (modelInstance == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Loading ${selectedModel.title}…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Top Header Overlay with Model Selector
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SampleModel.entries.forEach { model ->
                    ElevatedFilterChip(
                        selected = selectedModel == model,
                        onClick = {
                            selectedModel = model
                            modelScaleMultiplier = 1.0f
                        },
                        label = { Text(model.title) },
                        modifier = Modifier.testTag("chip_${model.name.lowercase()}")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { showControls = !showControls },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                        .testTag("toggle_controls_button")
                ) {
                    Icon(
                        imageVector = if (showControls) Icons.Default.Visibility else Icons.Default.Tune,
                        contentDescription = "Toggle controls overlay",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Bottom Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("viewer_controls_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3D PBR Viewport Controls",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                lightIntensity = 120_000.0f
                                modelScaleMultiplier = 1.0f
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset viewport",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Sunlight Intensity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = "Light intensity",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Light: ${(lightIntensity / 1000).toInt()}k lx",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = lightIntensity,
                            onValueChange = { lightIntensity = it },
                            valueRange = 20_000f..250_000f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slider_light_intensity")
                        )
                    }

                    // Gesture Hint
                    Text(
                        text = "• 1-finger drag to Orbit  • Pinch to Zoom  • 2-finger to Pan  • Double-tap to Scale",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
