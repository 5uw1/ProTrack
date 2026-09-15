package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.WorkCategory
import com.suw1labs.worktracker.ui.i18n.strings

/** Read-only exposed dropdown used for all pick-one fields in the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LabeledDropdown(
    label: String,
    selectedText: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .let { if (testTag != null) it.testTag(testTag) else it }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun Project.displayLabel(): String = "$code · $name"

/** Project picker that also offers "no project" (null). */
@Composable
fun ProjectDropdown(
    projects: List<Project>,
    selectedProjectId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = strings.sapProject,
    testTag: String? = null,
    onAddProject: (() -> Unit)? = null
) {
    val noProject = strings.noProjectOption
    val addLabel = strings.addProjectOption
    // A sentinel project marks the "add new project" entry at the end of the list.
    val addSentinel = remember { Project(id = Long.MIN_VALUE, code = "", name = "") }
    val options: List<Project?> = listOf<Project?>(null) + projects + (if (onAddProject != null) listOf(addSentinel) else emptyList())
    val selected = projects.find { it.id == selectedProjectId }
    LabeledDropdown(
        label = label,
        selectedText = selected?.displayLabel() ?: noProject,
        options = options,
        optionText = { if (it === addSentinel) addLabel else it?.displayLabel() ?: noProject },
        onSelect = { if (it === addSentinel) onAddProject?.invoke() else onSelect(it?.id) },
        modifier = modifier,
        testTag = testTag
    )
}

@Composable
fun CategoryDropdown(
    categories: List<WorkCategory>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val selected = categories.find { it.id == selectedCategoryId }
    val suffix = strings.unproductiveSuffix
    LabeledDropdown(
        label = strings.category,
        selectedText = selected?.let { categoryLabel(it, suffix) } ?: strings.selectCategory,
        options = categories,
        optionText = { categoryLabel(it, suffix) },
        onSelect = { onSelect(it.id) },
        modifier = modifier,
        testTag = testTag
    )
}

fun categoryLabel(category: WorkCategory, unproductiveSuffix: String): String =
    if (category.isProductive) category.name else "${category.name} $unproductiveSuffix"
