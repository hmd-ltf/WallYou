package com.bnyro.wallpaper

import com.bnyro.wallpaper.util.ImageHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSampleSizeTest {
    private val screenLongSide = 2400

    @Test
    fun keepsImagesThatAreAlreadySmall() {
        assertEquals(1, ImageHelper.calculateInSampleSize(1920, 1080, screenLongSide))
    }

    @Test
    fun keepsImagesThatWouldBecomeSmallerThanTheScreen() {
        // 12 MP camera photo: halving it would make the short side 1500 px < 2400 px
        assertEquals(1, ImageHelper.calculateInSampleSize(4000, 3000, screenLongSide))
    }

    @Test
    fun downsamplesLargePhotosWhileTheyStillCoverTheScreen() {
        // 50 MP camera photo: 4080 x 3060 still covers the screen in any orientation
        assertEquals(2, ImageHelper.calculateInSampleSize(8160, 6120, screenLongSide))
        assertEquals(2, ImageHelper.calculateInSampleSize(6120, 8160, screenLongSide))
    }

    @Test
    fun downsamplesHugePanoramasEvenBelowScreenSize() {
        // 60 MP would need about 240 MB of memory
        assertEquals(2, ImageHelper.calculateInSampleSize(20000, 3000, screenLongSide))
    }

    @Test
    fun handlesUnknownImageSizes() {
        assertEquals(1, ImageHelper.calculateInSampleSize(-1, -1, screenLongSide))
        assertEquals(1, ImageHelper.calculateInSampleSize(0, 0, screenLongSide))
    }
}
