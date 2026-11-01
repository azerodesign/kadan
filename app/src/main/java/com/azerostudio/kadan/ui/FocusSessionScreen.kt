package com.azerostudio.kadan.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azerostudio.kadan.data.Task
import com.azerostudio.kadan.ui.theme.CyanFixedDim
import com.azerostudio.kadan.ui.theme.KadanBlue
import com.azerostudio.kadan.ui.theme.KadanFocusDarkBackground
import com.azerostudio.kadan.ui.theme.KadanTheme
import kotlinx.coroutines.delay

/*
 * FocusSessionScreen — Screen Sesi Fokus & Full-Screen Reminder Kadan.
 *
 * Implements the focus session UI with dark cyber-minimalist atmosphere:
 *  - Ambient cyan radial glow overlay
 *  - Status mimic bar with pulsing cyan session indicator and silent status
 *  - Central squircle timer module with exact vector stopwatch illustration
 *  - Category and urgency pills
 *  - Interactive Command Hub with Emergency dialer, gradient Start/Finish button,
 *    and 10m Snooze trigger
 */
@Composable
fun FocusSessionScreen(
    task: Task?,
    onDismiss: () -> Unit = {},
    onStartFocus: () -> Unit = {},
    onSnooze: (minutes: Int) -> Unit = {},
    onEmergencyCall: () -> Unit = {},
    onCompleteTask: (Task) -> Unit = {}
) {
    var isSessionActive by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(90 * 60) }

    LaunchedEffect(isSessionActive) {
        if (isSessionActive) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val startInteraction = remember { MutableInteractionSource() }
    val isStartPressed by startInteraction.collectIsPressedAsState()
    val startScale by animateFloatAsState(if (isStartPressed) 0.96f else 1f, label = "startScale")

    val emergencyInteraction = remember { MutableInteractionSource() }
    val isEmergencyPressed by emergencyInteraction.collectIsPressedAsState()
    val emergencyScale by animateFloatAsState(if (isEmergencyPressed) 0.92f else 1f, label = "emergencyScale")

    val snoozeInteraction = remember { MutableInteractionSource() }
    val isSnoozePressed by snoozeInteraction.collectIsPressedAsState()
    val snoozeScale by animateFloatAsState(if (isSnoozePressed) 0.92f else 1f, label = "snoozeScale")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KadanFocusDarkBackground)
            .drawBehind {
                val canvasCenter = center
                val glowRadius = size.minDimension * 0.72f * glowScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanFixedDim.copy(alpha = 0.16f),
                            KadanBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = canvasCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = canvasCenter
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle Top System Status Bar Mimic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Pulsing cyan dot + "KADAN SESSION"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyanFixedDim.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "KADAN SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right: Silent status + Dismiss button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.alpha(0.7f)
                    ) {
                        SilentDndIcon(
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Silent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Exit / close button to return to task list
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val stroke = 1.8f.dp.toPx()
                            val color = Color(0xFFC3C6D7).copy(alpha = 0.8f)
                            drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
                            drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                }
            }

            // Central Distraction-Free Focus Graphic & Content Module
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glow & Icon Squircle Box
                Box(
                    modifier = Modifier.padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer cyan glow layer
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(CyanFixedDim.copy(alpha = 0.22f))
                            .blur(14.dp)
                    )

                    // Core Container Box
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(
                                width = 1.dp,
                                color = CyanFixedDim.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FocusTimerGraphic(
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Category & Urgency Pill Badge
                val categoryName = task?.category ?: "Kerja"
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cyan glowing dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyanFixedDim)
                        )

                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            PriorityHighIcon(
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Mendesak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Headline Title
                Text(
                    text = if (isSessionActive) "Sesi Fokus Berjalan" else "Waktunya Fokus",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Task Title / Body
                val taskTitle = task?.title?.ifBlank { null } ?: "Review draft kontrak klien baru"
                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 20.dp)
                )

                // Time / Schedule Pill Badge
                val timeRangeText = if (isSessionActive) {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    "%02d:%02d tersisa • Mode Aktif".format(m, s)
                } else if (task?.dueTimeMinutes != null) {
                    val startH = task.dueTimeMinutes / 60
                    val startM = task.dueTimeMinutes % 60
                    val endTotal = task.dueTimeMinutes + 90
                    val endH = (endTotal / 60) % 24
                    val endM = endTotal % 60
                    "%02d:%02d - %02d:%02d WIB • 90 Menit".format(startH, startM, endH, endM)
                } else {
                    "09:00 - 10:30 WIB • 90 Menit"
                }

                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScheduleIcon(
                            modifier = Modifier.size(16.dp),
                            tint = CyanFixedDim
                        )
                        Text(
                            text = timeRangeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Mode Status Pill Badge
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LockIcon(
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Mode fokus aktif • Notifikasi dibisukan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Bottom Floating Interactive Command Hub
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Action Triggers Row (max width 342.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 342.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Emergency Phone Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(emergencyScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable(
                                interactionSource = emergencyInteraction,
                                indication = null,
                                onClick = onEmergencyCall
                            )
                            .semantics { contentDescription = "Panggilan Darurat" },
                        contentAlignment = Alignment.Center
                    ) {
                        PhoneIcon(
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Center: Primary Action Button ("Mulai Sesi Fokus" or "Selesaikan Tugas")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .scale(startScale)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(percent = 50),
                                ambientColor = CyanFixedDim,
                                spotColor = CyanFixedDim
                            )
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                brush = if (!isSessionActive) {
                                    Brush.linearGradient(listOf(CyanFixedDim, KadanBlue))
                                } else {
                                    Brush.linearGradient(listOf(KadanBlue, Color(0xFF10B981)))
                                }
                            )
                            .clickable(
                                interactionSource = startInteraction,
                                indication = null,
                                onClick = {
                                    if (!isSessionActive) {
                                        isSessionActive = true
                                        onStartFocus()
                                    } else {
                                        if (task != null) {
                                            onCompleteTask(task)
                                        } else {
                                            isSessionActive = false
                                        }
                                    }
                                }
                            )
                            .semantics {
                                contentDescription = if (!isSessionActive) "Mulai Sesi Fokus" else "Selesaikan Tugas"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isSessionActive) {
                                PlayIcon(
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                                Text(
                                    text = "Mulai Sesi Fokus",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp,
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            } else {
                                CheckIcon(
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = "Selesaikan Tugas",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Right: Snooze Button ("10m Tunda")
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(snoozeScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable(
                                interactionSource = snoozeInteraction,
                                indication = null,
                                onClick = { onSnooze(10) }
                            )
                            .semantics { contentDescription = "Tunda 10 Menit" },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "10m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 13.sp
                            )
                            Text(
                                text = "TUNDA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                lineHeight = 9.sp
                            )
                        }
                    }
                }

                // Minimalist Android Gesture Line Indicator
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 2.dp)
                        .width(128.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Vector Illustrations & Icons (Exact replicas of design mockup)
// -------------------------------------------------------------

@Composable
fun FocusTimerGraphic(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFBDF4FF)
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 1.8f.dp.toPx()

        // Dial circle at (12, 13) radius 8
        drawCircle(
            color = tint,
            radius = 8f * s,
            center = Offset(12f * s, 13f * s),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Hands: (12, 9) -> (12, 13) -> (14.5, 15)
        val hands = Path().apply {
            moveTo(12f * s, 9f * s)
            lineTo(12f * s, 13f * s)
            lineTo(14.5f * s, 15f * s)
        }
        drawPath(
            path = hands,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Top button bar: (10, 2) to (14, 2)
        drawLine(
            color = tint,
            start = Offset(10f * s, 2f * s),
            end = Offset(14f * s, 2f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Top stem: (12, 2) to (12, 5)
        drawLine(
            color = tint,
            start = Offset(12f * s, 2f * s),
            end = Offset(12f * s, 5f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Top-right side pusher: (19.5, 7.5) to (21, 6)
        drawLine(
            color = tint,
            start = Offset(19.5f * s, 7.5f * s),
            end = Offset(21f * s, 6f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PhoneIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFC3C6D7)
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 1.8f.dp.toPx()
        val path = Path().apply {
            moveTo(6.5f * s, 4f * s)
            cubicTo(6.5f * s, 3f * s, 7.5f * s, 2.5f * s, 8.5f * s, 3f * s)
            lineTo(10.2f * s, 5.2f * s)
            cubicTo(10.8f * s, 6f * s, 10.6f * s, 7f * s, 9.8f * s, 7.6f * s)
            lineTo(8.9f * s, 8.3f * s)
            cubicTo(9.7f * s, 10.8f * s, 11.7f * s, 12.8f * s, 14.2f * s, 13.6f * s)
            lineTo(14.9f * s, 12.7f * s)
            cubicTo(15.5f * s, 11.9f * s, 16.5f * s, 11.7f * s, 17.3f * s, 12.3f * s)
            lineTo(19.5f * s, 14f * s)
            cubicTo(20f * s, 15f * s, 19.5f * s, 16f * s, 18.5f * s, 16.5f * s)
            cubicTo(15.5f * s, 18.5f * s, 11f * s, 17.5f * s, 7.5f * s, 14f * s)
            cubicTo(4f * s, 10.5f * s, 3f * s, 6f * s, 5f * s, 3f * s)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun PlayIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val path = Path().apply {
            moveTo(8.5f * s, 5f * s)
            lineTo(19f * s, 12f * s)
            lineTo(8.5f * s, 19f * s)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

@Composable
fun CheckIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 2.4f.dp.toPx()
        val path = Path().apply {
            moveTo(5f * s, 12f * s)
            lineTo(10f * s, 17f * s)
            lineTo(19f * s, 7f * s)
        }
        drawPath(path = path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LockIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 1.6f.dp.toPx()
        val shackle = Path().apply {
            moveTo(8f * s, 11f * s)
            lineTo(8f * s, 7.5f * s)
            cubicTo(8f * s, 5f * s, 16f * s, 5f * s, 16f * s, 7.5f * s)
            lineTo(16f * s, 11f * s)
        }
        drawPath(shackle, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawRoundRect(
            color = tint,
            topLeft = Offset(6f * s, 11f * s),
            size = Size(12f * s, 9f * s),
            cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun SilentDndIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 1.6f.dp.toPx()
        drawCircle(
            color = tint,
            radius = 8f * s,
            center = Offset(12f * s, 12f * s),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(8f * s, 12f * s),
            end = Offset(16f * s, 12f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PriorityHighIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 2f.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(12f * s, 5.5f * s),
            end = Offset(12f * s, 13.5f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = 1.3f * s,
            center = Offset(12f * s, 17f * s)
        )
    }
}

@Composable
fun ScheduleIcon(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val stroke = 1.6f.dp.toPx()
        drawCircle(
            color = tint,
            radius = 8f * s,
            center = Offset(12f * s, 12f * s),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(12f * s, 7f * s),
            end = Offset(12f * s, 12f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(12f * s, 12f * s),
            end = Offset(15.5f * s, 14f * s),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Preview
@Composable
fun FocusSessionPreview() {
    KadanTheme(darkTheme = true) {
        FocusSessionScreen(
            task = Task(
                id = 1,
                title = "Review draft kontrak klien baru",
                category = "Kerja",
                dueDate = 20000,
                dueTimeMinutes = 9 * 60
            )
        )
    }
}
