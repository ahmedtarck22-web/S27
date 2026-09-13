package io.github.sceneview.demo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader

enum class PrimitiveShape(val label: String) {
    CUBE("Cube"),
    SPHERE("Sphere"),
    CYLINDER("Cylinder")
}

private val ShapeColors = listOf(
    Color(0xFF38BDF8) to "Cyan",
    Color(0xFF6366F1) to "Indigo",
    Color(0xFF10B981) to "Emerald",
    Color(0xFFF59E0B) to "Amber",
    Color(0xFFEC4899) to "Pink",
    Color(0xFFE2E8F0) to "Silver"
)

@Composable
fun ShapesScreen(
    modifier: Modifier = Modifier
) {
    var selectedShape by remember { mutableStateOf(PrimitiveShape.SPHERE) }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var metallic by remember { mutableFloatStateOf(0.85f) }
    var roughness by remember { mutableFloatStateOf(0.2f) }
    var autoRotate by remember { mutableStateOf(true) }
    var currentRotationY by remember { mutableFloatStateOf(0f) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createKTX1Environment(
            "environments/neutral/neutral_ibl.ktx",
            "environments/neutral/neutral_skybox.ktx"
        )
    }

    val selectedColor = ShapeColors[selectedColorIndex].first
    val materialInstance = remember(selectedColor, metallic, roughness) {
        materialLoader.createColorInstance(
            color = selectedColor.toArgb(),
            metallic = metallic,
            roughness = roughness,
            reflectance = 0.5f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("shapes_screen")
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            environment = environment,
            mainLightNode = rememberMainLightNode(engine) {
                intensity = 100_000f
            },
            cameraNode = rememberCameraNode(engine) {
                position = Position(0f, 0f, 2.0f)
            },
            cameraManipulator = rememberCameraManipulator(),
            onFrame = {
                if (autoRotate) {
                    currentRotationY = (currentRotationY + 1.2f) % 360f
                }
            }
        ) {
            val rot = Rotation(x = 15f, y = currentRotationY, z = 0f)
            when (selectedShape) {
                PrimitiveShape.CUBE -> CubeNode(
                    size = Float3(0.7f, 0.7f, 0.7f),
                    materialInstance = materialInstance,
                    rotation = rot
                )
                PrimitiveShape.SPHERE -> SphereNode(
                    radius = 0.45f,
                    materialInstance = materialInstance,
                    rotation = rot
                )
                PrimitiveShape.CYLINDER -> CylinderNode(
                    radius = 0.35f,
                    height = 0.75f,
                    materialInstance = materialInstance,
                    rotation = rot
                )
            }
        }

        // Top Shape Picker Chips
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimitiveShape.entries.forEach { shape ->
                ElevatedFilterChip(
                    selected = selectedShape == shape,
                    onClick = { selectedShape = shape },
                    label = { Text(shape.label) },
                    modifier = Modifier.testTag("chip_shape_${shape.name.lowercase()}")
                )
            }
        }

        // Bottom Controls Card
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("shapes_controls_card"),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Physically Based Material",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Spin",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = autoRotate,
                            onCheckedChange = { autoRotate = it },
                            modifier = Modifier.testTag("switch_autorotate")
                        )
                    }
                }

                // Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Albedo:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShapeColors.forEachIndexed { index, (color, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorIndex = index }
                                    .then(
                                        if (selectedColorIndex == index) {
                                            Modifier.border(
                                                width = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                        } else Modifier
                                    )
                                    .testTag("color_chip_$index")
                            )
                        }
                    }
                }

                // Metallic Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Metallic: ${String.format("%.2f", metallic)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(96.dp)
                    )
                    Slider(
                        value = metallic,
                        onValueChange = { metallic = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("slider_metallic")
                    )
                }

                // Roughness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rough: ${String.format("%.2f", roughness)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(96.dp)
                    )
                    Slider(
                        value = roughness,
                        onValueChange = { roughness = it },
                        valueRange = 0.04f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("slider_roughness")
                    )
                }
            }
        }
    }
}
