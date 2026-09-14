package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportManager
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.DailyWorkStat
import com.example.ui.viewmodel.ProjectWorkStat
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.TrackerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val filteredEntries by viewModel.filteredReportEntries.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val projectDistribution by viewModel.projectDistribution.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val projectSummaries by viewModel.projectSummaries.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }

    val totalSeconds = filteredEntries.sumOf { it.durationSeconds }
    val totalHours = totalSeconds / 3600.0
    val billableSeconds = filteredEntries.filter { it.isBillable }.sumOf { it.durationSeconds }
    val billableHours = billableSeconds / 3600.0
    val totalEarnings = filteredEntries.filter { it.isBillable }.sumOf {
        (it.durationSeconds / 3600.0) * it.hourlyRate
    }
    val billableRatio = if (totalSeconds > 0) (billableSeconds.toDouble() / totalSeconds.toDouble()).toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Period Selector & Export Button Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productivity Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("export_reports_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Period Filters
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReportPeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setReportPeriod(period) },
                        label = { Text(period.label) },
                        modifier = Modifier.testTag("period_chip_${period.name}")
                    )
                }
            }
        }

        // Executive Stat Cards Grid
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Total Hours Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOTAL TIME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalHours)} hrs",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ExportManager.formatDurationHms(totalSeconds),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Billable Earnings Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BILLABLE EARNINGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.0f", totalEarnings)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", billableHours)} hrs (${(billableRatio * 100).toInt()}%)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Daily Work Hours Bar Chart Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("productivity_bar_chart_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Hours Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Billable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (dailyStats.isEmpty() || dailyStats.all { it.totalHours == 0.0 }) {
                        Text(
                            text = "No recorded sessions in this period.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        DailyHoursChart(dailyStats = dailyStats)
                    }
                }
            }
        }

        // Project Distribution Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_distribution_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project Time Allocation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (projectDistribution.isEmpty()) {
                        Text(
                            text = "No project activity in this period.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Segmented bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            projectDistribution.forEach { item ->
                                val pColor = try {
                                    Color(android.graphics.Color.parseColor(item.projectColor))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(item.percentage.coerceAtLeast(0.01f))
                                        .height(12.dp)
                                        .background(pColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Projects list
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            projectDistribution.forEach { p ->
                                val pColor = try {
                                    Color(android.graphics.Color.parseColor(p.projectColor))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
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
                                                .background(pColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = p.projectName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            if (p.client.isNotBlank()) {
                                                Text(
                                                    text = p.client,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", p.hours)} hrs (${(p.percentage * 100).toInt()}%)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (p.earnings > 0) {
                                            Text(
                                                text = "$${String.format(Locale.US, "%.0f", p.earnings)}",
                                                fontSize = 11.sp,
                                                color = EmeraldGreen,
                                                fontWeight = FontWeight.SemiBold
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

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showExportDialog) {
        ExportDialog(
            projects = allProjects,
            projectSummaries = projectSummaries,
            timeEntries = filteredEntries,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun DailyHoursChart(
    dailyStats: List<DailyWorkStat>,
    modifier: Modifier = Modifier
) {
    val maxHours = (dailyStats.maxOfOrNull { it.totalHours } ?: 8.0).coerceAtLeast(4.0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val billableColor = EmeraldGreen
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = dailyStats.size
            if (barCount == 0) return@Canvas

            val spacing = 16.dp.toPx()
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(14.dp.toPx())

            // Baseline
            drawLine(
                color = gridColor,
                start = Offset(0f, canvasHeight - 10f),
                end = Offset(canvasWidth, canvasHeight - 10f),
                strokeWidth = 2f
            )

            // Draw bars
            dailyStats.forEachIndexed { index, stat ->
                val x = spacing + index * (barWidth + spacing)
                val totalH = ((stat.totalHours / maxHours) * (canvasHeight - 20f)).toFloat()
                val billableH = ((stat.billableHours / maxHours) * (canvasHeight - 20f)).toFloat()

                val yTotal = canvasHeight - 10f - totalH
                val yBillable = canvasHeight - 10f - billableH

                // Total bar (background)
                if (totalH > 0) {
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.35f),
                        topLeft = Offset(x, yTotal),
                        size = Size(barWidth, totalH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                // Billable bar (solid accent)
                if (billableH > 0) {
                    drawRoundRect(
                        color = billableColor,
                        topLeft = Offset(x, yBillable),
                        size = Size(barWidth, billableH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        // Labels underneath bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyStats.forEach { stat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stat.dayLabel,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (stat.totalHours > 0) "${String.format(Locale.US, "%.1f", stat.totalHours)}h" else "-",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stat.totalHours > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
