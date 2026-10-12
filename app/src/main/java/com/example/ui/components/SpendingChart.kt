package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CurrencyUtils
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.VioletInsight
import com.example.ui.viewmodel.CategorySpend

@Composable
fun MonthlySpendingBarChart(
    dailyExpensesPaise: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    if (dailyExpensesPaise.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NavySurfaceHigh.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending recorded for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxPaise = (dailyExpensesPaise.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(10000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Peak: ${CurrencyUtils.formatPaiseToRupees(maxPaise, showDecimals = false)}",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas Bar Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = dailyExpensesPaise.size
            val barSpacing = 12.dp.toPx()
            val totalSpacing = barSpacing * (count - 1)
            val barWidth = ((size.width - totalSpacing) / count).coerceIn(8.dp.toPx(), 28.dp.toPx())
            val chartHeight = size.height

            // Background baseline grid line
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 2.dp.toPx()
            )

            dailyExpensesPaise.forEachIndexed { index, (label, amount) ->
                val barHeight = (amount.toFloat() / maxPaise.toFloat()) * (chartHeight * 0.9f)
                val x = index * (barWidth + barSpacing)
                val y = chartHeight - barHeight

                val gradient = Brush.verticalGradient(
                    colors = listOf(ElectricCyan, ElectricBlue),
                    startY = y,
                    endY = chartHeight
                )

                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Labels under bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyExpensesPaise.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun CategoryProgressRow(
    categorySpend: CategorySpend,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (categorySpend.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "category_progress"
    )

    val categoryColor = when (categorySpend.category) {
        "Food & Dining" -> Color(0xFFF97316)
        "Bills & Utilities" -> Color(0xFF3B82F6)
        "Shopping" -> Color(0xFFEC4899)
        "Entertainment" -> Color(0xFF8B5CF6)
        "Investments" -> EmeraldGreen
        "Travel & Commute" -> Color(0xFF06B6D4)
        "Loans & EMI" -> Color(0xFFEF4444)
        else -> ElectricCyan
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = categorySpend.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${categorySpend.transactionCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (hideAmounts) "••••" else CurrencyUtils.formatPaiseToRupees(categorySpend.totalPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar track and fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(categoryColor)
            )
        }
    }
}
