package com.bnyro.wallpaper.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.NetworkType
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.db.DatabaseHolder
import com.bnyro.wallpaper.enums.WallpaperSource
import com.bnyro.wallpaper.enums.WallpaperTarget
import com.bnyro.wallpaper.ext.formatTime
import com.bnyro.wallpaper.ext.toast
import com.bnyro.wallpaper.obj.WallpaperConfig
import com.bnyro.wallpaper.ui.components.dialogs.TimePickerDialog
import com.bnyro.wallpaper.ui.components.prefs.CheckboxPref
import com.bnyro.wallpaper.ui.components.prefs.ListPreference
import com.bnyro.wallpaper.ui.components.prefs.MultiSelectionBlockPreference
import com.bnyro.wallpaper.ui.components.prefs.SettingsCategory
import com.bnyro.wallpaper.ui.nav.DrawerScreens
import com.bnyro.wallpaper.util.FavoritesHelper
import com.bnyro.wallpaper.util.PickFolderContract
import com.bnyro.wallpaper.util.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val networkTypes = listOf(
    R.string.all_networks to NetworkType.CONNECTED,
    R.string.unmetered to NetworkType.UNMETERED,
    R.string.metered to NetworkType.METERED,
    R.string.not_roaming to NetworkType.NOT_ROAMING,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperChangerPrefDialog(
    config: WallpaperConfig,
    onConfigChange: (WallpaperConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    var wallpaperSource by remember { mutableStateOf(config.source) }
    var wallpaperTarget by remember { mutableStateOf(config.target) }
    var changeInterval by remember { mutableIntStateOf(config.changeIntervalMinutes) }
    var networkType by remember { mutableStateOf(config.networkType) }
    // one entry per source, so that every option maps to an existing [WallpaperSource]
    val wallpaperSources = WallpaperSource.entries.map { source ->
        when (source) {
            WallpaperSource.ONLINE -> R.string.online
            WallpaperSource.FAVORITES -> R.string.favorites
            WallpaperSource.LOCAL -> R.string.local
        }
    }
    var wallpaperEnginesIndices = remember {
        config.selectedApiRoutes.map { route ->
            DrawerScreens.apiScreens.indexOfFirst { it.route == route }
        }.filter { it >= 0 } // the source may have been removed in the meantime
    }
    val favoriteTags = remember {
        config.favoriteTags.toMutableStateList()
    }
    var allFavoriteTags by remember {
        mutableStateOf(config.favoriteTags)
    }
    LaunchedEffect(Unit) {
        allFavoriteTags = withContext(Dispatchers.IO) {
            val favorites = DatabaseHolder.Database.favoritesDao().getFavorites()
            // also show selected tags that are no longer used, so that they can be deselected
            (FavoritesHelper.getAllTags(favorites) + config.favoriteTags).distinct()
        }
    }
    var themeColor by remember {
        mutableStateOf(config.themeColor)
    }
    val localFolderUris = remember {
        config.localFolderUris.toMutableStateList()
    }
    var applyImageFilters by remember {
        mutableStateOf(config.applyImageFilters)
    }
    var startTimeMillis by remember {
        mutableStateOf(config.startTimeMillis)
    }
    var endTimeMillis by remember {
        mutableStateOf(config.endTimeMillis)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            DialogButton(stringResource(android.R.string.cancel)) {
                onDismissRequest()
            }
        },
        confirmButton = {
            DialogButton(stringResource(android.R.string.ok)) {
                if (changeInterval == 0) {
                    context.toast(context.getString(R.string.invalid_time_interval))
                    return@DialogButton
                }

                val hasStartAndEndTime = startTimeMillis != null && endTimeMillis != null
                if (hasStartAndEndTime && startTimeMillis!! == endTimeMillis!!) {
                    context.toast(context.getString(R.string.invalid_time_interval))
                    return@DialogButton
                }

                val newConfig = WallpaperConfig(
                    id = config.id,
                    changeIntervalMinutes = changeInterval,
                    networkType = networkType,
                    target = wallpaperTarget,
                    source = wallpaperSource,
                    selectedApiRoutes = wallpaperEnginesIndices.map { DrawerScreens.apiScreens[it].route },
                    localFolderUris = localFolderUris,
                    applyImageFilters = applyImageFilters,
                    startTimeMillis = if (hasStartAndEndTime) startTimeMillis else null,
                    endTimeMillis = if (hasStartAndEndTime) endTimeMillis else null,
                    favoriteTags = favoriteTags.toList(),
                    themeColor = themeColor,
                )
                onConfigChange(newConfig)
                onDismissRequest()
            }
        },
        title = {
            Text(stringResource(R.string.wallpaper_changer))
        },
        text = {
            val localWallpaperDirChooser = rememberLauncherForActivityResult(PickFolderContract()) {
                val uri = it ?: return@rememberLauncherForActivityResult

                config.localFolderUris += uri.toString()
                localFolderUris.add(uri.toString())
            }

            Column {
                MultiSelectionBlockPreference(
                    preferenceKey = null,
                    entries = listOf(R.string.home, R.string.lockscreen).map { stringResource(it) },
                    values = listOf(WallpaperTarget.HOME, WallpaperTarget.LOCK).map { it.name },
                    defaultSelections = when (wallpaperTarget) {
                        WallpaperTarget.BOTH -> listOf(0, 1)
                        WallpaperTarget.HOME -> listOf(0)
                        else -> listOf(1)
                    },
                    requireAtLeastOne = true
                ) { selections ->
                    wallpaperTarget = when {
                        selections.size == 2 -> WallpaperTarget.BOTH
                        selections[0] == 0 -> WallpaperTarget.HOME
                        else -> WallpaperTarget.LOCK
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(stringResource(R.string.change_interval))
                var intervalDays by remember {
                    mutableIntStateOf(changeInterval / 60 / 24)
                }
                var intervalHours by remember {
                    mutableIntStateOf(changeInterval / 60 % 24)
                }
                // Android doesn't run periodic background work more often than every 15 minutes,
                // so minutes can only be chosen in steps of that size
                var intervalMinutesIndex by remember {
                    mutableIntStateOf(
                        (changeInterval % 60 / MINUTE_STEP).coerceIn(MINUTE_STEPS.indices)
                    )
                }
                LaunchedEffect(intervalDays, intervalHours, intervalMinutesIndex) {
                    changeInterval = ((intervalDays * 24) + intervalHours) * 60 +
                        MINUTE_STEPS[intervalMinutesIndex]
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IntervalPicker(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.days),
                        items = (0..90).map { it.toString() },
                        startIndex = intervalDays
                    ) { index ->
                        intervalDays = index
                    }
                    IntervalPicker(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.hours),
                        items = (0..23).map { it.toString() },
                        startIndex = intervalHours
                    ) { index ->
                        intervalHours = index
                    }
                    IntervalPicker(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.minutes),
                        items = MINUTE_STEPS.map { it.toString() },
                        startIndex = intervalMinutesIndex
                    ) { index ->
                        intervalMinutesIndex = index
                    }
                }

                AnimatedVisibility(visible = wallpaperSource != WallpaperSource.LOCAL) {
                    ListPreference(
                        prefKey = null,
                        title = stringResource(R.string.network_type),
                        entries = networkTypes.map { stringResource(id = it.first) },
                        values = networkTypes.map { it.second.name },
                        defaultValue = networkType.name
                    ) {
                        networkType = NetworkType.valueOf(it)
                    }
                }

                ListPreference(
                    prefKey = null,
                    title = stringResource(R.string.wallpaper_changer_source),
                    entries = wallpaperSources.map { stringResource(it) },
                    values = List(wallpaperSources.size) { index -> index.toString() },
                    defaultValue = wallpaperSource.ordinal.toString()
                ) { newValue ->
                    wallpaperSource = WallpaperSource.entries[newValue.toInt()]
                }

                Crossfade(targetState = wallpaperSource, label = "wallpaper_source") { state ->
                    when (state) {
                        WallpaperSource.ONLINE -> Column {
                            MultiSelectionBlockPreference(
                                preferenceKey = null,
                                entries = DrawerScreens.apiScreens.map { it.title.str() },
                                values = DrawerScreens.apiScreens.map { it.route },
                                defaultSelections = wallpaperEnginesIndices
                            ) { selections ->
                                wallpaperEnginesIndices = selections
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        WallpaperSource.LOCAL -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SettingsCategory(title = stringResource(R.string.directories))

                            localFolderUris.forEach {
                                var selectedDirectoryName by remember {
                                    mutableStateOf("")
                                }

                                LaunchedEffect(it) {
                                    DocumentFile.fromTreeUri(context, it.toUri())?.let { file ->
                                        selectedDirectoryName = file.name ?: it
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))
                                Row(
                                    modifier = Modifier.padding(start = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedDirectoryName)
                                    Spacer(modifier = Modifier.weight(1f))
                                    ButtonWithIcon(icon = Icons.Default.Delete) {
                                        localFolderUris.remove(it)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    localWallpaperDirChooser.launch(null)
                                }
                            ) {
                                Text(stringResource(R.string.choose_dir))
                            }
                        }

                        WallpaperSource.FAVORITES -> Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SettingsCategory(title = stringResource(R.string.favorite_tags_filter))
                            Text(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                text = stringResource(
                                    if (allFavoriteTags.isEmpty()) R.string.favorite_tags_filter_empty
                                    else R.string.favorite_tags_filter_desc
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allFavoriteTags.forEach { tag ->
                                    FilterChip(
                                        selected = tag in favoriteTags,
                                        onClick = {
                                            if (tag in favoriteTags) favoriteTags.remove(tag)
                                            else favoriteTags.add(tag)
                                        },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }

                        else -> Unit
                    }
                }

                SettingsCategory(title = stringResource(R.string.theme_color_filter))
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_color_filter_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ThemeColorPicker(
                        selectedColor = themeColor,
                        onColorSelected = { themeColor = it }
                    )
                }

                CheckboxPref(
                    prefKey = null,
                    title = stringResource(R.string.apply_image_filters),
                    summary = stringResource(R.string.apply_image_filters_desc),
                    defaultValue = applyImageFilters
                ) { newValue ->
                    applyImageFilters = newValue
                }

                var customTimeInterval by remember {
                    mutableStateOf(endTimeMillis != null && startTimeMillis != null)
                }
                CheckboxPref(
                    prefKey = null,
                    title = stringResource(R.string.time_interval),
                    summary = stringResource(R.string.time_interval_desc),
                    defaultValue = customTimeInterval
                ) { newValue ->
                    customTimeInterval = newValue

                    if (!customTimeInterval) {
                        startTimeMillis = null
                        endTimeMillis = null
                    } else {
                        startTimeMillis = 0
                        endTimeMillis = 0
                    }
                }
                AnimatedVisibility(visible = customTimeInterval) {
                    var showStartTimePicker by remember {
                        mutableStateOf(false)
                    }

                    var showEndTimePicker by remember {
                        mutableStateOf(false)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTimeFilled,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { showStartTimePicker = true }
                        ) {
                            Text((startTimeMillis ?: 0).formatTime())
                        }

                        Icon(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            imageVector = Icons.AutoMirrored.Default.ArrowForward,
                            contentDescription = null
                        )

                        Button(
                            onClick = { showEndTimePicker = true }
                        ) {
                            Text((endTimeMillis ?: 0).formatTime())
                        }
                    }

                    if (showStartTimePicker) {
                        TimePickerDialog(
                            startTimeMillis ?: 0,
                            onTimeChange = { startTimeMillis = it }
                        ) {
                            showStartTimePicker = false
                        }
                    }

                    if (showEndTimePicker) {
                        TimePickerDialog(
                            endTimeMillis ?: 0,
                            onTimeChange = { endTimeMillis = it }
                        ) {
                            showEndTimePicker = false
                        }
                    }
                }
            }
        }
    )
}

private const val MINUTE_STEP = 15
private val MINUTE_STEPS = (0 until 60 step MINUTE_STEP).toList()

@Composable
private fun IntervalPicker(
    modifier: Modifier,
    label: String,
    items: List<String>,
    startIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NumberPicker(
            items = items,
            startIndex = startIndex,
            onItemSelected = onItemSelected
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
