package net.youapps.wallpaper_apis.wi.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class WikiImageInfo(
    @SerialName("descriptionshorturl") val descriptionShortUrl: String,
    @SerialName("descriptionurl") val descriptionUrl: String,
    val url: String,
    val width: Int,
    val height: Int,
    @SerialName("thumburl") val thumbUrl: String? = null,
    /**
     * Additional information about the image, e.g. its description and categories, see
     * https://www.mediawiki.org/wiki/Extension:CommonsMetadata.
     */
    @SerialName("extmetadata") val extMetadata: Map<String, WikiExtMetadataValue> = emptyMap()
) {
    /** The text of the given [extMetadata] field, which may contain HTML. */
    fun getMetadata(key: String): String? =
        (extMetadata[key]?.value as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
}

@Serializable
data class WikiExtMetadataValue(
    // usually a string, but may be a number or an object for some fields
    val value: JsonElement? = null
)
