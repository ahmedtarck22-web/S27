package io.github.sceneview.demo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider

enum class ViewMode(val label: String) {
    OBJECT("Object"),
    AR("AR"),
    MR("MR")
}

enum class RotationMode(val label: String) {
    FREE("X & Y Axes (Free)"),
    X_AXIS("X-Axis Only")
}

val CapsuleBgColor = Color(0xFFCACACC)
val CapsuleSelectedColor = Color(0xFFA0A0A5)
val CapsuleTextColor = Color(0xFF1E1E20)
val CapsuleUnselectedTextColor = Color(0xFF4A4A50)
val RecordRedColor = Color(0xFFEF4444)

/**
 * Switch button between 'Free Rotate' and 'X-Axis Only' rotation modes
 */
@Composable
fun RotationModeSwitch(
    rotationMode: RotationMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xCC0E141E))
                .border(
                    width = 1.dp,
                    color = if (rotationMode == RotationMode.FREE) Color(0xFF00E5FF) else Color(0xFFFFB300),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.2f))
                ) { onToggle() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("btn_rotation_mode_toggle"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (rotationMode == RotationMode.FREE) Color(0xFF00E5FF) else Color(0xFFFFB300))
                )
                Text(
                    text = rotationMode.label,
                    color = Color(0xFFE2F3FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Top capsule segmented switch: [ Object | AR | MR ]
 */
@Composable
fun TopModeSelector(
    selectedMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(CapsuleBgColor)
                .padding(4.dp)
                .testTag("top_mode_selector"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Ordered from right-to-left: MR, AR, OBJECT as requested
                val orderedModes = listOf(ViewMode.MR, ViewMode.AR, ViewMode.OBJECT)
                orderedModes.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) CapsuleSelectedColor else Color.Transparent,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "mode_bg"
                    )
                    val textColor = if (isSelected) CapsuleTextColor else CapsuleUnselectedTextColor

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.2f))
                            ) { onModeSelected(mode) }
                            .padding(horizontal = 20.dp, vertical = 7.dp)
                            .testTag("tab_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom capsule action bar: [ PHOTO | (REC) | Open | Clean ]
 */
@Composable
fun BottomControlBar(
    isRecording: Boolean,
    onPhotoClick: () -> Unit,
    onRecClick: () -> Unit,
    onOpenClick: () -> Unit,
    onCleanClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClearClick: () -> Unit = onCleanClick
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val recPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_scale"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(CapsuleBgColor)
                .padding(horizontal = 14.dp, vertical = 5.dp)
                .testTag("bottom_control_bar"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PHOTO Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.2f))
                        ) { onPhotoClick() }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("btn_photo"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PHOTO",
                        color = CapsuleTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // REC Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(if (isRecording) recPulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(RecordRedColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f))
                        ) { onRecClick() }
                        .testTag("btn_rec"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // White Dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = if (isRecording) "STOP" else "REC",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                // Open Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.2f))
                        ) { onOpenClick() }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("btn_open"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open",
                        color = CapsuleTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Clean Button (Clears model from scene and purges cache and RAM)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.2f))
                        ) { onCleanClick() }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("btn_clean"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clean",
                        color = CapsuleTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
