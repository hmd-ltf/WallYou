package com.bnyro.wallpaper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bnyro.wallpaper.db.DatabaseHolder
import com.bnyro.wallpaper.db.obj.Wallpaper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.youapps.wallpaper_apis.RetrofitHelper
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Keeps copies of wallpapers on the device, so that they are available without internet:
 * all favorites and the most recently applied wallpapers (see [Preferences.historyOfflineCountKey]).
 */
object OfflineWallpaperHelper {
    private const val TAG = "OfflineWallpaperHelper"
    private const val DIRECTORY_NAME = "offline_wallpapers"
    private const val TEMP_SUFFIX = ".tmp"

    /** Unfinished downloads older than this are left overs and can be deleted. */
    private const val STALE_TEMP_FILE_MILLIS = 60 * 60 * 1000L

    /**
     * Guards changes to the stored files and their paths in the database, so that a file isn't
     * deleted as unused while it's about to be saved to the database.
     */
    private val mutex = Mutex()

    private val dao get() = DatabaseHolder.Database.favoritesDao()

    private fun getDirectory(context: Context) =
        File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }

    private fun getFile(context: Context, imgSrc: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(imgSrc.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(getDirectory(context), hash)
    }

    private fun canDownload(imgSrc: String) =
        imgSrc.startsWith("https://") || imgSrc.startsWith("http://")

    /**
     * How many of the most recently applied wallpapers are kept on the device.
     */
    fun getHistoryOfflineCount(): Int {
        if (!Preferences.getBoolean(Preferences.wallpaperHistory, true)) return 0

        return Preferences.getString(
            Preferences.historyOfflineCountKey,
            Preferences.defaultHistoryOfflineCount.toString()
        ).toIntOrNull() ?: Preferences.defaultHistoryOfflineCount
    }

    /**
     * The wallpapers (by [Wallpaper.imgSrc]) of which a copy should be kept on the device.
     */
    private fun getWallpapersToKeep(wallpapers: List<Wallpaper>): Set<String> {
        val recentlyApplied = wallpapers
            .filter { it.inHistory }
            .sortedByDescending { it.timeAdded }
            .take(getHistoryOfflineCount())

        return (wallpapers.filter { it.favorite } + recentlyApplied).map { it.imgSrc }.toSet()
    }

    /**
     * Download the image at [imgSrc] into the offline storage, without saving it to the database yet.
     *
     * @return the downloaded file, or null if downloading failed
     */
    suspend fun download(context: Context, imgSrc: String): File? = withContext(Dispatchers.IO) {
        if (!canDownload(imgSrc)) return@withContext null

        val target = getFile(context, imgSrc)
        if (target.exists() && target.length() > 0) return@withContext target

        // unique per download, in case the same image is downloaded twice at the same time
        val tempFile = File.createTempFile(target.name, TEMP_SUFFIX, target.parentFile)
        try {
            val request = Request.Builder().url(imgSrc).build()
            RetrofitHelper.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Downloading $imgSrc failed with status ${response.code}")
                    return@withContext null
                }

                tempFile.outputStream().use { response.body.byteStream().copyTo(it) }
            }

            // make sure that the file is an image that can be loaded later
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempFile.path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e(TAG, "$imgSrc is not a supported image")
                tempFile.delete()
                return@withContext null
            }

            if (!tempFile.renameTo(target)) {
                tempFile.delete()
                return@withContext null
            }
            target
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Downloading $imgSrc failed", e)
            tempFile.delete()
            null
        }
    }

    /**
     * Remember in the database that the [file] is the offline copy of the wallpaper with [imgSrc].
     */
    suspend fun saveLocalPath(imgSrc: String, file: File) = mutex.withLock {
        withContext(Dispatchers.IO) {
            dao.setLocalPath(imgSrc, file.absolutePath)
        }
    }

    /**
     * Delete the offline copies that are no longer needed, e.g. because the wallpaper isn't a
     * favorite anymore or too many wallpapers have been applied since then.
     */
    suspend fun deleteUnusedFiles(context: Context) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val wallpapers = dao.getAll()
            val toKeep = getWallpapersToKeep(wallpapers)

            for (wallpaper in wallpapers) {
                val path = wallpaper.localPath ?: continue
                if (wallpaper.imgSrc !in toKeep || !File(path).exists()) {
                    File(path).delete()
                    dao.setLocalPath(wallpaper.imgSrc, null)
                }
            }

            // remove files whose wallpaper has been deleted from the database
            val usedPaths = wallpapers
                .filter { it.imgSrc in toKeep }
                .mapNotNull { it.localPath }
                .toSet()
            val now = System.currentTimeMillis()
            getDirectory(context).listFiles()?.forEach { file ->
                // unfinished downloads may still be running
                val isRunningDownload = file.name.endsWith(TEMP_SUFFIX) &&
                    now - file.lastModified() < STALE_TEMP_FILE_MILLIS
                if (file.absolutePath !in usedPaths && !isRunningDownload) file.delete()
            }
        }
    }

    /**
     * Download all wallpapers that should be available offline but aren't yet, compute missing
     * seed colors and delete files that are no longer needed.
     *
     * @return whether all wallpapers could be downloaded
     */
    suspend fun sync(context: Context): Boolean = withContext(Dispatchers.IO) {
        deleteUnusedFiles(context)

        var success = true
        val wallpapers = dao.getAll()
        val toKeep = getWallpapersToKeep(wallpapers)
        for (wallpaper in wallpapers) {
            if (wallpaper.imgSrc !in toKeep || wallpaper.localPath != null) continue

            val file = download(context, wallpaper.imgSrc)
            if (file != null) {
                saveLocalPath(wallpaper.imgSrc, file)
            } else if (canDownload(wallpaper.imgSrc)) {
                success = false
            }
        }

        // the seed colors are needed to filter favorites by their theme color
        for (wallpaper in dao.getAll()) {
            if (wallpaper.seedColor != null) continue
            val path = wallpaper.localPath ?: continue

            val seedColor = computeSeedColor(File(path)) ?: continue
            dao.setSeedColor(wallpaper.imgSrc, seedColor)
        }

        // the history may have changed while downloading
        deleteUnusedFiles(context)
        success
    }

    /**
     * Compute the seed color of the Material You theme the image in [file] generates.
     */
    fun computeSeedColor(file: File): Int? {
        val bitmap = decodeSampledBitmap(file, ThemeColorHelper.SEED_COLOR_IMAGE_SIZE) ?: return null
        return ThemeColorHelper.getSeedColor(bitmap)
    }

    /**
     * Decode the image in [file], downsampled as far as possible while both sides stay at least
     * [minSideLength] long.
     */
    fun decodeSampledBitmap(file: File, minSideLength: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize =
            ImageHelper.calculateInSampleSize(options.outWidth, options.outHeight, minSideLength)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.path, options)
    }
}
