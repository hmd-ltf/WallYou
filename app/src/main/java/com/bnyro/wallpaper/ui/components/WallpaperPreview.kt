package com.bnyro.wallpaper.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.db.obj.Wallpaper
import com.bnyro.wallpaper.enums.ResizeMethod
import com.bnyro.wallpaper.util.ImageHelper
import com.bnyro.wallpaper.util.Preferences
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

private enum class PreviewMode(val titleRes: Int) {
    HOME(R.string.home),
    LOCK(R.string.lockscreen),
    PLAIN(R.string.wallpaper_only)
}

private val overlayTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.5f),
    offset = Offset(0f, 2f),
    blurRadius = 8f
)

/**
 * Shows how the [wallpaper] will look like when it's applied to the whole screen,
 * optionally with a mock-up of the home or lock screen on top, without actually applying it.
 */
@Composable
fun WallpaperPreview(
    wallpaper: Wallpaper,
    onApply: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(PreviewMode.HOME) }
    var showControls by remember { mutableStateOf(true) }

    // mirror how the wallpaper gets resized when applying it, see [WallpaperHelper]
    val contentScale = remember {
        val resizeMethod = Preferences.getString(Preferences.resizeMethodKey, ResizeMethod.ZOOM.name)
        // the system always scales the wallpaper to fill the screen, except when it's stretched to the exact size
        if (resizeMethod == ResizeMethod.CROP.name) ContentScale.FillBounds else ContentScale.Crop
    }

    FullscreenDialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures { showControls = !showControls }
                }
        ) {
            val lowRes = rememberAsyncImagePainter(
                model = ImageHelper.buildRequest(context, wallpaper.preview)
            )
            AsyncImage(
                model = ImageHelper.buildRequest(context, wallpaper.fullImage),
                contentDescription = stringResource(R.string.wallpaper_preview),
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                placeholder = lowRes,
                error = lowRes
            )

            Crossfade(targetState = mode, label = "previewMode") { currentMode ->
                when (currentMode) {
                    PreviewMode.HOME -> HomeScreenMockup()
                    PreviewMode.LOCK -> LockScreenMockup()
                    PreviewMode.PLAIN -> Box(modifier = Modifier.fillMaxSize())
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilledTonalIconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.go_back)
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SingleChoiceSegmentedButtonRow {
                        PreviewMode.entries.forEachIndexed { index, entry ->
                            SegmentedButton(
                                selected = mode == entry,
                                onClick = { mode = entry },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PreviewMode.entries.size
                                ),
                                label = { Text(stringResource(entry.titleRes), maxLines = 1) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onApply) {
                        Icon(
                            imageVector = Icons.Rounded.Wallpaper,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.apply))
                    }
                }
            }
        }
    }
}

/**
 * The current time, updated every few seconds.
 */
@Composable
private fun rememberCurrentTime(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
private fun LockScreenMockup() {
    val context = LocalContext.current
    val now = rememberCurrentTime()
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now) {
        DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"), now)
            .toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    shadow = overlayTextShadow
                )
            )
            Text(
                text = time,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Light,
                    shadow = overlayTextShadow
                )
            )
        }

        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(28.dp)
        )
    }
}

@Composable
private fun HomeScreenMockup() {
    val now = rememberCurrentTime()
    val date = remember(now) {
        DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMM"), now)
            .toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = date,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 40.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                shadow = overlayTextShadow
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // a row of placeholder app icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) { AppIconPlaceholder() }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // a placeholder search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.DarkGray
                )
            }
        }
    }
}

@Composable
private fun AppIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color.White.copy(alpha = 0.8f), CircleShape)
    )
}
