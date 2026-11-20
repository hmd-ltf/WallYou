package com.bnyro.wallpaper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bnyro.wallpaper.R
import net.youapps.wallpaper_apis.WallpaperApi

private const val EXCLUDE_PREFIX = "-"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsEditor(
    api: WallpaperApi,
    onModify: () -> Unit
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var tags by remember {
        // older versions allowed adding the same tag multiple times
        mutableStateOf(
            api.selectedTags.distinct()
        )
    }

    fun onChange() {
        api.selectedTags = tags
        onModify.invoke()
    }

    /**
     * Add the given tags, replacing previous occurrences of them (e.g. if a tag was previously excluded).
     */
    fun addTags(newTags: List<String>) {
        val newTagNames = newTags.map { it.removePrefix(EXCLUDE_PREFIX) }
        tags = tags.filter { it.removePrefix(EXCLUDE_PREFIX) !in newTagNames } + newTags.distinct()
        onChange()
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "add") {
            ElevatedAssistChip(
                onClick = {
                    showDialog = true
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(
                            modifier = Modifier.width(2.dp)
                        )
                        Text(stringResource(R.string.add))
                    }
                }
            )
        }

        items(tags, key = { "tag_$it" }) {
            val isExcluded = api.supportsTagExclusion && it.startsWith(EXCLUDE_PREFIX)

            ElevatedAssistChip(
                modifier = Modifier.animateItem(),
                onClick = {
                    tags = tags - it
                    onChange()
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Remove, null)
                        Spacer(
                            modifier = Modifier.width(2.dp)
                        )
                        Text(
                            text = it.removePrefix(EXCLUDE_PREFIX),
                            textDecoration = if (isExcluded) TextDecoration.LineThrough else null
                        )
                    }
                },
                trailingIcon = if (isExcluded) {
                    {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = stringResource(R.string.exclude),
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                }
            )
        }
    }

    val tagNames = tags.map { it.removePrefix(EXCLUDE_PREFIX) }
    val suggestions = api.suggestedTags.filter { it !in tagNames }
    if (suggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.popular_tags),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(suggestions, key = { it }) { suggestion ->
                SuggestionChip(
                    modifier = Modifier.animateItem(),
                    onClick = {
                        addTags(listOf(suggestion))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    label = {
                        Text(suggestion)
                    }
                )
            }
        }
    }

    if (showDialog) {
        var text by remember {
            mutableStateOf("")
        }
        var exclude by remember {
            mutableStateOf(false)
        }

        fun submit() {
            // multiple tags can be added at once by separating them with commas
            val newTags = text.split(",")
                .map { it.trim() }
                .map { if (api.supportsTagExclusion) it.removePrefix(EXCLUDE_PREFIX).trim() else it }
                .filter { it.isNotEmpty() }
                .distinct()
                .map { if (exclude) EXCLUDE_PREFIX + it else it }

            if (newTags.isNotEmpty()) addTags(newTags)
            showDialog = false
        }

        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = { submit() }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.add)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                        },
                        singleLine = true,
                        supportingText = {
                            Text(stringResource(R.string.tags_input_hint))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )

                    if (api.supportsTagExclusion) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !exclude,
                                onClick = { exclude = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                                    )
                                },
                                label = { Text(stringResource(R.string.include)) }
                            )
                            FilterChip(
                                selected = exclude,
                                onClick = { exclude = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                                    )
                                },
                                label = { Text(stringResource(R.string.exclude)) }
                            )
                        }
                    }
                }
            }
        )
    }
}
