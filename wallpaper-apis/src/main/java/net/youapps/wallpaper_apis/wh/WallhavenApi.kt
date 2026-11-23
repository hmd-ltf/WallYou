package net.youapps.wallpaper_apis.wh

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.TextUtils
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

class WallhavenApi : WallpaperApi() {
    override val name: String = "Wallhaven"
    override val baseUrl: String = "https://wallhaven.cc/api/v1/"

    override val availableFilters: Map<String, List<String>>
        get() = mapOf(
            CATEGORIES to CATEGORY_OPTIONS,
            // NSFW wallpapers can only be loaded when logged in with an API key
            PURITY to if (apiKey.isNullOrBlank()) PURITY_OPTIONS - NSFW else PURITY_OPTIONS,
            SORTING to SORTING_OPTIONS,
            TOPLIST_RANGE to TOPLIST_RANGE_OPTIONS,
            ORDER to ORDER_OPTIONS,
            RATIOS to RATIO_OPTIONS,
            RESOLUTIONS to RESOLUTION_OPTIONS,
            MIN_RESOLUTION to listOf(ANY) + MIN_RESOLUTION_OPTIONS,
            COLORS to listOf(ANY) + COLOR_OPTIONS
        )

    override val multiSelectFilters: Set<String> = setOf(CATEGORIES, PURITY, RATIOS, RESOLUTIONS)
    override val optionalFilters: Set<String> = setOf(RATIOS, RESOLUTIONS)

    override val defaultFilters: Map<String, String>
        get() = mapOf(
            CATEGORIES to "general",
            PURITY to "sfw",
            SORTING to "date_added",
            TOPLIST_RANGE to "1M",
            ORDER to "desc",
            RATIOS to "portrait",
            RESOLUTIONS to "",
            MIN_RESOLUTION to ANY,
            COLORS to ANY
        )

    override fun isFilterApplicable(key: String, filters: Map<String, String>): Boolean {
        return when (key) {
            TOPLIST_RANGE -> filters[SORTING] == "toplist"
            else -> true
        }
    }

    override val tagSearch = TagSearch.NATIVE
    override val excludesTagsNatively = true
    override val randomRespectsTags = true
    override val suggestedTags: List<String> = listOf(
        "nature",
        "landscape",
        "mountains",
        "space",
        "city",
        "minimalism",
        "digital art",
        "abstract",
        "anime",
        "cars",
        "forest",
        "sunset",
        "night",
        "water",
        "architecture",
        "fantasy art",
        "artwork",
        "video games",
        "pixel art",
        "cyberpunk"
    )

    override val supportsApiKey: Boolean = true
    override val apiKeyUrl: String = "https://wallhaven.cc/settings/account"

    private val api = RetrofitHelper.create<Wallhaven>(baseUrl)

    /**
     * The seed of the current random search, used to prevent duplicates when loading further pages.
     */
    private var randomSeed: String? = null

    override suspend fun validateApiKey(apiKey: String): Boolean {
        val response = api.getUserSettings(apiKey.trim())

        return when {
            response.isSuccessful -> true
            response.code() == 401 -> false
            else -> throw Exception("HTTP ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        val filters = selectedFilters
        val apiKey = apiKey?.takeIf { it.isNotBlank() }

        val categories = getMultiFilter(filters, CATEGORIES)
        val purity = getMultiFilter(filters, PURITY)
            .filter { it != NSFW || apiKey != null }
            .ifEmpty { listOf("sfw") }
        val sorting = filters[SORTING]?.takeIf { it in SORTING_OPTIONS } ?: "date_added"

        if (page <= 1 || sorting != "random") randomSeed = null

        val response = api.search(
            page = page,
            query = searchQuery,
            categories = toBitmask(CATEGORY_OPTIONS, categories),
            purity = toBitmask(PURITY_OPTIONS, purity),
            sorting = sorting,
            order = filters[ORDER]?.takeIf { it in ORDER_OPTIONS } ?: "desc",
            topRange = filters[TOPLIST_RANGE]
                ?.takeIf { sorting == "toplist" && it in TOPLIST_RANGE_OPTIONS },
            ratios = getMultiFilter(filters, RATIOS).joinToString(",").ifEmpty { null },
            resolutions = getMultiFilter(filters, RESOLUTIONS).joinToString(",").ifEmpty { null },
            atLeast = filters[MIN_RESOLUTION]?.takeIf { it in MIN_RESOLUTION_OPTIONS },
            colors = filters[COLORS]?.takeIf { it in COLOR_OPTIONS },
            seed = randomSeed,
            apiKey = apiKey
        )

        if (sorting == "random") randomSeed = response.meta?.seed ?: randomSeed

        return response.data?.map {
            Wallpaper(
                imgSrc = it.path!!,
                title = it.category,
                url = it.url,
                category = it.category,
                fileSize = it.file_size,
                resolution = it.resolution,
                thumb = it.thumbs?.original,
                creationDate = it.created_at
            )
        }.orEmpty()
    }

    override suspend fun getRandomWallpaperUrl(): String? {
        return getWallpapers(1).randomOrNull()?.imgSrc
    }

    /**
     * Get the valid selected options of a multi select filter.
     *
     * Falls back to the default options if a required filter has no valid option selected.
     */
    private fun getMultiFilter(filters: Map<String, String>, key: String): List<String> {
        val options = when (key) {
            CATEGORIES -> CATEGORY_OPTIONS
            PURITY -> PURITY_OPTIONS
            RATIOS -> RATIO_OPTIONS
            RESOLUTIONS -> RESOLUTION_OPTIONS
            else -> emptyList()
        }

        val selected = TextUtils.splitFilterValues(filters[key]).filter { it in options }
        if (selected.isNotEmpty() || key in optionalFilters) return selected

        return TextUtils.splitFilterValues(defaultFilters[key])
    }

    /**
     * Convert the selected options to Wallhaven's bitmask format, e.g. `110` for `general` and `anime`.
     */
    private fun toBitmask(options: List<String>, selected: List<String>): String {
        return options.joinToString("") { if (it in selected) "1" else "0" }
    }

    companion object {
        private const val CATEGORIES = "categories"
        private const val PURITY = "purity"
        private const val SORTING = "sorting"
        private const val TOPLIST_RANGE = "toplist_range"
        private const val ORDER = "order"
        private const val RATIOS = "ratios"
        private const val RESOLUTIONS = "resolutions"
        private const val MIN_RESOLUTION = "min_resolution"
        private const val COLORS = "colors"

        private const val ANY = "any"
        private const val NSFW = "nsfw"

        // the order of these options matters, as it's used for generating the bitmask
        private val CATEGORY_OPTIONS = listOf("general", "anime", "people")
        private val PURITY_OPTIONS = listOf("sfw", "sketchy", NSFW)

        private val SORTING_OPTIONS =
            listOf("date_added", "relevance", "random", "views", "favorites", "toplist", "hot")
        private val TOPLIST_RANGE_OPTIONS = listOf("1d", "3d", "1w", "1M", "3M", "6M", "1y")
        private val ORDER_OPTIONS = listOf("desc", "asc")

        private val RATIO_OPTIONS = listOf(
            "portrait", "landscape",
            "9x16", "10x16", "9x18",
            "1x1", "3x2", "4x3", "5x4",
            "16x9", "16x10", "21x9", "32x9", "48x9"
        )

        private val RESOLUTION_OPTIONS = listOf(
            // portrait, i.e. phones and tablets
            "720x1280", "1080x1920", "1080x2160", "1080x2340", "1080x2400",
            "1440x2560", "1440x3040", "1440x3200", "2160x3840",
            // landscape, i.e. desktops and tablets
            "1280x720", "1920x1080", "1920x1200", "2560x1080", "2560x1440",
            "2560x1600", "3440x1440", "3840x1600", "3840x2160"
        )

        private val MIN_RESOLUTION_OPTIONS = listOf(
            "1080x1920", "1440x2560", "2160x3840",
            "1280x720", "1920x1080", "2560x1440", "3840x2160"
        )

        private val COLOR_OPTIONS = listOf(
            "660000", "990000", "cc0000", "cc3333", "ea4c88", "993399", "663399", "333399",
            "0066cc", "0099cc", "66cccc", "77cc33", "669900", "336600", "666600", "999900",
            "cccc33", "ffff00", "ffcc33", "ff9900", "ff6600", "cc6633", "996633", "663300",
            "000000", "999999", "cccccc", "ffffff", "424153"
        )
    }
}
