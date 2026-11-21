package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrafficSample
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RealtimeTrafficChartCard(
    samples: List<TrafficSample>,
    currentRxBps: Long,
    currentTxBps: Long,
    interfaceLabel: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(samples.size) { mutableStateOf<Int?>(null) }

    val peakRx = remember(samples) { samples.maxOfOrNull { it.rxBps } ?: 0L }
    val peakTx = remember(samples) { samples.maxOfOrNull { it.txBps } ?: 0L }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grafik Trafik Real-Time",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Interface: $interfaceLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Live Download / Upload pill readouts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThroughputPill(
                        label = "DL",
                        value = Formatters.formatBps(currentRxBps),
                        color = NocBlueDownload,
                        isDownload = true
                    )
                    ThroughputPill(
                        label = "UL",
                        value = Formatters.formatBps(currentTxBps),
                        color = NocEmeraldOnline,
                        isDownload = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (samples.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Menunggu sampel trafik dari router...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxBps = remember(samples) {
                    val m = samples.maxOf { maxOf(it.rxBps, it.txBps) }
                    maxOf(m, 100_000L) // minimum 100 Kbps scale
                }
                val yStepLabelTop = Formatters.formatBps(maxBps)
                val yStepLabelMid = Formatters.formatBps(maxBps / 2)

                val outlineColor = MaterialTheme.colorScheme.outline
                val surfaceElevated = MaterialTheme.colorScheme.surfaceVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceElevated.copy(alpha = 0.35f))
                        .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(samples) {
                                detectTapGestures { offset ->
                                    if (samples.size > 1) {
                                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                        selectedIndex = (ratio * (samples.size - 1)).roundToInt()
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                        // Horizontal Gridlines
                        listOf(0.08f, 0.5f, 0.92f).forEach { yRatio ->
                            val y = h * yRatio
                            drawLine(
                                color = outlineColor.copy(alpha = 0.7f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }

                        if (samples.size >= 2) {
                            val stepX = w / (samples.size - 1).toFloat()
                            val rxPoints = samples.mapIndexed { idx, s ->
                                val norm = (s.rxBps.toFloat() / maxBps.toFloat()).coerceIn(0f, 1f)
                                Offset(idx * stepX, h * (0.92f - norm * 0.82f))
                            }
                            val txPoints = samples.mapIndexed { idx, s ->
                                val norm = (s.txBps.toFloat() / maxBps.toFloat()).coerceIn(0f, 1f)
                                Offset(idx * stepX, h * (0.92f - norm * 0.82f))
                            }

                            drawSmoothAreaAndLine(rxPoints, h * 0.92f, NocBlueDownload, 0.24f)
                            drawSmoothAreaAndLine(txPoints, h * 0.92f, NocEmeraldOnline, 0.18f)

                            selectedIndex?.let { sel ->
                                val idx = sel.coerceIn(0, samples.lastIndex)
                                val x = rxPoints[idx].x
                                drawLine(
                                    color = Color.White.copy(alpha = 0.45f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, h * 0.95f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawCircle(
                                    color = NocBlueDownload,
                                    radius = 4.5.dp.toPx(),
                                    center = rxPoints[idx]
                                )
                                drawCircle(
                                    color = NocEmeraldOnline,
                                    radius = 4.5.dp.toPx(),
                                    center = txPoints[idx]
                                )
                            }
                        } else if (samples.size == 1) {
                            val s = samples.first()
                            val rxNorm = (s.rxBps.toFloat() / maxBps.toFloat()).coerceIn(0f, 1f)
                            val txNorm = (s.txBps.toFloat() / maxBps.toFloat()).coerceIn(0f, 1f)
                            val rxY = h * (0.92f - rxNorm * 0.82f)
                            val txY = h * (0.92f - txNorm * 0.82f)
                            drawLine(NocBlueDownload, Offset(0f, rxY), Offset(w, rxY), strokeWidth = 2.5.dp.toPx())
                            drawLine(NocEmeraldOnline, Offset(0f, txY), Offset(w, txY), strokeWidth = 2.5.dp.toPx())
                        }
                    }

                    // Y-axis scale labels
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = yStepLabelTop,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Text(
                            text = yStepLabelMid,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "0 bps",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selected point or Peak summary row
                val inspected = selectedIndex?.let { samples.getOrNull(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inspected != null) {
                        Text(
                            text = "${Formatters.formatTime(inspected.timestampMillis)} • DL ${Formatters.formatBps(inspected.rxBps)} / UL ${Formatters.formatBps(inspected.txBps)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Puncak DL: ${Formatters.formatBps(peakRx)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = NocBlueDownload
                        )
                        Text(
                            text = "Puncak UL: ${Formatters.formatBps(peakTx)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = NocEmeraldOnline
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothAreaAndLine(
    points: List<Offset>,
    baselineY: Float,
    color: Color,
    fillAlpha: Float
) {
    if (points.size < 2) return
    val linePath = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.lastIndex) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        }
    }
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(points.last().x, baselineY)
        lineTo(points.first().x, baselineY)
        close()
    }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = fillAlpha), Color.Transparent),
            startY = 0f,
            endY = baselineY
        )
    )
    drawPath(
        path = linePath,
        color = color,
        style = Stroke(
            width = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

@Composable
private fun ThroughputPill(
    label: String,
    value: String,
    color: Color,
    isDownload: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDownload) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = color
        )
    }
}
