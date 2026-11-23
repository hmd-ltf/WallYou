package com.bnyro.wallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.util.ThemeColorHelper

/**
 * Choose the color of the Material You theme wallpapers should generate, or any theme (null).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeColorPicker(
    selectedColor: Int?,
    onColorSelected: (Int?) -> Unit
) {
    val presets = ThemeColorHelper.presetColors
    // a color picked from a wallpaper's theme, which isn't one of the presets
    val customColor = selectedColor?.takeIf { color -> presets.none { it.first == color } }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorSwatch(
            color = null,
            selected = selectedColor == null,
            description = stringResource(R.string.any_theme)
        ) { onColorSelected(null) }

        presets.forEach { (color, nameRes) ->
            ColorSwatch(
                color = Color(color),
                selected = selectedColor == color,
                description = stringResource(nameRes)
            ) { onColorSelected(color) }
        }

        if (customColor != null) {
            ColorSwatch(
                color = Color(customColor),
                selected = true,
                description = stringResource(R.string.wallpaper_theme_color)
            ) { onColorSelected(customColor) }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color?,
    selected: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val ringColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(40.dp)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) ringColor else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(color ?: MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            selected -> Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = when {
                    color == null -> MaterialTheme.colorScheme.onSurface
                    color.luminance() > 0.5f -> Color.Black
                    else -> Color.White
                }
            )

            color == null -> Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
