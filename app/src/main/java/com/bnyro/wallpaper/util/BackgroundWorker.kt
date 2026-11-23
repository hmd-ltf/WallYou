package com.bnyro.wallpaper.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bnyro.wallpaper.db.DatabaseHolder
import com.bnyro.wallpaper.db.obj.Wallpaper
import com.bnyro.wallpaper.enums.WallpaperSource
import com.bnyro.wallpaper.obj.WallpaperConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Random

class BackgroundWorker(
    applicationContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(applicationContext, workerParameters) {
    override suspend fun doWork(): Result {
        val wallpaperConfigs = Preferences.getWallpaperConfigs()

        val configId = workerParameters.inputData.getInt(WorkerHelper.WALLPAPER_CONFIG_ID, -1)
        if (configId == -1) {
            for (config in wallpaperConfigs) {
                runWallpaperChanger(config)
            }
            return Result.success()
        }

        val config = wallpaperConfigs.firstOrNull {
            it.id == configId
        } ?: return Result.success()

        val nowMillis = TimeHelper.timeTodayInMillis()
        if (config.startTimeMillis != null && config.endTimeMillis != null &&
            !TimeHelper.isInTimeRange(nowMillis, config.startTimeMillis!!, config.endTimeMillis!!)
        ) return Result.success()

        return if (runWallpaperChanger(config)) Result.success()
        else Result.retry()
    }

    /**
     * A wallpaper that was picked to be applied.
     *
     * @param onlineUrl the URL it was loaded from, if it's an online wallpaper
     * @param offlineFile the copy of the online wallpaper that was saved on the device
     * @param seedColor the seed color of its Material You theme, if it has been computed
     */
    private class PickedWallpaper(
        val bitmap: Bitmap,
        val onlineUrl: String? = null,
        val offlineFile: File? = null,
        val seedColor: Int? = null
    )

    /**
     * Thrown when none of the wallpapers matches the filters of the config, e.g. the theme color.
     * Retrying won't help in that case.
     */
    private class NoMatchingWallpaperException(message: String) : Exception(message)

    /**
     * Fetch and apply a wallpaper using the given config
     *
     * @return Whether the wallpaper change applied successfully without errors
     */
    private suspend fun runWallpaperChanger(config: WallpaperConfig): Boolean {
        return try {
            // Images are loaded only as large as needed to cover the screen in any orientation.
            // Loading photos and online wallpapers at full resolution can use hundreds of MB and
            // make the worker run out of memory, so the wallpaper silently never changes.
            val (screenWidth, screenHeight) = WallpaperHelper.getMetrics(applicationContext)
            val minSideLength = maxOf(screenWidth, screenHeight)

            val picked = when (config.source) {
                WallpaperSource.ONLINE -> getOnlineWallpaper(config, minSideLength)
                WallpaperSource.FAVORITES -> getFavoritesWallpaper(config, minSideLength)
                WallpaperSource.LOCAL -> getLocalWallpaper(config, minSideLength)
            } ?: return false

            if (config.applyImageFilters) {
                WallpaperHelper.setWallpaperWithFilters(
                    applicationContext,
                    picked.bitmap,
                    config.target
                )
            } else {
                WallpaperHelper.setWallpaperWithoutFilters(
                    applicationContext,
                    picked.bitmap,
                    config.target
                )
            }

            // only add the wallpaper to the history once it has actually been applied
            if (picked.onlineUrl != null) addToHistory(picked)

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoMatchingWallpaperException) {
            Log.i(TAG, e.message.orEmpty())
            true
        } catch (e: Throwable) {
            // also catches OutOfMemoryError, so that one failing config doesn't stop the others
            Log.e(TAG, "Failed to change wallpaper", e)
            false
        } finally {
            if (config.source == WallpaperSource.ONLINE) {
                // downloads of wallpapers that didn't match or that are too old to be kept
                OfflineWallpaperHelper.deleteUnusedFiles(applicationContext)
            }
        }
    }

    private suspend fun addToHistory(picked: PickedWallpaper) = withContext(Dispatchers.IO) {
        if (!Preferences.getBoolean(Preferences.wallpaperHistory, true)) return@withContext

        val url = picked.onlineUrl ?: return@withContext
        val wallpaper = Wallpaper(imgSrc = url, seedColor = picked.seedColor)
        DatabaseHolder.Database.favoritesDao().insert(wallpaper, null, true)
        picked.offlineFile?.let { OfflineWallpaperHelper.saveLocalPath(url, it) }
    }

    private suspend fun getOnlineWallpaper(
        config: WallpaperConfig,
        minSideLength: Int
    ): PickedWallpaper? {
        if (config.selectedApiRoutes.isEmpty()) return null

        // keep a copy of the wallpaper, so that it can be saved or liked later, even without internet
        val keepOffline = OfflineWallpaperHelper.getHistoryOfflineCount() > 0
        val themeColor = config.themeColor
        val attempts = if (themeColor != null) MAX_ONLINE_THEME_ATTEMPTS else 1

        var foundNonMatching = false
        repeat(attempts) {
            val source = config.selectedApiRoutes.random()
            val url = withContext(Dispatchers.IO) {
                try {
                    Preferences.getApiByRoute(source).randomWallpaperUrl()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    null
                }
            } ?: return@repeat

            val offlineFile = if (keepOffline) {
                OfflineWallpaperHelper.download(applicationContext, url)
            } else {
                null
            }
            val bitmap = ImageHelper.urlToBitmap(
                offlineFile?.let { "file://${it.absolutePath}" } ?: url,
                applicationContext,
                forceReload = true,
                minSideLength = minSideLength
            ) ?: return@repeat

            val seedColor = ThemeColorHelper.getSeedColor(bitmap)
            if (themeColor != null && !ThemeColorHelper.matchesThemeColor(seedColor, themeColor)) {
                foundNonMatching = true
                return@repeat
            }

            return PickedWallpaper(bitmap, url, offlineFile, seedColor)
        }

        if (foundNonMatching) {
            throw NoMatchingWallpaperException("No online wallpaper matched the theme color")
        }
        return null
    }

    private suspend fun getFavoritesWallpaper(
        config: WallpaperConfig,
        minSideLength: Int
    ): PickedWallpaper? {
        val dao = DatabaseHolder.Database.favoritesDao()
        val favorites = withContext(Dispatchers.IO) { dao.getFavorites() }
            .filter { favorite ->
                config.favoriteTags.isEmpty() || favorite.tags.any { it in config.favoriteTags }
            }
        if (favorites.isEmpty()) {
            throw NoMatchingWallpaperException("No favorites with the selected tags")
        }

        val themeColor = config.themeColor
        var seedColorsComputed = 0
        var failedLoads = 0
        for (favorite in favorites.shuffled(Random(System.currentTimeMillis()))) {
            if (themeColor != null) {
                val seedColor = favorite.seedColor ?: run {
                    // only compute a limited number of missing seed colors per run, since this
                    // might require downloading the wallpapers
                    if (seedColorsComputed >= MAX_SEED_COLOR_COMPUTATIONS) return@run null
                    seedColorsComputed++
                    computeSeedColor(favorite)?.also { dao.setSeedColor(favorite.imgSrc, it) }
                } ?: continue

                if (!ThemeColorHelper.matchesThemeColor(seedColor, themeColor)) continue
            }

            // prefer the copy on the device, so that this also works without internet
            val bitmap = ImageHelper.urlToBitmap(
                favorite.fullImage,
                applicationContext,
                forceReload = true,
                minSideLength = minSideLength
            ) ?: favorite.localPath?.let {
                ImageHelper.urlToBitmap(
                    favorite.imgSrc,
                    applicationContext,
                    forceReload = true,
                    minSideLength = minSideLength
                )
            }
            if (bitmap != null) return PickedWallpaper(bitmap)

            // e.g. no internet and not saved for offline use yet, try another one
            if (++failedLoads >= MAX_FAILED_LOADS) return null
        }

        if (themeColor != null && failedLoads == 0) {
            throw NoMatchingWallpaperException("No favorite matched the theme color")
        }
        return null
    }

    private suspend fun computeSeedColor(wallpaper: Wallpaper): Int? = withContext(Dispatchers.IO) {
        wallpaper.localPath?.let { path ->
            OfflineWallpaperHelper.computeSeedColor(File(path))?.let { return@withContext it }
        }

        val bitmap = ImageHelper.urlToBitmap(
            wallpaper.imgSrc,
            applicationContext,
            minSideLength = ThemeColorHelper.SEED_COLOR_IMAGE_SIZE
        ) ?: return@withContext null
        ThemeColorHelper.getSeedColor(bitmap)
    }

    private suspend fun getLocalWallpaper(
        config: WallpaperConfig,
        minSideLength: Int
    ): PickedWallpaper? = withContext(Dispatchers.IO) {
        val wallpapers = LocalWallpaperHelper.getLocalWalls(applicationContext, config)
        if (wallpapers.isEmpty()) {
            Log.e(TAG, "No images found in the selected folders")
            return@withContext null
        }

        // use Java's random number generator with a custom seed instead of Kotlin's
        // see https://stackoverflow.com/questions/73475522/kotlin-random-always-generates-the-same-random-numbers
        val randomGenerator = Random(System.currentTimeMillis())

        val themeColor = config.themeColor
        if (themeColor == null) {
            val randomIndex = randomGenerator.nextInt(wallpapers.size)
            val bitmap = ImageHelper.getLocalImage(
                applicationContext,
                wallpapers[randomIndex],
                minSideLength
            )
            return@withContext bitmap?.let { PickedWallpaper(it) }
        }

        for (uri in wallpapers.shuffled(randomGenerator).take(MAX_LOCAL_THEME_CHECKS)) {
            val preview = ImageHelper.getLocalImage(
                applicationContext,
                uri,
                ThemeColorHelper.SEED_COLOR_IMAGE_SIZE
            ) ?: continue
            if (!ThemeColorHelper.matchesThemeColor(ThemeColorHelper.getSeedColor(preview), themeColor)) {
                continue
            }

            val bitmap = ImageHelper.getLocalImage(applicationContext, uri, minSideLength)
            if (bitmap != null) return@withContext PickedWallpaper(bitmap)
        }

        throw NoMatchingWallpaperException("No local wallpaper matched the theme color")
    }

    companion object {
        private const val TAG = "BackgroundWorker"

        /** How many online wallpapers are tried at most to find one matching the theme color. */
        private const val MAX_ONLINE_THEME_ATTEMPTS = 5

        /** How many local images are checked at most to find one matching the theme color. */
        private const val MAX_LOCAL_THEME_CHECKS = 20

        /** How many missing seed colors of favorites are computed at most per run. */
        private const val MAX_SEED_COLOR_COMPUTATIONS = 10

        /** After how many favorites that couldn't be loaded the changer gives up for now. */
        private const val MAX_FAILED_LOADS = 3
    }
}
