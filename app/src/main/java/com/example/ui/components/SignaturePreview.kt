package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.PointDto

@Composable
fun SignaturePreview(
    points: List<PointDto>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFAFAFA))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        if (points.isEmpty()) {
            Text(
                text = "Assinatura eletrônica registrada",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(16f, size.height * 0.8f),
                    end = Offset(size.width - 16f, size.height * 0.8f),
                    strokeWidth = 1.5f
                )

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
                    color = Color(0xFF0D47A1),
                    style = Stroke(
                        width = 3.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
