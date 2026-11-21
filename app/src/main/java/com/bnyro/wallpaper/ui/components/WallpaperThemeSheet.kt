package com.bnyro.wallpaper.ui.components

import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.util.ClipboardHelper
import com.bnyro.wallpaper.util.MaterialThemeGenerator
import com.materialkolor.dynamiccolor.DynamicScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Android extracts wallpaper colors from a downscaled image of about this many pixels.
 */
private const val COLOR_EXTRACTION_AREA = 112 * 112

/**
 * Bottom sheet showing the Material You theme that Android would generate from the wallpaper [bitmap].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperThemeSheet(
    bitmap: Bitmap?,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var seedColors by remember { mutableStateOf<List<Int>?>(null) }
    var selectedSeedIndex by remember { mutableIntStateOf(0) }
    val systemDark = isSystemInDarkTheme()
    var isDark by remember { mutableStateOf(systemDark) }
    var style by remember { mutableStateOf(MaterialThemeGenerator.Style.TONAL_SPOT) }

    LaunchedEffect(bitmap) {
        if (bitmap == null) return@LaunchedEffect

        seedColors = withContext(Dispatchers.Default) {
            MaterialThemeGenerator.extractSeedColors(bitmap.toSamplePixels())
        }
        selectedSeedIndex = selectedSeedIndex.coerceIn(0, ((seedColors?.size ?: 1) - 1).coerceAtLeast(0))
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.material_theme),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.material_theme_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val seeds = seedColors
            if (seeds.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val seed = seeds[selectedSeedIndex.coerceIn(0, seeds.size - 1)]
            val dynamicScheme = remember(seed, isDark, style) {
                MaterialThemeGenerator.createScheme(seed, isDark, style)
            }
            val colorScheme = remember(dynamicScheme) { dynamicScheme.toColorScheme() }

            SectionLabel(stringResource(R.string.seed_color))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                seeds.forEachIndexed { index, color ->
                    SeedColorSwatch(
                        color = Color(color),
                        selected = index == selectedSeedIndex,
                        ringColor = MaterialTheme.colorScheme.primary,
                        onClick = { selectedSeedIndex = index }
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { index, dark ->
                    SegmentedButton(
                        selected = isDark == dark,
                        onClick = { isDark = dark },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        icon = {
                            Icon(
                                imageVector = if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        },
                        label = {
                            Text(stringResource(if (dark) R.string.theme_dark else R.string.theme_light))
                        }
                    )
                }
            }

            SectionLabel(stringResource(R.string.style))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaterialThemeGenerator.Style.entries.forEach { entry ->
                    FilterChip(
                        selected = style == entry,
                        onClick = { style = entry },
                        label = { Text(stringResource(entry.titleRes)) }
                    )
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                ThemeSample()
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel(stringResource(R.string.color_roles))
                Text(
                    text = stringResource(R.string.tap_to_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ColorRoles(colorScheme)
        }
    }
}

private val MaterialThemeGenerator.Style.titleRes: Int
    get() = when (this) {
        MaterialThemeGenerator.Style.TONAL_SPOT -> R.string.style_tonal_spot
        MaterialThemeGenerator.Style.VIBRANT -> R.string.style_vibrant
        MaterialThemeGenerator.Style.EXPRESSIVE -> R.string.style_expressive
        MaterialThemeGenerator.Style.NEUTRAL -> R.string.style_neutral
    }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SeedColorSwatch(
    color: Color,
    selected: Boolean,
    ringColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) ringColor else Color.Transparent,
                shape = CircleShape
            )
            .padding(5.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = if (color.toArgb().luminance() > 0.5f) Color.Black else Color.White
            )
        }
    }
}

/**
 * A small mock app drawn with the current [MaterialTheme], to see the generated colors in use.
 */
@Composable
private fun ThemeSample() {
    val colors = MaterialTheme.colorScheme
    var switchChecked by remember { mutableStateOf(true) }
    var chipSelected by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(0.6f) }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerHigh, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.theme_preview_title),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Surface(
                color = colors.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.theme_preview_body),
                    color = colors.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text(stringResource(R.string.sample_button)) }
                FilledTonalButton(onClick = {}) { Text(stringResource(R.string.sample_tonal)) }
                OutlinedButton(onClick = {}) { Text(stringResource(R.string.outline)) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = chipSelected,
                    onClick = { chipSelected = !chipSelected },
                    label = { Text(stringResource(R.string.sample_chip)) },
                    leadingIcon = if (chipSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    }
                )
                Switch(checked = switchChecked, onCheckedChange = { switchChecked = it })
                Spacer(modifier = Modifier.weight(1f))
                SmallFloatingActionButton(
                    onClick = {},
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }

            Slider(value = sliderValue, onValueChange = { sliderValue = it })
        }
    }
}

@Composable
private fun ColorRoles(colors: ColorScheme) {
    val context = LocalContext.current
    val roles = listOf(
        Triple(R.string.primary, colors.primary, colors.onPrimary),
        Triple(R.string.on_primary, colors.onPrimary, colors.primary),
        Triple(R.string.primary_container, colors.primaryContainer, colors.onPrimaryContainer),
        Triple(R.string.secondary, colors.secondary, colors.onSecondary),
        Triple(R.string.secondary_container, colors.secondaryContainer, colors.onSecondaryContainer),
        Triple(R.string.tertiary, colors.tertiary, colors.onTertiary),
        Triple(R.string.tertiary_container, colors.tertiaryContainer, colors.onTertiaryContainer),
        Triple(R.string.surface, colors.surface, colors.onSurface),
        Triple(R.string.surface_container, colors.surfaceContainer, colors.onSurface),
        Triple(R.string.outline, colors.outline, colors.surface),
        Triple(R.string.error_color, colors.error, colors.onError)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        roles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (nameRes, color, onColor) ->
                    val hex = color.toHex()
                    val copiedMessage = stringResource(R.string.copied, hex)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(color, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                ClipboardHelper(context).write(hex)
                                // Android 13+ shows its own confirmation when copying to the clipboard
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(nameRes),
                            color = onColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = hex,
                            color = onColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun Color.toHex() = "#%06X".format(toArgb() and 0xFFFFFF)

private fun Int.luminance(): Float {
    val r = (this shr 16 and 0xFF) / 255f
    val g = (this shr 8 and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/**
 * Downscale the bitmap like Android does before extracting the wallpaper colors and return its pixels.
 */
private fun Bitmap.toSamplePixels(): IntArray {
    val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this
    }

    val scale = sqrt(COLOR_EXTRACTION_AREA.toFloat() / (source.width * source.height)).coerceAtMost(1f)
    val width = max(1, (source.width * scale).roundToInt())
    val height = max(1, (source.height * scale).roundToInt())
    val scaled = Bitmap.createScaledBitmap(source, width, height, true)

    val pixels = IntArray(width * height)
    scaled.getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}

/**
 * Convert the generated [DynamicScheme] to a Compose [ColorScheme].
 */
private fun DynamicScheme.toColorScheme(): ColorScheme {
    return if (isDark) toDarkColorScheme(this) else toLightColorScheme(this)
}

private fun toDarkColorScheme(scheme: DynamicScheme) = darkColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        inversePrimary = Color(scheme.inversePrimary),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceTint = Color(scheme.surfaceTint),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        surfaceBright = Color(scheme.surfaceBright),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        surfaceDim = Color(scheme.surfaceDim)
)

private fun toLightColorScheme(scheme: DynamicScheme) = lightColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        inversePrimary = Color(scheme.inversePrimary),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceTint = Color(scheme.surfaceTint),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        surfaceBright = Color(scheme.surfaceBright),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        surfaceDim = Color(scheme.surfaceDim)
)
