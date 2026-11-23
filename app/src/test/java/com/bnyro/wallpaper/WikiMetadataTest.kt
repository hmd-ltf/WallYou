package com.bnyro.wallpaper

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.wi.obj.WikiImagesResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WikiMetadataTest {
    // shortened response of the MediaWiki imageinfo API with extmetadata
    private val response = """
        {"batchcomplete":"","query":{"pages":{"-1":{"ns":6,"title":"File:Troglodytes troglodytes.jpg",
        "missing":"","known":"","imagerepository":"shared","imageinfo":[{"size":123,"width":4000,
        "height":3000,"thumburl":"https://upload.wikimedia.org/thumb.jpg","thumbwidth":300,"thumbheight":225,
        "url":"https://upload.wikimedia.org/wren.jpg","descriptionurl":"https://commons.wikimedia.org/wiki/File:Wren.jpg",
        "descriptionshorturl":"https://commons.wikimedia.org/w/index.php?curid=1",
        "extmetadata":{"ImageDescription":{"value":"A <a href=\"/wiki/Wren\">Eurasian wren</a> singing","source":"commons-desc-page"},
        "Categories":{"value":"Birds of Europe|Troglodytes troglodytes","source":"commons-categories","hidden":""},
        "DateTimeOriginal":{"value":20210512,"source":"commons-desc-page"}}}]}}}}
    """.trimIndent()

    @Test
    fun decodesMetadata() {
        val pages = RetrofitHelper.json.decodeFromString<WikiImagesResponse>(response).query.pages
        val info = pages.values.first().imageInfo.first()

        assertEquals("Birds of Europe|Troglodytes troglodytes", info.getMetadata("Categories"))
        assertEquals("A <a href=\"/wiki/Wren\">Eurasian wren</a> singing", info.getMetadata("ImageDescription"))
        // numbers are allowed as values, too
        assertEquals("20210512", info.getMetadata("DateTimeOriginal"))
        assertNull(info.getMetadata("Artist"))
    }
}
