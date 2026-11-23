package com.bnyro.wallpaper.obj

import android.content.Context
import androidx.work.NetworkType
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.enums.WallpaperSource
import com.bnyro.wallpaper.enums.WallpaperTarget
import com.bnyro.wallpaper.ext.formatMinutes
import com.bnyro.wallpaper.ui.nav.DrawerScreens
import kotlinx.serialization.Serializable

@Serializable
data class WallpaperConfig(
    val id: Int,
    var changeIntervalMinutes: Int = 12 * 60,
    var networkType: NetworkType = NetworkType.CONNECTED,
    var target: WallpaperTarget = WallpaperTarget.BOTH,
    var source: WallpaperSource = WallpaperSource.ONLINE,
    var applyImageFilters: Boolean = true,
    var selectedApiRoutes: List<String> = DrawerScreens.apiScreens.map { it.route },
    var localFolderUris: List<String> = listOf(),
    var startTimeMillis: Long? = null,
    var endTimeMillis: Long? = null,
    /** Only use favorites with any of these tags, all favorites if empty. */
    var favoriteTags: List<String> = listOf(),
    /** Only use wallpapers that generate a Material You theme of this color (ARGB), any if null. */
    var themeColor: Int? = null,
) {
    fun getSummary(context: Context): String {
        val targetString = when (target) {
            WallpaperTarget.HOME -> R.string.home
            WallpaperTarget.LOCK -> R.string.lockscreen
            WallpaperTarget.BOTH -> R.string.both
        }
        val sourceString = when (source) {
            WallpaperSource.LOCAL -> R.string.local
            WallpaperSource.ONLINE -> R.string.online
            WallpaperSource.FAVORITES -> R.string.favorites
        }

        return "${context.getString(sourceString)} - ${context.getString(targetString)} (${
            changeIntervalMinutes.toLong().formatMinutes()
        })"
    }
}
