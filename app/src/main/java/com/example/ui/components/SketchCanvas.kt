package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun SketchCanvasDialog(
    initialData: String?,
    onSaveSketch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val paths = remember { mutableStateListOf<DrawnPath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var currentColor by remember { mutableStateOf(Color(0xFF38BDF8)) }
    var currentStrokeWidth by remember { mutableStateOf(6f) }

    val colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFF38BDF8), // Cyan
        Color(0xFFA78BFA), // Violet
        Color(0xFFF43F5E), // Rose
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981)  // Emerald
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✍️ Sketch & Draw",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex) },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.LightGray)
                    }

                    IconButton(
                        onClick = { paths.clear(); currentPoints = emptyList() },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            // Serialize path points count or summary
                            val serialized = "sketch:${paths.size}strokes"
                            onSaveSketch(serialized)
                            onDismiss()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF06B6D4)
                        )
                    ) {
                        Text("Done", color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tool bar: colors and stroke sizes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (currentColor == c) 2.5.dp else 0.dp,
                                color = if (currentColor == c) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { currentColor = c }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Stroke width buttons
                listOf(3f to "Fine", 7f to "Med", 14f to "Bold").forEach { (width, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentStrokeWidth == width) Color(0xFF334155) else Color.Transparent)
                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                            .clickable { currentStrokeWidth = width }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(label, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    paths.add(DrawnPath(currentPoints, currentColor, currentStrokeWidth))
                                    currentPoints = emptyList()
                                }
                            },
                            onDragCancel = {
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw saved paths
                    paths.forEach { drawn ->
                        if (drawn.points.size > 1) {
                            val path = Path().apply {
                                moveTo(drawn.points[0].x, drawn.points[0].y)
                                for (i in 1 until drawn.points.size) {
                                    lineTo(drawn.points[i].x, drawn.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = drawn.color,
                                style = Stroke(
                                    width = drawn.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw active dragging path
                    if (currentPoints.size > 1) {
                        val path = Path().apply {
                            moveTo(currentPoints[0].x, currentPoints[0].y)
                            for (i in 1 until currentPoints.size) {
                                lineTo(currentPoints[i].x, currentPoints[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = currentColor,
                            style = Stroke(
                                width = currentStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}
