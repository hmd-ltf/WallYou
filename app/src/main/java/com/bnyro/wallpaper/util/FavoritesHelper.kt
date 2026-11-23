package com.bnyro.wallpaper.util

import android.content.Context
import com.bnyro.wallpaper.db.DatabaseHolder
import com.bnyro.wallpaper.db.obj.Wallpaper

object FavoritesHelper {
    /**
     * Add the [wallpaper] to the favorites and save it for offline use.
     */
    suspend fun addToFavorites(context: Context, wallpaper: Wallpaper) {
        DatabaseHolder.Database.favoritesDao().insert(wallpaper, true, null)
        OfflineSyncWorker.enqueue(context.applicationContext)
    }

    /**
     * Remove the [wallpaper] from the favorites and delete its offline copy if it's not needed anymore.
     */
    suspend fun removeFromFavorites(context: Context, wallpaper: Wallpaper) {
        DatabaseHolder.Database.favoritesDao().removeFromFavorites(wallpaper)
        OfflineWallpaperHelper.deleteUnusedFiles(context.applicationContext)
    }

    /**
     * All tags that are used by the given favorites, sorted alphabetically.
     */
    fun getAllTags(favorites: List<Wallpaper>): List<String> =
        favorites.flatMap { it.tags }.distinct().sortedBy { it.lowercase() }
}
