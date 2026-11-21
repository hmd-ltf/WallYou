package com.bnyro.wallpaper.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.ext.formatFilterName
import com.bnyro.wallpaper.ext.formatFilterOption
import com.bnyro.wallpaper.ext.parseFilterColorOrNull
import com.bnyro.wallpaper.ui.components.DialogButton
import com.bnyro.wallpaper.ui.components.TagsEditor
import com.bnyro.wallpaper.util.WallpaperApiWrapper
import net.youapps.wallpaper_apis.TextUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    api: WallpaperApiWrapper,
    onDismissRequest: (Boolean) -> Unit
) {
    var modified by remember {
        mutableStateOf(false)
    }

    var showCommunityNameDialog by remember {
        mutableStateOf(false)
    }

    var communityName by remember {
        mutableStateOf(api.communityName.orEmpty())
    }

    var selectedFilters by remember {
        mutableStateOf(api.selectedFilters)
    }

    AlertDialog(
        onDismissRequest = {
            onDismissRequest.invoke(modified)
        },
        confirmButton = {
            DialogButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    onDismissRequest.invoke(modified)
                }
            )
        },
        title = {
            Text(stringResource(R.string.filter))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (api.requiresCommunityName) {
                    Column {
                        Text(text = stringResource(R.string.community_name))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = communityName,
                                fontSize = 18.sp
                            )
                            IconButton(
                                onClick = { showCommunityNameDialog = true }
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                            }
                        }
                    }
                }

                if (api.supportsTags) {
                    TagsEditor(
                        api = api
                    ) {
                        modified = true
                    }
                    Spacer(
                        modifier = Modifier
                            .height(5.dp)
                    )
                }

                api.availableFilters.forEach { (key, options) ->
                    AnimatedVisibility(
                        visible = api.isFilterApplicable(key, selectedFilters)
                    ) {
                        FilterRow(
                            api = api,
                            filterKey = key,
                            options = options,
                            value = selectedFilters[key].orEmpty(),
                            onValueChange = { newValue ->
                                modified = true
                                selectedFilters = selectedFilters + (key to newValue)
                                api.setFilter(key, newValue)
                            }
                        )
                    }
                }

                if (api.supportsApiKey && api.apiKey == null) {
                    Row(
                        modifier = Modifier.padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.log_in_to_unlock_nsfw),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    )

    if (showCommunityNameDialog) {
        var newCommunityName by remember {
            mutableStateOf(communityName)
        }

        AlertDialog(
            onDismissRequest = { showCommunityNameDialog = false },
            confirmButton = {
                DialogButton(
                    text = stringResource(android.R.string.ok)
                ) {
                    api.communityName = newCommunityName
                    communityName = newCommunityName
                    modified = true
                    showCommunityNameDialog = false
                }
            },
            dismissButton = {
                DialogButton(text = stringResource(android.R.string.cancel)) {
                    newCommunityName = communityName
                    showCommunityNameDialog = false
                }
            },
            title = {
                Text(stringResource(R.string.community_name))
            },
            text = {
                OutlinedTextField(
                    value = newCommunityName,
                    onValueChange = { newCommunityName = it },
                    label = {
                        Text(stringResource(R.string.community_name))
                    },
                    placeholder = {
                        Text(api.communityName.orEmpty())
                    }
                )
            }
        )
    }
}

/**
 * A row of chips for a single search filter.
 *
 * Supports both single select filters and multi select filters (see [WallpaperApiWrapper.multiSelectFilters]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    api: WallpaperApiWrapper,
    filterKey: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit
) {
    val isMultiSelect = filterKey in api.multiSelectFilters
    val isOptional = filterKey in api.optionalFilters
    val selectedOptions = remember(value) {
        if (isMultiSelect) TextUtils.splitFilterValues(value) else listOf(value)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = filterKey.formatFilterName(),
                fontSize = 14.sp
            )

            if (isMultiSelect) {
                Text(
                    text = if (selectedOptions.isEmpty() && isOptional) {
                        stringResource(R.string.any)
                    } else {
                        pluralStringResource(
                            R.plurals.n_selected,
                            selectedOptions.size,
                            selectedOptions.size
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(
            modifier = Modifier
                .height(5.dp)
        )

        val rowState = rememberLazyListState()
        LaunchedEffect(Unit) {
            val selectedItem = options.indexOfFirst { entry -> entry in selectedOptions }
            if (selectedItem > 0) rowState.scrollToItem(selectedItem)
        }
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(options, key = { it }) { entry ->
                val isSelected = entry in selectedOptions
                val color = remember(entry) { entry.parseFilterColorOrNull()?.let { Color(it) } }

                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        if (!isMultiSelect) {
                            onValueChange(entry)
                            return@ElevatedFilterChip
                        }

                        val newSelection = if (isSelected) {
                            selectedOptions - entry
                        } else {
                            // keep the order of the available options
                            options.filter { it in selectedOptions || it == entry }
                        }

                        // required filters must keep at least one selected option
                        if (newSelection.isEmpty() && !isOptional) return@ElevatedFilterChip

                        onValueChange(TextUtils.joinFilterValues(newSelection))
                    },
                    leadingIcon = if (isMultiSelect && isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    label = {
                        if (color != null) {
                            ColorSwatch(color = color, isSelected = isSelected)
                        } else {
                            Text(entry.formatFilterOption())
                        }
                    }
                )
            }
        }
        Spacer(
            modifier = Modifier
                .height(5.dp)
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape
            )
            .padding(2.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                // pick a check mark color that's readable on top of the swatch
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White
            )
        }
    }
}
