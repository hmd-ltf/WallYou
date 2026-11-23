package com.bnyro.wallpaper.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bnyro.wallpaper.db.obj.Wallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites")
    suspend fun getAll(): List<Wallpaper>

    @Query("SELECT * FROM favorites WHERE favorite = 1")
    suspend fun getFavorites(): List<Wallpaper>

    @Query("SELECT * FROM favorites WHERE favorite = 1 ORDER BY timeAdded DESC")
    fun getFavoritesFlow(): Flow<List<Wallpaper>>

    @Query("SELECT * FROM favorites WHERE inHistory = 1 ORDER BY timeAdded DESC")
    fun getHistoryFlow(): Flow<List<Wallpaper>>

    @Query("SELECT * FROM favorites WHERE inHistory = 1 ORDER BY timeAdded DESC")
    suspend fun getHistory(): List<Wallpaper>

    @Query("SELECT EXISTS (SELECT 1 FROM favorites WHERE favorite = 1 AND imgSrc = :imgSrc)")
    suspend fun isLiked(imgSrc: String): Boolean

    @Query("SELECT * FROM favorites WHERE imgSrc = :imgSrc")
    suspend fun findByImgSrc(imgSrc: String): Wallpaper?

    @Query("SELECT * FROM favorites WHERE imgSrc = :imgSrc")
    fun findByImgSrcFlow(imgSrc: String): Flow<Wallpaper?>

    /**
     * Do not use this method directly unless when restoring backups
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallpapers: List<Wallpaper>)

    /**
     * Add the [wallpaper] or update it if it's already stored, keeping the information that only
     * exists in the database (e.g. tags or the copy saved for offline use).
     *
     * @param isFavorite whether it's a favorite, null to keep the current state
     * @param isHistory whether it's in the history, null to keep the current state
     */
    suspend fun insert(wallpaper: Wallpaper, isFavorite: Boolean?, isHistory: Boolean?) {
        val existing = findByImgSrc(wallpaper.imgSrc)

        val favorite = isFavorite ?: existing?.favorite ?: false
        val inHistory = isHistory ?: existing?.inHistory ?: false
        // newly added or applied wallpapers should show up at the top of the lists
        val isNewEntry = existing == null || isHistory == true ||
            (isFavorite == true && !existing.favorite)

        val merged = wallpaper.copy(
            title = wallpaper.title ?: existing?.title,
            url = wallpaper.url ?: existing?.url,
            category = wallpaper.category ?: existing?.category,
            resolution = wallpaper.resolution ?: existing?.resolution,
            fileSize = wallpaper.fileSize ?: existing?.fileSize,
            thumb = wallpaper.thumb ?: existing?.thumb,
            creationDate = wallpaper.creationDate ?: existing?.creationDate,
            description = wallpaper.description ?: existing?.description,
            localPath = wallpaper.localPath ?: existing?.localPath,
            tags = wallpaper.tags.ifEmpty { existing?.tags.orEmpty() },
            seedColor = wallpaper.seedColor ?: existing?.seedColor,
        ).apply {
            author = wallpaper.author ?: existing?.author
            this.favorite = favorite
            this.inHistory = inHistory
            timeAdded = existing?.timeAdded?.takeIf { !isNewEntry } ?: System.currentTimeMillis()
        }

        if (existing != null) updateWallpaper(merged) else insertAll(listOf(merged))
    }

    @Update
    suspend fun updateWallpaper(wallpaper: Wallpaper)

    @Query("UPDATE favorites SET tags = :tags WHERE imgSrc = :imgSrc")
    suspend fun setTags(imgSrc: String, tags: List<String>)

    @Query("UPDATE favorites SET localPath = :localPath WHERE imgSrc = :imgSrc")
    suspend fun setLocalPath(imgSrc: String, localPath: String?)

    @Query("UPDATE favorites SET seedColor = :seedColor WHERE imgSrc = :imgSrc")
    suspend fun setSeedColor(imgSrc: String, seedColor: Int?)

    @Query("UPDATE favorites SET favorite = 0 WHERE imgSrc = :imgSrc")
    suspend fun unsetFavorite(imgSrc: String)

    @Query("UPDATE favorites SET inHistory = 0 WHERE imgSrc = :imgSrc")
    suspend fun unsetHistory(imgSrc: String)

    suspend fun removeFromFavorites(wallpaper: Wallpaper) {
        unsetFavorite(wallpaper.imgSrc)

        cleanup()
    }

    suspend fun removeFromHistory(wallpaper: Wallpaper) {
        unsetHistory(wallpaper.imgSrc)

        cleanup()
    }

    @Query("DELETE FROM favorites WHERE favorite = 0 and inHistory = 0")
    suspend fun cleanup()
}
