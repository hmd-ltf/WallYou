package net.youapps.wallpaper_apis.wh.obj

import kotlinx.serialization.Serializable

@Serializable
data class WhSettingsResponse(
    val `data`: WhUserSettings? = null
)

@Serializable
data class WhUserSettings(
    val purity: List<String>? = null,
    val categories: List<String>? = null,
    val resolutions: List<String>? = null,
    val aspect_ratios: List<String>? = null,
    val toplist_range: String? = null,
    val tag_blacklist: List<String>? = null
)
