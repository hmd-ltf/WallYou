package com.bnyro.wallpaper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.exifinterface.media.ExifInterface
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ImageHelper {
    fun buildRequest(context: Context, url: String): ImageRequest =
        createRequestBuilder(context, url).build()

    private fun createRequestBuilder(
        context: Context,
        url: String,
        forceReload: Boolean = false
    ): ImageRequest.Builder {
        val imageRequestBuilder = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(Size.ORIGINAL)

        if (forceReload) {
            // disable all caches for this request to fresh-reload the image
            imageRequestBuilder
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
        }

        return imageRequestBuilder
    }

    fun urlToBitmap(
        scope: CoroutineScope,
        imageURL: String?,
        context: Context,
        onSuccess: (bitmap: Bitmap) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val request = createRequestBuilder(context, imageURL ?: return@launch)
                .target {
                    onSuccess(it.toBitmap())
                }
                .build()
            context.imageLoader.enqueue(request)
        }
    }

    /**
     * @param minSideLength When positive, the image is downsampled as far as possible while both
     * of its sides stay at least this long, instead of being loaded at its original size.
     */
    suspend fun urlToBitmap(
        imageURL: String?,
        context: Context,
        forceReload: Boolean = false,
        minSideLength: Int = 0
    ): Bitmap? {
        if (imageURL == null) return null

        val imageRequest = createRequestBuilder(context, imageURL, forceReload)
        if (minSideLength > 0) {
            imageRequest
                .size(minSideLength, minSideLength)
                .scale(Scale.FILL)
                // never upscale images that are smaller than the requested size
                .precision(Precision.INEXACT)
        }
        val result = context.imageLoader.execute(imageRequest.build())

        if (result is SuccessResult) {
            return result.image.toBitmap()
        } else if (result is ErrorResult) {
            Log.e("error loading image", result.throwable.localizedMessage.orEmpty())
        }
        return null
    }

    private fun rotateBitmap(bitmap: Bitmap, exifInterface: ExifInterface): Bitmap {
        val orientation = runCatching {
            exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrElse { return bitmap }

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> rotateImage(
                bitmap,
                90f
            )

            ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> rotateImage(
                bitmap,
                180f
            )

            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> rotateImage(
                bitmap,
                -90f
            )

            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, rotation: Float): Bitmap {
        val matrix = Matrix().apply {
            setScale(-1f, 1f)
            setRotate(rotation)
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return oriented
    }

    /**
     * @param minSideLength When positive, the image is downsampled as far as possible while both
     * of its sides stay at least this long, instead of being decoded at its original size.
     */
    fun getLocalImage(context: Context, imagePath: Uri, minSideLength: Int = 0): Bitmap? {
        val options = BitmapFactory.Options()
        if (minSideLength > 0) {
            options.inJustDecodeBounds = true
            context.contentResolver.openInputStream(imagePath)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize =
                calculateInSampleSize(options.outWidth, options.outHeight, minSideLength)
            options.inJustDecodeBounds = false
        }

        return context.contentResolver.openFileDescriptor(imagePath, "r")?.use {
            val bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
                ?: return null
            val exifInterface =
                ExifInterface(it.fileDescriptor) // This will change fileDescriptor position
            rotateBitmap(bitmap, exifInterface)
        }
    }

    /**
     * The largest power of two the image can be scaled down by while both of its sides stay
     * at least [minSideLength] long, or while it would still have more than [MAX_DECODED_PIXELS]
     * pixels.
     */
    fun calculateInSampleSize(width: Int, height: Int, minSideLength: Int): Int {
        if (width <= 0 || height <= 0) return 1

        var sampleSize = 1
        while (true) {
            val nextSize = sampleSize * 2
            val keepsMinSize = minOf(width, height) / nextSize >= minSideLength
            val tooManyPixels =
                width.toLong() * height / (sampleSize.toLong() * sampleSize) > MAX_DECODED_PIXELS
            if (!keepsMinSize && !tooManyPixels) break
            if (minOf(width, height) / nextSize < 1) break
            sampleSize = nextSize
        }
        return sampleSize
    }

    /** About 100 MB with 4 bytes per pixel. */
    private const val MAX_DECODED_PIXELS = 25_000_000L
}
