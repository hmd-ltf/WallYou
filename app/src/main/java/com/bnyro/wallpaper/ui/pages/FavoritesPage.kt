package com.bnyro.wallpaper.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.ui.components.NothingHere
import com.bnyro.wallpaper.ui.components.WallpaperGrid
import com.bnyro.wallpaper.ui.components.WallpaperPageView
import com.bnyro.wallpaper.ui.models.MainModel
import com.bnyro.wallpaper.util.FavoritesHelper

@Composable
fun FavoritesPage(viewModel: MainModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val favorites by viewModel.favWallpapers.collectAsState()

        var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
        var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }

        val allTags = remember(favorites) { FavoritesHelper.getAllTags(favorites) }
        // the tag may have been removed from all favorites in the meantime
        val activeTag = selectedTag?.takeIf { it in allTags }
        val shownFavorites = remember(favorites, activeTag) {
            if (activeTag == null) favorites else favorites.filter { activeTag in it.tags }
        }

        if (allTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = activeTag == null,
                        onClick = { selectedTag = null },
                        label = { Text(stringResource(R.string.all)) }
                    )
                }
                items(allTags, key = { "tag_$it" }) { tag ->
                    FilterChip(
                        selected = activeTag == tag,
                        onClick = { selectedTag = if (activeTag == tag) null else tag },
                        label = { Text(tag) }
                    )
                }
            }
        }

        if (shownFavorites.isNotEmpty()) {
            WallpaperGrid(
                viewModel = viewModel,
                wallpapers = shownFavorites,
                onClickWallpaper = {
                    selectedIndex = it
                }
            )
            selectedIndex?.takeIf { it < shownFavorites.size }?.let {
                WallpaperPageView(initialPage = it, wallpapers = shownFavorites) {
                    selectedIndex = null
                }
            }
        } else {
            NothingHere(
                text = stringResource(R.string.no_favorites),
                icon = Icons.Default.HeartBroken
            )
        }
    }
}
