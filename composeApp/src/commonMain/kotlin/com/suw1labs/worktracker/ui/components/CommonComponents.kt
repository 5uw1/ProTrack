package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.ui.theme.AmberWarning
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.formatFixed
import com.suw1labs.worktracker.util.parseHexColor
import com.suw1labs.worktracker.ui.i18n.strings

@Composable
fun DeadlineUrgencyBadge(
    deadlineTimestamp: Long?,
    modifier: Modifier = Modifier
) {
    if (deadlineTimestamp == null) return
    val t = strings

    val now = currentTimeMillis()
    val diffMillis = deadlineTimestamp - now
    val diffHours = diffMillis / (1000.0 * 3600.0)

    val (bgColor, textColor, text, icon) = when {
        diffHours < 0 -> {
            val overdueHours = -diffHours
            val overdueStr = if (overdueHours >= 24) {
                t.overdueDays((overdueHours / 24).toInt())
            } else {
                t.overdueHours(overdueHours.toInt())
            }
            Tuple4(RoseUrgent.copy(alpha = 0.15f), RoseUrgent, overdueStr, Icons.Default.Error)
        }
        diffHours <= 2.0 -> {
            Tuple4(RoseUrgent.copy(alpha = 0.15f), RoseUrgent, t.dueUnder2h, Icons.Default.Warning)
        }
        diffHours <= 24.0 -> {
            Tuple4(AmberWarning.copy(alpha = 0.15f), AmberWarning, t.dueInHours(diffHours.toInt()), Icons.Default.Alarm)
        }
        diffHours <= 48.0 -> {
            Tuple4(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, t.dueTomorrow, Icons.Default.Alarm)
        }
        else -> {
            val dateStr = DateFormats.monthDay(deadlineTimestamp)
            Tuple4(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, t.dueOn(dateStr), Icons.Default.Alarm)
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val label = strings.priorityName(priority)
    val color = when (priority.uppercase()) {
        "URGENT" -> RoseUrgent
        "HIGH" -> AmberWarning
        "MEDIUM" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val label = if (status.uppercase() in setOf("ACTIVE", "ON_HOLD", "COMPLETED")) strings.projectStatus(status.uppercase()) else strings.taskStatus(status)
    val color = when (status.uppercase()) {
        "DONE", "COMPLETED" -> EmeraldGreen
        "IN_PROGRESS", "ACTIVE" -> MaterialTheme.colorScheme.primary
        "REVIEW", "ON_HOLD" -> AmberWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            if (status == "DONE" || status == "COMPLETED") {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HoursProgressBar(
    loggedHours: Double,
    budgetHours: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val progress = if (budgetHours > 0) {
            (loggedHours / budgetHours).toFloat().coerceIn(0f, 1f)
        } else 0f

        val isOverBudget = budgetHours > 0 && loggedHours > budgetHours

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.hrsLogged(loggedHours.formatFixed(1)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOverBudget) RoseUrgent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (budgetHours > 0) {
                Text(
                    text = strings.budget(budgetHours.formatFixed(0)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = if (isOverBudget) RoseUrgent else accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun ColorPaletteSelector(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val palette = listOf(
        "#3B82F6", // Blue
        "#10B981", // Emerald
        "#8B5CF6", // Purple
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#06B6D4", // Cyan
        "#EF4444", // Red
        "#64748B"  // Slate
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        palette.forEach { hex ->
            val color = parseHexColor(hex) ?: MaterialTheme.colorScheme.primary
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(hex) }
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .testTag("color_option_$hex")
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data holder helper
private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
