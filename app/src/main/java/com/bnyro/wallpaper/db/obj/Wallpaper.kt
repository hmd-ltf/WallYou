package com.bnyro.wallpaper.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.youapps.wallpaper_apis.Wallpaper as ApiWallpaper

@Entity(tableName = "favorites")
@Serializable
data class Wallpaper(
    @PrimaryKey val imgSrc: String = "",
    @ColumnInfo val title: String? = null,
    @ColumnInfo val url: String? = null,
    @ColumnInfo var author: String? = null,
    @ColumnInfo val category: String? = null,
    @ColumnInfo val resolution: String? = null,
    @ColumnInfo val fileSize: Long? = null,
    @ColumnInfo val thumb: String? = null,
    @ColumnInfo val creationDate: String? = null,
    @ColumnInfo(defaultValue = "NULL") val description: String? = null,

    // The following ones may not be set by wallpaper APIs!
    @ColumnInfo(defaultValue = "1") var favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") var inHistory: Boolean = false,
    @ColumnInfo(defaultValue = "0") var timeAdded: Long = 0,

    /**
     * Absolute path of the copy of the image that is stored on the device, so that it's
     * available offline. Not part of backups, because the file only exists on this device.
     */
    @Transient
    @ColumnInfo(defaultValue = "NULL") val localPath: String? = null,

    /** Tags the user gave this wallpaper, see [com.bnyro.wallpaper.db.Converters]. */
    @ColumnInfo(defaultValue = "[]") val tags: List<String> = emptyList(),

    /**
     * The seed color (ARGB) Android would generate the Material You theme from,
     * [NO_SEED_COLOR] if the wallpaper has no suitable color, or null if not computed yet.
     */
    @ColumnInfo(defaultValue = "NULL") val seedColor: Int? = null,
) {
    /** The image shown in grids, preferring the copy stored on the device. */
    val preview get() = localFileUri ?: thumb ?: imgSrc

    /** The full size image, preferring the copy stored on the device. */
    val fullImage get() = localFileUri ?: imgSrc

    val isAvailableOffline get() = localPath != null

    private val localFileUri get() = localPath?.let { "file://$it" }

    constructor(wallpaper: ApiWallpaper) : this(
        imgSrc = wallpaper.imgSrc,
        title = wallpaper.title,
        url = wallpaper.url,
        author = wallpaper.author,
        category = wallpaper.author,
        resolution = wallpaper.resolution,
        fileSize = wallpaper.fileSize,
        thumb = wallpaper.thumb,
        creationDate = wallpaper.creationDate,
        description = wallpaper.description
    )

    companion object {
        /** Stored as [seedColor] when the wallpaper has no color to generate a theme from. */
        const val NO_SEED_COLOR = 0
    }
}
