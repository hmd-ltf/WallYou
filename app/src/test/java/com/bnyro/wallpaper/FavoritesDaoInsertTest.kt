package com.bnyro.wallpaper

import com.bnyro.wallpaper.db.dao.FavoritesDao
import com.bnyro.wallpaper.db.obj.Wallpaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [FavoritesDao.insert], which merges new information into wallpapers that are already stored.
 */
class FavoritesDaoInsertTest {
    private class FakeDao : FavoritesDao {
        val rows = linkedMapOf<String, Wallpaper>()

        override suspend fun getAll() = rows.values.toList()
        override suspend fun getFavorites() = rows.values.filter { it.favorite }
        override fun getFavoritesFlow(): Flow<List<Wallpaper>> = flowOf(emptyList())
        override fun getHistoryFlow(): Flow<List<Wallpaper>> = flowOf(emptyList())
        override suspend fun getHistory() = rows.values.filter { it.inHistory }
        override suspend fun isLiked(imgSrc: String) = rows[imgSrc]?.favorite == true
        override suspend fun findByImgSrc(imgSrc: String) = rows[imgSrc]
        override fun findByImgSrcFlow(imgSrc: String): Flow<Wallpaper?> = flowOf(rows[imgSrc])
        override suspend fun insertAll(wallpapers: List<Wallpaper>) {
            wallpapers.forEach { rows[it.imgSrc] = it }
        }
        override suspend fun updateWallpaper(wallpaper: Wallpaper) {
            rows[wallpaper.imgSrc] = wallpaper
        }
        override suspend fun setTags(imgSrc: String, tags: List<String>) {
            rows[imgSrc] = rows[imgSrc]!!.copy(tags = tags)
        }
        override suspend fun setLocalPath(imgSrc: String, localPath: String?) {
            rows[imgSrc] = rows[imgSrc]!!.copy(localPath = localPath)
        }
        override suspend fun setSeedColor(imgSrc: String, seedColor: Int?) {
            rows[imgSrc] = rows[imgSrc]!!.copy(seedColor = seedColor)
        }
        override suspend fun unsetFavorite(imgSrc: String) {
            rows[imgSrc]?.favorite = false
        }
        override suspend fun unsetHistory(imgSrc: String) {
            rows[imgSrc]?.inHistory = false
        }
        override suspend fun cleanup() {
            rows.values.removeAll { !it.favorite && !it.inHistory }
        }
    }

    private val src = "https://example.com/wallpaper.jpg"

    @Test
    fun applyingAFavoriteKeepsItsTagsAndOfflineCopy() = runBlocking {
        val dao = FakeDao()
        dao.insert(Wallpaper(imgSrc = src, title = "Mountains"), isFavorite = true, isHistory = null)
        dao.setTags(src, listOf("nature"))
        dao.setLocalPath(src, "/files/offline_wallpapers/abc")

        // the wallpaper changer only knows the URL when it adds a wallpaper to the history
        dao.insert(Wallpaper(imgSrc = src, seedColor = 0xFF1E88E5.toInt()), isFavorite = null, isHistory = true)

        val stored = dao.findByImgSrc(src)!!
        assertTrue(stored.favorite)
        assertTrue(stored.inHistory)
        assertEquals("Mountains", stored.title)
        assertEquals(listOf("nature"), stored.tags)
        assertEquals("/files/offline_wallpapers/abc", stored.localPath)
        assertEquals(0xFF1E88E5.toInt(), stored.seedColor)
    }

    @Test
    fun applyingAgainMovesTheWallpaperToTheTop() = runBlocking {
        val dao = FakeDao()
        dao.insert(Wallpaper(imgSrc = src), isFavorite = null, isHistory = true)
        dao.updateWallpaper(dao.findByImgSrc(src)!!.copy().apply { timeAdded = 1 })

        dao.insert(Wallpaper(imgSrc = src), isFavorite = null, isHistory = true)

        assertTrue(dao.findByImgSrc(src)!!.timeAdded > 1)
    }

    @Test
    fun updatingInformationKeepsTheTimeAdded() = runBlocking {
        val dao = FakeDao()
        dao.insert(Wallpaper(imgSrc = src), isFavorite = true, isHistory = null)
        dao.updateWallpaper(dao.findByImgSrc(src)!!.copy().apply {
            timeAdded = 1
            favorite = true
        })

        dao.insert(Wallpaper(imgSrc = src), isFavorite = true, isHistory = null)

        assertEquals(1L, dao.findByImgSrc(src)!!.timeAdded)
    }

    @Test
    fun unfavoritingKeepsHistoryEntries() = runBlocking {
        val dao = FakeDao()
        dao.insert(Wallpaper(imgSrc = src), isFavorite = true, isHistory = true)

        dao.removeFromFavorites(Wallpaper(imgSrc = src))

        val stored = dao.findByImgSrc(src)!!
        assertEquals(false, stored.favorite)
        assertTrue(stored.inHistory)
    }

    @Test
    fun localCopyIsPreferredForDisplaying() {
        val wallpaper = Wallpaper(imgSrc = src, thumb = "https://example.com/thumb.jpg")
        assertEquals("https://example.com/thumb.jpg", wallpaper.preview)
        assertEquals(src, wallpaper.fullImage)

        val offline = wallpaper.copy(localPath = "/files/offline_wallpapers/abc")
        assertEquals("file:///files/offline_wallpapers/abc", offline.preview)
        assertEquals("file:///files/offline_wallpapers/abc", offline.fullImage)
    }
}
