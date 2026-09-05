package com.spendora.ui.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.spendora.data.model.CategorySpendingItem
import java.util.Locale

@Composable
fun CategoryDonutChart(
    items: List<CategorySpendingItem>,
    totalSpending: Double,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || totalSpending <= 0.0) {
        Card(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No spending recorded for this cycle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    val chartColors = listOf(
        Color(0xFF9333EA), // Purple
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF6366F1), // Indigo
        Color(0xFF14B8A6), // Teal
        Color(0xFF8B5CF6)  // Violet
    )

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Spending by Category", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        var startAngle = -90f
                        val strokeWidth = 24.dp.toPx()

                        items.take(8).forEachIndexed { index, item ->
                            val sweepAngle = ((item.netAmount / totalSpending) * 360f).toFloat()
                            val color = runCatching {
                                Color(android.graphics.Color.parseColor(item.colorHex))
                            }.getOrDefault(chartColors[index % chartColors.size])

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(size.width, size.height),
                                topLeft = Offset.Zero
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%,.0f", totalSpending)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Legend (Top categories)
                Column(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.take(4).forEachIndexed { index, item ->
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(item.colorHex))
                        }.getOrDefault(chartColors[index % chartColors.size])

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = color
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${item.categoryName} (${String.format(Locale.US, "%.1f", item.percentageOfTotal)}%)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
