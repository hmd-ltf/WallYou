package com.bnyro.wallpaper.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bnyro.wallpaper.db.dao.FavoritesDao
import com.bnyro.wallpaper.db.obj.Wallpaper

@Database(
    version = 4,
    autoMigrations = [
        AutoMigration(1, 2),
        AutoMigration(2, 3),
        AutoMigration(3, 4)
    ],
    entities = [
        Wallpaper::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}
