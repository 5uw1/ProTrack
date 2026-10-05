package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suw1labs.worktracker.platform.AppUpdater
import com.suw1labs.worktracker.platform.UpdateState
import com.suw1labs.worktracker.ui.i18n.strings

/** Settings card: the running version, whether a newer one is out, and the button that installs it. */
@Composable
fun AppUpdateCard(updater: AppUpdater, modifier: Modifier = Modifier) {
    val t = strings
    val state by updater.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = modifier.fillMaxWidth().testTag("app_update_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.updatesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        t.updateVersion(updater.currentVersion ?: "–") + when (val s = state) {
                            is UpdateState.UpToDate -> " · ${t.updateUpToDate}"
                            is UpdateState.Checking -> " · ${t.updateChecking}"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            when (val s = state) {
                is UpdateState.Available -> {
                    Text(t.updateAvailable(s.release.version), fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { updater.install() }, modifier = Modifier.testTag("app_update_install")) { Text(t.updateInstall) }
                        if (s.release.pageUrl.isNotBlank()) TextButton(onClick = { uriHandler.openUri(s.release.pageUrl) }) { Text(t.updateWhatsNew) }
                    }
                }
                is UpdateState.Downloading -> {
                    Text(t.updateDownloading((s.progress * 100).toInt()), color = colors.onSurfaceVariant)
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                }
                is UpdateState.Installing -> Text(t.updateInstalling, color = colors.primary)
                is UpdateState.Manual -> Text(t.updateManual, color = colors.onSurfaceVariant)
                is UpdateState.Failed -> {
                    Text(t.updateFailed(s.reason), color = colors.error)
                    OutlinedButton(onClick = { if (s.release != null) updater.install() else updater.check() }) {
                        Text(if (s.release != null) t.updateInstall else t.updateCheck)
                    }
                }
                UpdateState.Idle, is UpdateState.UpToDate -> OutlinedButton(onClick = { updater.check() }, modifier = Modifier.testTag("app_update_check")) { Text(t.updateCheck) }
                UpdateState.Checking -> Unit
            }
        }
    }
}
