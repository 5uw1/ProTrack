package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suw1labs.worktracker.ui.components.TrackModal
import com.suw1labs.worktracker.ui.i18n.strings

/** One dependency that ships inside the app, with whose copyright and under which licence. */
data class OpenSourceLibrary(
    val name: String,
    val copyright: String,
    val license: String,
)

/**
 * What is compiled into the released app. Test-only dependencies (JUnit, Robolectric, Roborazzi)
 * are not here: they never reach a user's device.
 */
val OpenSourceLibraries: List<OpenSourceLibrary> = listOf(
    OpenSourceLibrary("Kotlin, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization", "Copyright © JetBrains s.r.o. and Kotlin Programming Language contributors", "Apache License 2.0"),
    OpenSourceLibrary("Compose Multiplatform (runtime, foundation, UI, Material 3)", "Copyright © JetBrains s.r.o.", "Apache License 2.0"),
    OpenSourceLibrary("Jetpack Compose, Material Icons", "Copyright © The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Room and SQLite", "Copyright © The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Lifecycle, Activity, Core-KTX", "Copyright © The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLibrary("Jetpack Glance (Android widget)", "Copyright © The Android Open Source Project", "Apache License 2.0"),
)

/** Attribution as the Apache licence asks for it: who wrote what, and where the terms are. */
@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    val t = strings
    TrackModal()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp).testTag("licenses_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.licensesTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_licenses")) {
                        Icon(Icons.Default.Close, contentDescription = t.close, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(t.licensesIntro, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(OpenSourceLibraries) { library ->
                        Column {
                            Text(library.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(library.copyright, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(library.license, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(t.licensesApacheNotice, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "http://www.apache.org/licenses/LICENSE-2.0",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}
