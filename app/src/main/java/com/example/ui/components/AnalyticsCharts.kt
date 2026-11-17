package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.analytics.AnalyticsFilterState
import com.example.data.analytics.BarChartEntry
import com.example.data.analytics.LabeledValuePoint
import com.example.data.analytics.PaymentMethodUsageItem
import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.BusinessTransaction
import com.example.data.model.DateRangeWindow
import com.example.data.model.formatKes
import com.example.data.model.isValidIsoDate
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberDark
import com.example.ui.theme.DemoAmberLight
import java.util.Locale

@Composable
fun AnalyticsDateFilterCard(
    filterState: AnalyticsFilterState,
    activeWindow: DateRangeWindow,
    onSelectFilter: (AnalyticsDateFilter) -> Unit,
    onApplyCustomRange: (startIso: String, endIso: String) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    var startInput by remember(filterState.customStartIso) { mutableStateOf(filterState.customStartIso) }
    var endInput by remember(filterState.customEndIso) { mutableStateOf(filterState.customEndIso) }
    var rangeError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reporting Date Filter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = activeWindow.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnalyticsDateFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filterState.filter == option,
                        onClick = { onSelectFilter(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("${testTagPrefix}_period_${option.name.lowercase()}")
                    )
                }
            }

            if (filterState.filter == AnalyticsDateFilter.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startInput,
                        onValueChange = {
                            startInput = it
                            rangeError = null
                        },
                        label = { Text("Start (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_custom_start_input")
                    )
                    OutlinedTextField(
                        value = endInput,
                        onValueChange = {
                            endInput = it
                            rangeError = null
                        },
                        label = { Text("End (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_custom_end_input")
                    )
                }
                if (rangeError != null) {
                    Text(
                        text = rangeError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = {
                        if (!isValidIsoDate(startInput) || !isValidIsoDate(endInput)) {
                            rangeError = "Enter both start and end dates in YYYY-MM-DD format."
                        } else {
                            rangeError = null
                            onApplyCustomRange(startInput.trim(), endInput.trim())
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_apply_custom_range_button")
                ) {
                    Text("Apply Custom Date Range")
                }
            }
        }
    }
}

@Composable
fun PerformanceTrendLineChartCard(
    title: String,
    subtitle: String,
    points: List<LabeledValuePoint>,
    primaryLabel: String,
    primaryColor: Color = BrandTeal,
    secondaryLabel: String? = null,
    secondaryColor: Color = BrandNavy,
    isDemoData: Boolean = false,
    emptyMessage: String = "No dated records in this period to plot a trend line.",
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isDemoData) {
                    Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                        Text(
                            text = "DEMO DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DemoAmberDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartLegendDot(label = primaryLabel, color = primaryColor)
                if (secondaryLabel != null) {
                    ChartLegendDot(label = secondaryLabel, color = secondaryColor)
                }
            }

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val allValues = buildList {
                    points.forEach { pt ->
                        add(pt.value)
                        if (secondaryLabel != null) add(pt.secondaryValue)
                    }
                }
                val minVal = (allValues.minOrNull() ?: 0.0).coerceAtMost(0.0)
                val maxVal = (allValues.maxOrNull() ?: 1.0).let { if (it <= minVal) minVal + 1.0 else it }
                val span = (maxVal - minVal).coerceAtLeast(1.0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val padH = 12.dp.toPx()
                        val padV = 12.dp.toPx()
                        val plotW = (size.width - padH * 2f).coerceAtLeast(10f)
                        val plotH = (size.height - padV * 2f).coerceAtLeast(10f)

                        for (step in 0..3) {
                            val y = padV + plotH * (step / 3f)
                            drawLine(
                                color = gridColor.copy(alpha = 0.45f),
                                start = Offset(padH, y),
                                end = Offset(size.width - padH, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        fun xForIndex(idx: Int): Float {
                            return if (points.size == 1) padH + plotW / 2f
                            else padH + (idx.toFloat() / (points.size - 1)) * plotW
                        }

                        fun yForValue(v: Double): Float {
                            val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                            return padV + plotH * (1f - norm)
                        }

                        if (secondaryLabel != null && points.size >= 2) {
                            val secPath = Path()
                            points.forEachIndexed { idx, pt ->
                                val x = xForIndex(idx)
                                val y = yForValue(pt.secondaryValue)
                                if (idx == 0) secPath.moveTo(x, y) else secPath.lineTo(x, y)
                            }
                            drawPath(
                                path = secPath,
                                color = secondaryColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        if (secondaryLabel != null) {
                            points.forEachIndexed { idx, pt ->
                                drawCircle(
                                    color = secondaryColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(xForIndex(idx), yForValue(pt.secondaryValue))
                                )
                            }
                        }

                        if (points.size >= 2) {
                            val priPath = Path()
                            points.forEachIndexed { idx, pt ->
                                val x = xForIndex(idx)
                                val y = yForValue(pt.value)
                                if (idx == 0) priPath.moveTo(x, y) else priPath.lineTo(x, y)
                            }
                            drawPath(
                                path = priPath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        points.forEachIndexed { idx, pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 5.5.dp.toPx(),
                                center = Offset(xForIndex(idx), yForValue(pt.value))
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 4.dp.toPx(),
                                center = Offset(xForIndex(idx), yForValue(pt.value))
                            )
                        }
                    }
                }

                val labelStep = if (points.size > 6) (points.size / 5).coerceAtLeast(1) else 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        if (idx % labelStep == 0 || idx == points.lastIndex) {
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryComparisonBarChartCard(
    title: String,
    subtitle: String,
    bars: List<LabeledValuePoint>,
    barColor: Color = BrandNavy,
    valueFormatter: (Double) -> String = { "${it.toInt()}" },
    isDemoData: Boolean = false,
    emptyMessage: String = "No category entries available in this period.",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isDemoData) {
                    Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                        Text(
                            text = "DEMO DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DemoAmberDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (bars.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val maxVal = bars.maxOf { it.value }.coerceAtLeast(1.0)
                bars.take(6).forEach { bar ->
                    val fraction = (bar.value / maxVal).toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bar.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = valueFormatter(bar.value),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            drawRoundRect(
                                color = barColor.copy(alpha = 0.14f),
                                size = size,
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width * fraction.coerceAtLeast(if (bar.value > 0) 0.03f else 0f), size.height),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodDonutChartCard(
    title: String,
    subtitle: String,
    breakdown: List<PaymentMethodUsageItem>,
    isDemoData: Boolean,
    modifier: Modifier = Modifier
) {
    val colorForMethod: (String) -> Color = { code ->
        when (code) {
            BusinessTransaction.METHOD_MPESA -> BrandTeal
            BusinessTransaction.METHOD_BANK -> BrandNavy
            BusinessTransaction.METHOD_CARD -> BrandTealDark
            BusinessTransaction.METHOD_OTHER_DIGITAL -> DemoAmber
            else -> AlertCoral
        }
    }
    val nonZero = breakdown.filter { it.totalAmountKes > 0.0 }
    val grandTotalKes = nonZero.sumOf { it.totalAmountKes }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isDemoData) {
                    Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                        Text(
                            text = "DEMO DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DemoAmberDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (nonZero.isEmpty() || grandTotalKes <= 0.0) {
                Text(
                    text = "No payment transactions in this date range to render method distribution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val sideBySide = maxWidth >= 420.dp
                    val donutContent: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier.size(144.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 22.dp.toPx()
                                val inset = strokePx / 2f
                                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                                var startAngle = -90f
                                nonZero.forEach { item ->
                                    val sweep = ((item.totalAmountKes / grandTotalKes) * 360.0).toFloat()
                                    drawArc(
                                        color = colorForMethod(item.methodCode),
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - 2f).coerceAtLeast(2f),
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatKes(grandTotalKes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val legendContent: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            breakdown.forEach { item ->
                                val pct = if (grandTotalKes > 0.0) (item.totalAmountKes / grandTotalKes) * 100.0 else 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(colorForMethod(item.methodCode))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${item.displayLabel} (${item.transactionCount})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${formatKes(item.totalAmountKes)} (${String.format(Locale.US, "%.0f%%", pct)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (sideBySide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            donutContent()
                            Box(modifier = Modifier.weight(1f)) {
                                legendContent()
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            donutContent()
                            legendContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalGroupedBarChartCard(
    title: String,
    subtitle: String,
    entries: List<BarChartEntry>,
    primaryLabel: String,
    primaryColor: Color = Color(0xFF7C3AED),
    secondaryLabel: String? = null,
    secondaryColor: Color = BrandTeal,
    valueFormatter: (Double) -> String = { formatKes(it) },
    isDemoData: Boolean = false,
    emptyMessage: String = "No records available to plot this bar graph.",
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isDemoData) {
                    Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                        Text(
                            text = "DEMO DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DemoAmberDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartLegendDot(label = primaryLabel, color = primaryColor)
                if (secondaryLabel != null) {
                    ChartLegendDot(label = secondaryLabel, color = secondaryColor)
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val allValues = buildList {
                    entries.forEach { e ->
                        add(e.primaryValue)
                        e.secondaryValue?.let { add(it) }
                    }
                }
                val minVal = (allValues.minOrNull() ?: 0.0).coerceAtMost(0.0)
                val maxVal = (allValues.maxOrNull() ?: 1.0).let { if (it <= minVal) minVal + 1.0 else it }
                val span = (maxVal - minVal).coerceAtLeast(1.0)
                val hasSecondary = secondaryLabel != null && entries.any { it.secondaryValue != null }

                val minSlotDp = 68.dp
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val contentWidth = (entries.size * minSlotDp.value).dp.coerceAtLeast(maxWidth)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .width(contentWidth)
                                .height(168.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val padH = 10.dp.toPx()
                                val padTop = 10.dp.toPx()
                                val padBottom = 10.dp.toPx()
                                val plotH = (size.height - padTop - padBottom).coerceAtLeast(12f)
                                val zeroY = padTop + plotH * ((maxVal - 0.0) / span).toFloat().coerceIn(0f, 1f)

                                for (step in 0..3) {
                                    val gy = padTop + plotH * (step / 3f)
                                    drawLine(
                                        color = gridColor.copy(alpha = 0.45f),
                                        start = Offset(padH, gy),
                                        end = Offset(size.width - padH, gy),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                // Emphasized zero baseline when negative values exist
                                if (minVal < 0.0) {
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(padH, zeroY),
                                        end = Offset(size.width - padH, zeroY),
                                        strokeWidth = 1.6.dp.toPx()
                                    )
                                }

                                val usableW = (size.width - padH * 2f).coerceAtLeast(20f)
                                val slotW = usableW / entries.size.coerceAtLeast(1)
                                val radiusPx = 5.dp.toPx()

                                entries.forEachIndexed { idx, entry ->
                                    val slotStart = padH + idx * slotW
                                    val groupW = (slotW * 0.68f).coerceAtLeast(12f)
                                    val groupLeft = slotStart + (slotW - groupW) / 2f

                                    fun drawBar(value: Double, x: Float, width: Float, color: Color) {
                                        val valY = padTop + plotH * ((maxVal - value) / span).toFloat().coerceIn(0f, 1f)
                                        val topY = minOf(valY, zeroY)
                                        val barH = kotlin.math.abs(zeroY - valY).coerceAtLeast(if (value != 0.0) 4f else 0f)
                                        if (barH > 0f) {
                                            drawRoundRect(
                                                color = color,
                                                topLeft = Offset(x, topY),
                                                size = Size(width, barH),
                                                cornerRadius = CornerRadius(radiusPx, radiusPx)
                                            )
                                        }
                                    }

                                    if (hasSecondary) {
                                        val gap = 4.dp.toPx().coerceAtMost(groupW * 0.18f)
                                        val singleW = ((groupW - gap) / 2f).coerceAtLeast(5f)
                                        drawBar(entry.primaryValue, groupLeft, singleW, primaryColor)
                                        entry.secondaryValue?.let { secVal ->
                                            drawBar(secVal, groupLeft + singleW + gap, singleW, secondaryColor)
                                        }
                                    } else {
                                        drawBar(entry.primaryValue, groupLeft, groupW, primaryColor)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.width(contentWidth),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            entries.forEach { entry ->
                                Column(
                                    modifier = Modifier.width((contentWidth / entries.size.coerceAtLeast(1))),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = entry.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = valueFormatter(entry.primaryValue),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!entry.badge.isNullOrBlank()) {
                                        val isProj = entry.badge == "PROJ"
                                        val isLow = entry.badge == "LOW"
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when {
                                                isLow -> AlertCoral.copy(alpha = 0.18f)
                                                isProj -> Color(0xFFEDE9FE)
                                                else -> BrandTeal.copy(alpha = 0.16f)
                                            }
                                        ) {
                                            Text(
                                                text = entry.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isLow -> AlertCoral
                                                    isProj -> Color(0xFF5B21B6)
                                                    else -> BrandTealDark
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
