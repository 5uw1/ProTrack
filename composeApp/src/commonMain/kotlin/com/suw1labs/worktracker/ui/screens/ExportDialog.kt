package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suw1labs.worktracker.data.export.ExportFormat
import com.suw1labs.worktracker.data.export.SapExportType
import com.suw1labs.worktracker.platform.ExportAction
import androidx.compose.material.icons.filled.Save
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.i18n.strings
import kotlinx.coroutines.delay

/**
 * Preview + share of the CSV that is booked into SAP at the end of the month.
 */
@Composable
fun SapExportDialog(
    periodLabel: String,
    buildContent: (type: SapExportType, roundToQuarter: Boolean, format: ExportFormat) -> String,
    onShare: (type: SapExportType, format: ExportFormat, content: String, action: ExportAction) -> Unit,
    onDismiss: () -> Unit
) {
    val t = strings
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var selectedType by remember { mutableStateOf(SapExportType.MONTHLY_SUMMARY) }
    var roundToQuarter by remember { mutableStateOf(false) }
    var format by remember { mutableStateOf(ExportFormat.EXCEL_CSV) }
    var copiedMessageVisible by remember { mutableStateOf(false) }

    val previewText = remember(selectedType, roundToQuarter, format) { buildContent(selectedType, roundToQuarter, format) }

    LaunchedEffect(copiedMessageVisible) {
        if (copiedMessageVisible) {
            delay(2000)
            copiedMessageVisible = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp).testTag("export_dialog_card").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(t.exportTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(periodLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = t.close) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(t.report, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SapExportType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(t.exportTypeLabel(type)) },
                            modifier = Modifier.testTag("export_type_${type.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(t.fileFormat, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ExportFormat.entries.forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(t.formatLabel(f)) },
                            modifier = Modifier.testTag("export_format_${f.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                ToggleRow(t.roundQuarter, roundToQuarter) { roundToQuarter = it }

                Spacer(modifier = Modifier.height(12.dp))
                Text(t.preview, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp)
                ) {
                    Box(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                        Text(
                            text = previewText.take(1500) + if (previewText.length > 1500) "\n${t.moreRows}" else "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            @Suppress("DEPRECATION")
                            clipboardManager.setText(AnnotatedString(previewText))
                            copiedMessageVisible = true
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("copy_export_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedMessageVisible) t.copied else t.copy, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onShare(selectedType, format, previewText, ExportAction.SAVE)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("save_export_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t.saveFile, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onShare(selectedType, format, previewText, ExportAction.SHARE)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_export_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t.shareFile, fontSize = 13.sp)
                }
                if (copiedMessageVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(t.copiedToClipboard, fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("copied_confirmation"))
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
