package com.spendora.ui.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spendora.data.model.SpendingTrendPoint
import java.util.Locale

@Composable
fun SpendingTrendChart(
    trendPoints: List<SpendingTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty()) {
        Card(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No spending trend data for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    val maxNet = trendPoints.maxOfOrNull { it.netSpending }?.coerceAtLeast(1.0) ?: 1.0
    val totalNet = trendPoints.sumOf { it.netSpending }
    val totalRefunds = trendPoints.sumOf { it.refundAmount }
    val avgSpending = if (trendPoints.isNotEmpty()) totalNet / trendPoints.size else 0.0

    val barColor = MaterialTheme.colorScheme.primary
    val refundColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Spending Trend", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Total: ₹${String.format(Locale.US, "%,.2f", totalNet)} • Avg: ₹${String.format(Locale.US, "%,.0f", avgSpending)}/interval",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (totalRefunds > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "incl. ₹${String.format(Locale.US, "%,.0f", totalRefunds)} refunds",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 20.dp.toPx() // Leave room for baseline
                    val count = trendPoints.size
                    val slotWidth = canvasWidth / count
                    val barWidth = (slotWidth * 0.65f).coerceIn(4.dp.toPx(), 28.dp.toPx())

                    // Baseline
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, canvasHeight),
                        end = Offset(canvasWidth, canvasHeight),
                        strokeWidth = 2f
                    )

                    // Draw bars
                    trendPoints.forEachIndexed { index, point ->
                        val x = (index * slotWidth) + (slotWidth - barWidth) / 2f
                        val barHeight = if (maxNet > 0) ((point.netSpending / maxNet) * (canvasHeight - 10f)).toFloat() else 0f
                        val y = canvasHeight - barHeight

                        if (barHeight > 0f) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        } else {
                            // Draw minimal tick for zero-spending days
                            drawCircle(
                                color = axisColor,
                                radius = 2.dp.toPx(),
                                center = Offset(x + barWidth / 2f, canvasHeight - 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // X-Axis readable label reduction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = when {
                    trendPoints.size > 20 -> 5
                    trendPoints.size > 10 -> 3
                    trendPoints.size > 6 -> 2
                    else -> 1
                }

                trendPoints.forEachIndexed { index, point ->
                    if (index % step == 0 || index == trendPoints.size - 1) {
                        val displayLabel = if (point.label.length >= 5) {
                            point.label.takeLast(5)
                        } else point.label

                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
