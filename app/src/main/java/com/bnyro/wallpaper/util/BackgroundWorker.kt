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

            val (bitmap, onlineUrl) = when (config.source) {
                WallpaperSource.ONLINE -> getOnlineWallpaper(config, minSideLength)
                WallpaperSource.FAVORITES -> getFavoritesWallpaper(minSideLength) to null
                WallpaperSource.LOCAL -> getLocalWallpaper(config, minSideLength) to null
            }
            if (bitmap == null) return false

            if (config.applyImageFilters) {
                WallpaperHelper.setWallpaperWithFilters(
                    applicationContext,
                    bitmap,
                    config.target
                )
            } else {
                WallpaperHelper.setWallpaperWithoutFilters(
                    applicationContext,
                    bitmap,
                    config.target
                )
            }

            // only add the wallpaper to the history once it has actually been applied
            if (onlineUrl != null && Preferences.getBoolean(Preferences.wallpaperHistory, true)) {
                withContext(Dispatchers.IO) {
                    val wallpaper = Wallpaper(imgSrc = onlineUrl)
                    DatabaseHolder.Database.favoritesDao().insert(wallpaper, null, true)
                }
            }

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // also catches OutOfMemoryError, so that one failing config doesn't stop the others
            Log.e(TAG, "Failed to change wallpaper", e)
            false
        }
    }

    /** @return the loaded wallpaper and the URL it was loaded from */
    private suspend fun getOnlineWallpaper(
        config: WallpaperConfig,
        minSideLength: Int
    ): Pair<Bitmap?, String?> {
        val source = config.selectedApiRoutes.ifEmpty { return null to null }.random()

        return withContext(Dispatchers.IO) {
            val url = try {
                Preferences.getApiByRoute(source).getRandomWallpaperUrl()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext null to null
            } ?: return@withContext null to null

            val bitmap = ImageHelper.urlToBitmap(
                url,
                applicationContext,
                forceReload = true,
                minSideLength = minSideLength
            )
            bitmap to url
        }
    }

    private suspend fun getFavoritesWallpaper(minSideLength: Int): Bitmap? {
        val favoriteUrl = withContext(Dispatchers.IO) {
            DatabaseHolder.Database.favoritesDao().getRandomFavorite()
        }?.imgSrc
        return ImageHelper.urlToBitmap(
            favoriteUrl,
            applicationContext,
            forceReload = true,
            minSideLength = minSideLength
        )
    }

    private suspend fun getLocalWallpaper(config: WallpaperConfig, minSideLength: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            val wallpapers = LocalWallpaperHelper.getLocalWalls(applicationContext, config)
            if (wallpapers.isEmpty()) {
                Log.e(TAG, "No images found in the selected folders")
                return@withContext null
            }

            // use Java's random number generator with a custom seed instead of Kotlin's
            // see https://stackoverflow.com/questions/73475522/kotlin-random-always-generates-the-same-random-numbers
            val randomGenerator = Random(System.currentTimeMillis())
            val randomIndex = randomGenerator.nextInt(wallpapers.size)

            ImageHelper.getLocalImage(applicationContext, wallpapers[randomIndex], minSideLength)
        }
    }

    companion object {
        private const val TAG = "BackgroundWorker"
    }
}
