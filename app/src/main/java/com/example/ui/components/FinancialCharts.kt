package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.LanguageManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class BarChartItem(
    val label: String,
    val value: Double,
    val subValue: Double = 0.0, // e.g. Profit or Secondary metric
    val color: Color = SkyPrimary,
    val subColor: Color = EmeraldProfit
)

data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * رسم بياني تفاعلي بالأعمدة (Interactive Bar Chart)
 * يدعم النقر على الأعمدة لعرض التفاصيل بدقة مع شريطين (المبيعات / الأرباح)
 */
@Composable
fun InteractiveBarChart(
    items: List<BarChartItem>,
    currency: AppCurrency,
    isAr: Boolean,
    modifier: Modifier = Modifier,
    unitLabel: String = ""
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAr) "لا توجد بيانات كافية لعرض المخطط" else "No data available for chart",
                fontSize = 12.sp,
                color = Slate400
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxValue = remember(items) {
        val max = items.maxOfOrNull { maxOf(it.value, it.subValue) } ?: 1.0
        if (max <= 0.0) 1.0 else max
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected Bar Details Tooltip Card
        if (selectedIndex != null && selectedIndex!! in items.indices) {
            val selected = items[selectedIndex!!]
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected.label,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isAr) "الإجمالي" else "Total",
                                fontSize = 9.sp,
                                color = Slate400
                            )
                            Text(
                                text = LanguageManager.formatMoney(selected.value, currency, isAr),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047)
                            )
                        }
                        if (selected.subValue > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isAr) "الربح" else "Profit",
                                    fontSize = 9.sp,
                                    color = Slate400
                                )
                                Text(
                                    text = LanguageManager.formatMoney(selected.subValue, currency, isAr),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldProfit
                                )
                            }
                        }
                    }
                }
            }
        }

        // The Canvas Bar Chart
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val bottomPadding = 30.dp.value * 2.5f
            val topPadding = 16.dp.value * 2f
            val chartHeight = height - bottomPadding - topPadding
            val barCount = items.size
            val slotWidth = width / barCount

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val clickedIndex = (offset.x / slotWidth).toInt().coerceIn(0, items.size - 1)
                            selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                        }
                    }
            ) {
                // Background grid lines (3 horizontal guideline levels)
                val lines = 3
                for (i in 0..lines) {
                    val y = topPadding + (chartHeight / lines) * i
                    drawLine(
                        color = Slate200,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Render Bars
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val slotX = index * slotWidth
                    val barWidth = (slotWidth * 0.45f).coerceAtLeast(10f).coerceAtMost(36f)
                    val barSpacing = slotWidth * 0.05f
                    val hasSub = item.subValue > 0

                    val barHeight1 = (item.value / maxValue).toFloat().coerceIn(0.04f, 1.0f) * chartHeight
                    val barY1 = topPadding + chartHeight - barHeight1

                    // First Bar (Main Value, e.g. Sales / Total)
                    val x1 = if (hasSub) slotX + (slotWidth - (barWidth * 2 + barSpacing)) / 2 else slotX + (slotWidth - barWidth) / 2
                    drawRoundRect(
                        color = if (isSelected) item.color else item.color.copy(alpha = 0.85f),
                        topLeft = Offset(x1, barY1),
                        size = Size(barWidth, barHeight1),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Second Bar (Sub Value, e.g. Profit)
                    if (hasSub) {
                        val barHeight2 = (item.subValue / maxValue).toFloat().coerceIn(0.04f, 1.0f) * chartHeight
                        val barY2 = topPadding + chartHeight - barHeight2
                        val x2 = x1 + barWidth + barSpacing
                        drawRoundRect(
                            color = if (isSelected) item.subColor else item.subColor.copy(alpha = 0.85f),
                            topLeft = Offset(x2, barY2),
                            size = Size(barWidth, barHeight2),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Bottom line
                    drawLine(
                        color = if (isSelected) SkyPrimary else Slate300,
                        start = Offset(slotX, topPadding + chartHeight),
                        end = Offset(slotX + slotWidth, topPadding + chartHeight),
                        strokeWidth = if (isSelected) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // Labels below bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SkyDeep else Slate600,
                        modifier = Modifier
                            .clickable { selectedIndex = if (selectedIndex == index) null else index }
                            .padding(2.dp)
                    )
                }
            }
        }

        // Legend indicator below chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(9.dp).background(SkyPrimary, RoundedCornerShape(2.dp)))
                Text(if (isAr) "إجمالي المبيعات / التكاليف" else "Total Revenue", fontSize = 10.sp, color = Slate600)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(9.dp).background(EmeraldProfit, RoundedCornerShape(2.dp)))
                Text(if (isAr) "صافي الأرباح" else "Net Profit", fontSize = 10.sp, color = Slate600)
            }
        }
    }
}

/**
 * رسم بياني تفاعلي دائري / الدونات (Interactive Donut Chart)
 * لتوزيع المصروفات أو فئات المخزون مع إمكانية النقر لاختيار الفئة
 */
@Composable
fun InteractiveDonutChart(
    slices: List<DonutSlice>,
    totalLabel: String,
    currency: AppCurrency,
    isAr: Boolean,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty() || slices.all { it.value <= 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAr) "لا توجد مصروفات أو بيانات مسجلة" else "No expense data recorded",
                fontSize = 12.sp,
                color = Slate400
            )
        }
        return
    }

    val totalValue = remember(slices) { slices.sumOf { it.value } }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Donut Canvas
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(slices) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Check which slice was tapped
                                var currentAngle = 0f
                                for (i in slices.indices) {
                                    val sliceSweep = (slices[i].value / totalValue * 360.0).toFloat()
                                    if (angle in currentAngle..(currentAngle + sliceSweep)) {
                                        selectedSliceIndex = if (selectedSliceIndex == i) null else i
                                        break
                                    }
                                    currentAngle += sliceSweep
                                }
                            }
                        }
                ) {
                    val strokeWidth = 26.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    var startAngle = -90f

                    slices.forEachIndexed { index, slice ->
                        val sweepAngle = ((slice.value / totalValue) * 360.0).toFloat()
                        val isSelected = selectedSliceIndex == index
                        val currentStroke = if (isSelected) strokeWidth * 1.2f else strokeWidth

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle.coerceAtLeast(1.5f),
                            useCenter = false,
                            topLeft = Offset((size.width - 2 * radius) / 2f, (size.height - 2 * radius) / 2f),
                            size = Size(2 * radius, 2 * radius),
                            style = Stroke(width = currentStroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Text Display inside Donut
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val currentDisplayValue = if (selectedSliceIndex != null) {
                        slices[selectedSliceIndex!!].value
                    } else totalValue

                    val currentDisplayLabel = if (selectedSliceIndex != null) {
                        slices[selectedSliceIndex!!].label
                    } else totalLabel

                    Text(
                        text = currentDisplayLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = LanguageManager.formatMoney(currentDisplayValue, currency, isAr),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            // Right: Legend Items List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedSliceIndex == index
                    val percent = if (totalValue > 0) (slice.value / totalValue * 100).toInt() else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) slice.color.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selectedSliceIndex = if (selectedSliceIndex == index) null else index }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(slice.color, CircleShape)
                            )
                            Text(
                                text = slice.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Slate800
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$percent%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = slice.color
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * شريط حالة ومؤشر مستويات المخزون (Stock Health Meter)
 */
@Composable
fun StockHealthMeter(
    adequateCount: Int,
    lowStockCount: Int,
    outOfStockCount: Int,
    isAr: Boolean,
    modifier: Modifier = Modifier
) {
    val total = (adequateCount + lowStockCount + outOfStockCount).coerceAtLeast(1)
    val adequateFraction = adequateCount.toFloat() / total
    val lowFraction = lowStockCount.toFloat() / total
    val outFraction = outOfStockCount.toFloat() / total

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAr) "سلامة وتوافر المخزون" else "Stock Availability",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
            Text(
                text = "${((adequateCount.toFloat() / total) * 100).toInt()}% " + (if (isAr) "متوفر بشكل مثالي" else "Optimal"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            if (adequateFraction > 0) {
                Box(modifier = Modifier.weight(adequateFraction).fillMaxHeight().background(StatusGreen))
            }
            if (lowFraction > 0) {
                Box(modifier = Modifier.weight(lowFraction).fillMaxHeight().background(AmberAccent))
            }
            if (outFraction > 0) {
                Box(modifier = Modifier.weight(outFraction).fillMaxHeight().background(RoseDebt))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(StatusGreen, CircleShape))
                Text(text = if (isAr) "كافي ($adequateCount)" else "Adequate ($adequateCount)", fontSize = 10.sp, color = Slate600)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(AmberAccent, CircleShape))
                Text(text = if (isAr) "منخفض ($lowStockCount)" else "Low ($lowStockCount)", fontSize = 10.sp, color = Slate600)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(RoseDebt, CircleShape))
                Text(text = if (isAr) "نافد ($outOfStockCount)" else "Out ($outOfStockCount)", fontSize = 10.sp, color = Slate600)
            }
        }
    }
}
