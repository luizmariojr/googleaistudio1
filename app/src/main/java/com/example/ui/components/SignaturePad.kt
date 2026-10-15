package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PointDto

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureChanged: (List<PointDto>) -> Unit
) {
    val points = remember { mutableStateListOf<PointDto>() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Assinatura do Recebedor",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedButton(
                onClick = {
                    points.clear()
                    onSignatureChanged(emptyList())
                },
                modifier = Modifier.testTag("clear_signature_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpar Assinatura",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Limpar")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(
                    width = 1.5.dp,
                    color = if (points.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag("signature_canvas_container")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("signature_canvas")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                points.add(PointDto(offset.x, offset.y, isBreak = false))
                                onSignatureChanged(points.toList())
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                points.add(PointDto(change.position.x, change.position.y, isBreak = false))
                                onSignatureChanged(points.toList())
                            },
                            onDragEnd = {
                                if (points.isNotEmpty()) {
                                    val last = points.last()
                                    points[points.size - 1] = last.copy(isBreak = true)
                                    onSignatureChanged(points.toList())
                                }
                            }
                        )
                    }
            ) {
                // Baseline guide line
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.6f),
                    start = Offset(20f, size.height * 0.75f),
                    end = Offset(size.width - 20f, size.height * 0.75f),
                    strokeWidth = 2f
                )

                if (points.isNotEmpty()) {
                    val path = Path()
                    var isNewSegment = true

                    for (i in points.indices) {
                        val pt = points[i]
                        if (isNewSegment) {
                            path.moveTo(pt.x, pt.y)
                            isNewSegment = false
                        } else {
                            path.lineTo(pt.x, pt.y)
                        }

                        if (pt.isBreak) {
                            isNewSegment = true
                        }
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF1A237E), // Professional blue-black ink
                        style = Stroke(
                            width = 4.5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (points.isEmpty()) {
                Text(
                    text = "Assine aqui com o dedo ou caneta stylus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
