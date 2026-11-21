package com.bnyro.wallpaper.util

import androidx.compose.ui.graphics.vector.ImageVector
import net.youapps.wallpaper_apis.TextUtils
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

/**
 * A wrapper around [WallpaperApi] that uses [Preferences] to store the engine settings.
 */
class WallpaperApiWrapper(private val api: WallpaperApi, val icon: ImageVector): WallpaperApi() {
    val route get() = api.name.replace(" ", "_").lowercase()

    // Delegated to underlying [api]
    override val name: String get() = api.name
    override val baseUrl: String get() = api.baseUrl
    override val availableFilters: Map<String, List<String>> get() = api.availableFilters
    override val multiSelectFilters: Set<String> get() = api.multiSelectFilters
    override val optionalFilters: Set<String> get() = api.optionalFilters
    override val defaultFilters: Map<String, String> get() = api.defaultFilters
    override val supportsTags: Boolean get() = api.supportsTags
    override val suggestedTags: List<String> get() = api.suggestedTags
    override val supportsTagExclusion: Boolean get() = api.supportsTagExclusion
    override val requiresCommunityName: Boolean get() = api.requiresCommunityName
    override val supportsApiKey: Boolean get() = api.supportsApiKey
    override val apiKeyUrl: String? get() = api.apiKeyUrl

    override fun isFilterApplicable(key: String, filters: Map<String, String>) =
        api.isFilterApplicable(key, filters)

    override suspend fun validateApiKey(apiKey: String): Boolean = api.validateApiKey(apiKey)

    override suspend fun getWallpapers(page: Int): List<Wallpaper> = api.getWallpapers(page)
    override suspend fun getRandomWallpaperUrl(): String? = api.getRandomWallpaperUrl()

    init {
        // the api key has to be set first, as it may change the available filters
        api.apiKey = apiKey

        migrateLegacyFilters()

        // copy all values from the [WallpaperApiWrapper] to the underlying [WallpaperApi] instance
        api.communityName = communityName
        api.selectedTags = selectedTags
        api.selectedFilters = selectedFilters
    }

    // Engine settings that automatically get stored to [Preferences]
    override var communityName: String?
        get() = getPref("community", api.communityName.orEmpty())
        set(value) {
            setPref("community", value.orEmpty())
            api.communityName = value
        }

    override var apiKey: String?
        get() = getPref("apiKey", "").takeIf { it.isNotBlank() }
        set(value) {
            setPref("apiKey", value?.trim().orEmpty())
            api.apiKey = value?.trim()?.takeIf { it.isNotBlank() }

            // the available filters may have changed, e.g. NSFW content is only available when logged in
            api.selectedFilters = selectedFilters
        }

    override var selectedFilters: Map<String, String>
        get() = run {
            val defaults = api.defaultFilters

            api.availableFilters.mapValues { (key, options) ->
                sanitizeFilter(key, options, getPref(key, defaults[key].orEmpty()), defaults[key].orEmpty())
            }
        }
        set(filters) {
            for ((key, value) in filters.entries) {
                setPref(key, value)
            }

            api.selectedFilters = selectedFilters
        }

    override var selectedTags: List<String>
        get() = getPref("tags", "").split(",").filter {
            it.isNotBlank()
        }
        set(value) {
            setPref("tags", value.joinToString(","))
            api.selectedTags = value
        }

    private fun getPref(key: String, defValue: String): String {
        return Preferences.getString(api.name + key, defValue)
    }

    private fun setPref(key: String, value: String) {
        Preferences.edit { putString(api.name + key, value) }
    }

    /**
     * Ensure that the [value] of a filter only contains options from [options],
     * falling back to [defaultValue] if no valid option is selected.
     */
    private fun sanitizeFilter(
        key: String,
        options: List<String>,
        value: String,
        defaultValue: String
    ): String {
        if (key !in api.multiSelectFilters) {
            return value.takeIf { it in options } ?: defaultValue
        }

        val selected = TextUtils.splitFilterValues(value).filter { it in options }
        if (selected.isEmpty() && key !in api.optionalFilters) {
            return TextUtils.joinFilterValues(
                TextUtils.splitFilterValues(defaultValue).filter { it in options }
            )
        }

        return TextUtils.joinFilterValues(selected)
    }

    /**
     * Older versions only supported selecting a single filter option, with "all" meaning that every option
     * is selected. Convert such values to the new, comma separated multi select format.
     */
    private fun migrateLegacyFilters() {
        for (key in api.multiSelectFilters) {
            if (getPref(key, "") != LEGACY_ALL_OPTION) continue

            val options = api.availableFilters[key].orEmpty()
            setPref(key, TextUtils.joinFilterValues(options))
        }
    }

    /**
     * Helper Method for changing a single search filter from [availableFilters].
     */
    fun setFilter(key: String, value: String) {
        selectedFilters = selectedFilters.toMutableMap().also {
            it[key] = value
        }
    }

    companion object {
        private const val LEGACY_ALL_OPTION = "all"
    }
}
