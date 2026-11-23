package com.bnyro.wallpaper.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.ui.components.DialogButton

/**
 * Edit the tags of a favorite wallpaper.
 *
 * @param initialTags the tags the wallpaper currently has
 * @param suggestions tags used by other favorites, offered for quick selection
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteTagsDialog(
    initialTags: List<String>,
    suggestions: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val tags = remember { initialTags.toMutableStateList() }
    var text by remember { mutableStateOf("") }

    fun addTagsFromText() {
        // multiple tags can be added at once by separating them with commas
        text.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { newTag ->
                if (tags.none { it.equals(newTag, ignoreCase = true) }) tags.add(newTag)
            }
        text = ""
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.tags)) },
        confirmButton = {
            DialogButton(stringResource(android.R.string.ok)) {
                addTagsFromText()
                onSave(tags.toList())
                onDismissRequest()
            }
        },
        dismissButton = {
            DialogButton(stringResource(android.R.string.cancel)) {
                onDismissRequest()
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.add_tag)) },
                    supportingText = { Text(stringResource(R.string.tags_input_hint)) },
                    trailingIcon = {
                        IconButton(onClick = { addTagsFromText() }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTagsFromText() })
                )

                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { tags.remove(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove),
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }

                val unusedSuggestions = suggestions.filter { suggestion ->
                    tags.none { it.equals(suggestion, ignoreCase = true) }
                }
                if (unusedSuggestions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.your_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        unusedSuggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { tags.add(suggestion) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                                    )
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }
            }
        }
    )
}
