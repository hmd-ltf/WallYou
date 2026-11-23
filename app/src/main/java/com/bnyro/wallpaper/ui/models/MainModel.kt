package com.bnyro.wallpaper.ui.models

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnyro.wallpaper.App
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.db.DatabaseHolder
import com.bnyro.wallpaper.db.DatabaseHolder.Database
import com.bnyro.wallpaper.db.obj.Wallpaper
import com.bnyro.wallpaper.enums.ThemeMode
import com.bnyro.wallpaper.ui.nav.DrawerScreens
import com.bnyro.wallpaper.util.Either
import com.bnyro.wallpaper.util.FavoritesHelper
import com.bnyro.wallpaper.util.OfflineWallpaperHelper
import com.bnyro.wallpaper.util.Preferences
import com.bnyro.wallpaper.util.WallpaperApiWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainModel : ViewModel() {
    private val themeModeIndex = Preferences.getString(
        Preferences.themeModeKey,
        ThemeMode.AUTO.value.toString()
    ).toInt()
    var themeMode by mutableStateOf(ThemeMode.entries[themeModeIndex])

    var currentDestination: DrawerScreens by mutableStateOf(DrawerScreens.apiScreens.first())
    var api = App.apis.first()

    var wallpapers by mutableStateOf(
        listOf<Wallpaper>()
    )

    val favWallpapers: StateFlow<List<Wallpaper>> =
        DatabaseHolder.Database.favoritesDao().getFavoritesFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = listOf()
        )

    val recentlyAppliedWallpapers: StateFlow<List<Wallpaper>> =
        DatabaseHolder.Database.favoritesDao().getHistoryFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = listOf()
        )

    var titleResource by mutableStateOf<Either<Int, String>>(Either.Left(R.string.app_name))

    var page: Int = 1

    /** Whether wallpapers are being loaded right now. */
    var isLoading by mutableStateOf(false)
        private set

    /** Whether the last page that was loaded didn't contain any new wallpapers. */
    var lastPageEmpty by mutableStateOf(false)
        private set

    /**
     * Whether more wallpapers might be found, even if the last page was empty, e.g. because the
     * source has no search and only a part of its wallpapers have been checked for the tags yet.
     */
    var hasMoreResults by mutableStateOf(true)
        private set

    /** Whether loading the last page failed. */
    var loadingFailed by mutableStateOf(false)
        private set

    private var wallpaperJob: Job? = null
    fun fetchWallpapers(onException: (Exception, WallpaperApiWrapper) -> Unit) {
        // all wallpapers have been loaded already
        if (lastPageEmpty && !hasMoreResults) return

        // ensure that only one job for loading new wallpapers is run at a time
        // this prevents that quick switching between different wallpaper sources causes the app
        // to display images from all sources, because they were still loading in the background
        wallpaperJob?.cancel()
        // started only after it's stored, so that it can recognize itself when finishing
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            isLoading = true
            loadingFailed = false
            try {
                val newWallpapers = withContext(Dispatchers.IO) {
                    api.search(page)
                }.map { Wallpaper(it) }
                // remove duplicated results - e.g. if multiple posts contain the same image
                val allWallpapers = (wallpapers + newWallpapers).distinctBy { it.imgSrc }
                lastPageEmpty = allWallpapers.size == wallpapers.size
                hasMoreResults = api.hasMoreResults
                wallpapers = allWallpapers
                page += 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.toString())
                loadingFailed = true
                onException.invoke(e, api)
            } finally {
                // a newer job may have replaced this one in the meantime
                if (wallpaperJob == coroutineContext[Job]) isLoading = false
            }
        }
        wallpaperJob = job
        job.start()
    }

    fun addToFavorites(context: Context, wallpaper: Wallpaper) =
        viewModelScope.launch(Dispatchers.IO) {
            FavoritesHelper.addToFavorites(context, wallpaper)
        }

    fun removeFromFavorites(context: Context, wallpaper: Wallpaper) =
        viewModelScope.launch(Dispatchers.IO) {
            FavoritesHelper.removeFromFavorites(context, wallpaper)
        }

    fun removeRecentlyAppliedWallpaper(context: Context, wallpaper: Wallpaper) =
        viewModelScope.launch(Dispatchers.IO) {
            Database.favoritesDao().removeFromHistory(wallpaper)
            OfflineWallpaperHelper.deleteUnusedFiles(context)
        }

    fun setTags(wallpaper: Wallpaper, tags: List<String>) =
        viewModelScope.launch(Dispatchers.IO) {
            Database.favoritesDao().setTags(wallpaper.imgSrc, tags)
        }

    fun clearWallpapers() {
        wallpapers = listOf()
        page = 1
        lastPageEmpty = false
        hasMoreResults = true
        loadingFailed = false
    }
}
