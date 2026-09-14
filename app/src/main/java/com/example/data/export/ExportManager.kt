package com.example.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.ProjectSummary
import com.example.data.model.TimeEntryWithDetails
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatDurationHms(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Standard RFC 4180 CSV export for time entries.
     */
    fun generateCsv(entries: List<TimeEntryWithDetails>): String {
        val sb = StringBuilder()
        // Header
        sb.append("ID,Date,Start Time,End Time,Duration (Hours),Duration (Formatted),Project,Client,Task,Billable,Hourly Rate,Total Earnings,Tags,Description\n")

        for (e in entries) {
            val dateStr = dateFormat.format(Date(e.startTime))
            val startStr = timeFormat.format(Date(e.startTime))
            val endStr = timeFormat.format(Date(e.endTime))
            val hours = String.format(Locale.US, "%.2f", e.durationSeconds / 3600.0)
            val hms = formatDurationHms(e.durationSeconds)
            val earnings = if (e.isBillable) {
                String.format(Locale.US, "%.2f", (e.durationSeconds / 3600.0) * e.hourlyRate)
            } else "0.00"

            sb.append(e.id).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(startStr)).append(",")
            sb.append(escapeCsv(endStr)).append(",")
            sb.append(hours).append(",")
            sb.append(escapeCsv(hms)).append(",")
            sb.append(escapeCsv(e.projectName)).append(",")
            sb.append(escapeCsv(e.client)).append(",")
            sb.append(escapeCsv(e.taskTitle ?: "General")).append(",")
            sb.append(if (e.isBillable) "Yes" else "No").append(",")
            sb.append(String.format(Locale.US, "%.2f", e.hourlyRate)).append(",")
            sb.append(earnings).append(",")
            sb.append(escapeCsv(e.tags)).append(",")
            sb.append(escapeCsv(e.description)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Excel-optimized CSV with UTF-8 BOM, executive summary, project metrics, and detailed logs.
     */
    fun generateExcelCsv(
        entries: List<TimeEntryWithDetails>,
        projects: List<ProjectSummary>
    ): String {
        val sb = StringBuilder()
        // Excel UTF-8 Byte Order Mark
        sb.append('\uFEFF')

        val now = System.currentTimeMillis()
        val totalSecs = entries.sumOf { it.durationSeconds }
        val billableSecs = entries.filter { it.isBillable }.sumOf { it.durationSeconds }
        val totalEarned = entries.filter { it.isBillable }.sumOf { (it.durationSeconds / 3600.0) * it.hourlyRate }

        sb.append("WORK TIME TRACKER - PRODUCTIVITY & BILLING REPORT\n")
        sb.append("Generated On:,").append(escapeCsv(dateTimeFormat.format(Date(now)))).append("\n")
        sb.append("Total Time Tracked:,").append(formatDurationHms(totalSecs)).append(" (").append(String.format(Locale.US, "%.2f", totalSecs / 3600.0)).append(" hrs)\n")
        sb.append("Billable Time:,").append(formatDurationHms(billableSecs)).append(" (").append(String.format(Locale.US, "%.2f", billableSecs / 3600.0)).append(" hrs)\n")
        sb.append("Total Billable Earnings:,$").append(String.format(Locale.US, "%.2f", totalEarned)).append("\n")
        sb.append("Total Entries:,").append(entries.size).append("\n\n")

        // Projects Summary Section
        sb.append("--- PROJECT MANAGEMENT SUMMARY ---\n")
        sb.append("Project Name,Client,Status,Hourly Rate,Budget (Hrs),Logged (Hrs),Billable (Hrs),Earnings ($),Completed Tasks,Total Tasks\n")
        for (p in projects) {
            val loggedHrs = String.format(Locale.US, "%.2f", p.totalSeconds / 3600.0)
            val billableHrs = String.format(Locale.US, "%.2f", p.billableSeconds / 3600.0)
            val earnings = String.format(Locale.US, "%.2f", p.totalEarnings)
            sb.append(escapeCsv(p.name)).append(",")
            sb.append(escapeCsv(p.client)).append(",")
            sb.append(escapeCsv(p.status)).append(",")
            sb.append("$").append(String.format(Locale.US, "%.2f", p.hourlyRate)).append(",")
            sb.append(p.budgetHours).append(",")
            sb.append(loggedHrs).append(",")
            sb.append(billableHrs).append(",")
            sb.append("$").append(earnings).append(",")
            sb.append(p.completedTasks).append(",")
            sb.append(p.totalTasks).append("\n")
        }

        sb.append("\n--- DETAILED TIME ENTRIES ---\n")
        sb.append("Date,Start,End,Hours,HH:MM:SS,Project,Client,Task,Billable,Hourly Rate,Amount,Tags,Notes\n")
        for (e in entries) {
            val dateStr = dateFormat.format(Date(e.startTime))
            val startStr = timeFormat.format(Date(e.startTime))
            val endStr = timeFormat.format(Date(e.endTime))
            val hours = String.format(Locale.US, "%.2f", e.durationSeconds / 3600.0)
            val hms = formatDurationHms(e.durationSeconds)
            val earnings = if (e.isBillable) {
                String.format(Locale.US, "%.2f", (e.durationSeconds / 3600.0) * e.hourlyRate)
            } else "0.00"

            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(startStr)).append(",")
            sb.append(escapeCsv(endStr)).append(",")
            sb.append(hours).append(",")
            sb.append(escapeCsv(hms)).append(",")
            sb.append(escapeCsv(e.projectName)).append(",")
            sb.append(escapeCsv(e.client)).append(",")
            sb.append(escapeCsv(e.taskTitle ?: "-")).append(",")
            sb.append(if (e.isBillable) "Yes" else "No").append(",")
            sb.append("$").append(String.format(Locale.US, "%.2f", e.hourlyRate)).append(",")
            sb.append("$").append(earnings).append(",")
            sb.append(escapeCsv(e.tags)).append(",")
            sb.append(escapeCsv(e.description)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes content to a file in the app's cache directory under 'exports/'.
     */
    fun saveExportFile(context: Context, content: String, filename: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }
        return file
    }

    /**
     * Launches Android Share Intent with FileProvider URI.
     */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }
}
