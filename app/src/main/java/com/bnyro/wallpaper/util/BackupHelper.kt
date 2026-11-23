package com.bnyro.wallpaper.util

import android.content.Context
import android.net.Uri
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.db.DatabaseHolder
import com.bnyro.wallpaper.db.obj.BackupFile
import com.bnyro.wallpaper.ext.toastFromMainThread
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.youapps.wallpaper_apis.RetrofitHelper

object BackupHelper {
    const val JSON_MIME = "application/json"

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun backupFavorites(uri: Uri, context: Context) {
        try {
            val favorites = DatabaseHolder.Database.favoritesDao().getAll()
            val backupFile = BackupFile(favorites = favorites)
            context.contentResolver.openOutputStream(uri)?.use {
                RetrofitHelper.json.encodeToStream(backupFile, it)
            }
            context.toastFromMainThread(context.getString(R.string.success))
        } catch (e: Exception) {
            context.toastFromMainThread(e.localizedMessage)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun restoreFavorites(uri: Uri, context: Context) {
        try {
            context.contentResolver.openInputStream(uri)?.use {
                val backupFile = RetrofitHelper.json.decodeFromStream<BackupFile>(it)
                val dao = DatabaseHolder.Database.favoritesDao()
                // keep the copies of already stored wallpapers that are saved on this device
                val existing = dao.getAll().associateBy { wallpaper -> wallpaper.imgSrc }
                dao.insertAll(backupFile.favorites.map { wallpaper ->
                    val stored = existing[wallpaper.imgSrc] ?: return@map wallpaper
                    wallpaper.copy(
                        localPath = stored.localPath,
                        seedColor = wallpaper.seedColor ?: stored.seedColor,
                        tags = wallpaper.tags.ifEmpty { stored.tags }
                    ).apply {
                        // don't remove wallpapers from the history that are only in the backup as favorites
                        inHistory = inHistory || stored.inHistory
                    }
                })
            }
            // download the restored favorites for offline use
            OfflineSyncWorker.enqueue(context.applicationContext)
            context.toastFromMainThread(context.getString(R.string.success))
        } catch (e: Exception) {
            context.toastFromMainThread(e.localizedMessage)
        }
    }
}