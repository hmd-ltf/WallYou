package net.youapps.wallpaper_apis

/**
 * An abstract class representing a Wallpaper API that supports loading a list of wallpapers via [getWallpapers]
 * and loading the URL of a single random wallpaper using [getRandomWallpaperUrl].
 */
abstract class WallpaperApi {
    /**
     * The name of the wallpaper API.
     */
    abstract val name: String

    /**
     * The base URL of the wallpaper API.
     */
    abstract val baseUrl: String

    /**
     * A map of the supported filter keys to a list of all its possible options.
     */
    open val availableFilters: Map<String, List<String>> = mapOf()

    /**
     * Keys of [availableFilters] that allow selecting multiple options at once.
     *
     * The selected options of such a filter are stored as a comma separated list in [selectedFilters],
     * e.g. `"general,anime"`. An empty value means that no option is selected.
     */
    open val multiSelectFilters: Set<String> = emptySet()

    /**
     * Keys of [multiSelectFilters] that may have no option selected at all, which means that the filter
     * isn't applied. All other multi select filters require at least one selected option.
     */
    open val optionalFilters: Set<String> = emptySet()

    /**
     * The default value for every filter from [availableFilters].
     */
    open val defaultFilters: Map<String, String>
        get() = availableFilters.mapValues { (_, values) -> values.first() }

    /**
     * Whether the filter with the given [key] is relevant for the currently selected [filters],
     * e.g. a time range filter that only applies when sorting by top wallpapers.
     */
    open fun isFilterApplicable(key: String, filters: Map<String, String>): Boolean = true

    /**
     * Popular tags that can be added to the search with a single tap. Only relevant if [supportsTags] is `true`.
     */
    open val suggestedTags: List<String> = emptyList()

    /**
     * Whether tags can be excluded from the search by prefixing them with a `-`, e.g. `-cars`.
     * Sources that can't do this themselves exclude them in [search], see [excludesTagsNatively].
     */
    open val supportsTagExclusion: Boolean = true

    /**
     * How the [selectedTags] are searched, see [TagSearch].
     */
    open val tagSearch: TagSearch = TagSearch.LOCAL

    /**
     * Whether [getWallpapers] already leaves out wallpapers with excluded tags (prefixed with `-`).
     * Otherwise, [selectedTags] only contains included tags when [getWallpapers] is called through
     * [search], see [includedTags].
     */
    open val excludesTagsNatively: Boolean = false

    /**
     * Whether an overridden [getRandomWallpaperUrl] respects the [selectedTags]. Otherwise,
     * [randomWallpaperUrl] picks a random wallpaper from the search results when tags are selected.
     */
    open val randomRespectsTags: Boolean = false

    /**
     * Whether the API supports logging in with a personal API key, e.g. for unlocking NSFW content.
     */
    open val supportsApiKey: Boolean = false

    /**
     * The website on which users can find their API key. Only relevant if [supportsApiKey] is `true`.
     */
    open val apiKeyUrl: String? = null

    /**
     * Whether the API supports logging in with a username and password, see [logIn].
     */
    open val supportsPasswordLogin: Boolean = false

    /**
     * Whether users have to enter the server (instance) of their account when logging in,
     * e.g. for federated platforms. Only relevant if [supportsPasswordLogin] is `true`.
     */
    open val requiresInstanceForLogin: Boolean = false

    /**
     * Whether the user is logged in, either with an [apiKey] or a [loginSession].
     */
    val isLoggedIn: Boolean
        get() = !apiKey.isNullOrBlank() || loginSession != null

    /**
     * Whether the API supports searching by tags, i.e. a search query. Sources without their own
     * search use [TagSearch.LOCAL].
     */
    open val supportsTags: Boolean = true

    /**
     * Whether the API requires a [communityName] to be set in order to work.
     */
    open val requiresCommunityName: Boolean = false

    // These params can be set from outside to configure the API

    private var _selectedFilters: Map<String, String> = mapOf()
    /**
     * The currently selected search filters.
     *
     * This is guaranteed to contain all keys from [availableFilters] and only values from [availableFilters].
     */
    open var selectedFilters: Map<String, String>
        // HACK: prevent NullPointerException when [availableFilters] gets initialized after [selectedFilters]
        // if removing this, initializing a [WallpaperApi] crashes immediately because [selectedFilters]
        // gets initialized before the [availableFilters] attribute of the inheriting wallpaper class is initialized
        set(value) = run {
            _selectedFilters = value
        }
        get() = run {
            _selectedFilters.ifEmpty { defaultFilters }
        }

    /**
     * The tags / query to use for searching. Only relevant if [supportsTags] is `true`.
     * Tags starting with [TagMatcher.EXCLUDE_PREFIX] are excluded.
     */
    open var selectedTags: List<String> = emptyList()

    /**
     * The selected tags that wallpapers have to contain, without excluded ones.
     * Sources with [TagSearch.NATIVE] pass these to their search.
     */
    protected val includedTags: List<String> get() = TagMatcher.includedTags(selectedTags)

    /**
     * The query for searching natively: all [selectedTags] if the source supports excluding tags,
     * [includedTags] otherwise.
     */
    protected val searchQuery: String
        get() = (if (excludesTagsNatively) selectedTags else includedTags).joinToString(" ")

    /**
     * Whether the [wallpaper] contains all included and none of the excluded [selectedTags],
     * judging by its title, description and other info.
     */
    protected fun matchesSelectedTags(wallpaper: Wallpaper): Boolean =
        TagMatcher.matches(wallpaper, includedTags, TagMatcher.excludedTags(selectedTags))

    /**
     * Paginate the given list of [wallpapers] after filtering it by the [selectedTags], for sources
     * that load all of their wallpapers at once.
     */
    protected fun searchInList(wallpapers: List<Wallpaper>, page: Int, pageSize: Int): List<Wallpaper> {
        val matching = wallpapers.filter { matchesSelectedTags(it) }
        val start = (page - 1) * pageSize
        moreResultsAvailable = start + pageSize < matching.size
        if (start >= matching.size) return emptyList()
        return matching.subList(start, minOf(start + pageSize, matching.size))
    }

    /** The page of [getWallpapers] that [search] continues with. */
    private var nextSourcePage = 1

    /**
     * Whether the last call to [search] didn't reach the end of the source, i.e. calling it with the
     * next page might find more wallpapers, even if the last call returned none.
     */
    open val hasMoreResults: Boolean get() = moreResultsAvailable

    /** Set by [search] implementations, see [hasMoreResults]. */
    protected var moreResultsAvailable = true

    /**
     * The name of the community. Only read if [requiresCommunityName] is `true`.
     */
    open var communityName: String? = null

    /**
     * The API key of the logged in user, or `null` if not logged in. Only read if [supportsApiKey] is `true`.
     */
    open var apiKey: String? = null

    /**
     * The session of the logged in user, or `null` if not logged in. Only read if [supportsPasswordLogin] is `true`.
     */
    open var loginSession: LoginSession? = null

    /**
     * Log in with the given [credentials]. The returned session has to be set as [loginSession] afterwards.
     *
     * @throws LoginException if the login failed for a known reason, e.g. invalid credentials
     * @throws Exception if the login failed for another reason, e.g. due to network errors
     */
    open suspend fun logIn(credentials: LoginCredentials): LoginSession =
        throw UnsupportedOperationException("$name doesn't support logging in with a password")

    /**
     * Invalidate the given [session] on the server, if supported. Errors are ignored.
     */
    open suspend fun logOut(session: LoginSession) {}

    /**
     * Check whether the given [apiKey] is valid, e.g. before logging in with it.
     *
     * @return `true` if the API key is valid
     * @throws Exception if the validity couldn't be checked, e.g. due to network errors
     */
    open suspend fun validateApiKey(apiKey: String): Boolean = false

    /**
     * Load a page of wallpapers of this source, respecting [selectedFilters] and, for sources with
     * [TagSearch.NATIVE], [selectedTags]. Use [search] to also filter by tags if the source can't.
     *
     * @param page the page of the search results, starting at 1 (e.g. 1, 2, 3, ...)
     */
    abstract suspend fun getWallpapers(page: Int): List<Wallpaper>

    /**
     * Load a page of wallpapers respecting [selectedTags] and [selectedFilters], filtering the
     * wallpapers in the app if the source can't search for the tags itself.
     *
     * As filtering may leave out all wallpapers of a page of the source, several pages of the source
     * may be loaded at once. An empty list means that there are no more results.
     *
     * @param page the page of the search results, starting at 1 (e.g. 1, 2, 3, ...)
     */
    open suspend fun search(page: Int): List<Wallpaper> {
        val included = if (tagSearch == TagSearch.LOCAL) includedTags else emptyList()
        val excluded = if (excludesTagsNatively) emptyList() else TagMatcher.excludedTags(selectedTags)

        if (included.isEmpty() && excluded.isEmpty()) {
            nextSourcePage = page + 1
            return getWallpapers(page).also { moreResultsAvailable = it.isNotEmpty() }
        }

        if (page <= 1) nextSourcePage = 1
        moreResultsAvailable = true

        val results = mutableListOf<Wallpaper>()
        repeat(MAX_SOURCE_PAGES_PER_SEARCH) {
            val wallpapers = getWallpapers(nextSourcePage)
            nextSourcePage++
            if (wallpapers.isEmpty()) {
                moreResultsAvailable = false
                return results
            }

            results += wallpapers.filter { TagMatcher.matches(it, included, excluded) }
            if (results.size >= MIN_RESULTS_PER_SEARCH) return results
        }
        return results
    }

    /**
     * Get the URL of a random wallpaper, respecting the [selectedTags] whenever possible.
     */
    open suspend fun randomWallpaperUrl(): String? {
        val hasExcludedTags = TagMatcher.excludedTags(selectedTags).isNotEmpty()
        val needsSearch = (includedTags.isNotEmpty() && (tagSearch == TagSearch.LOCAL || !randomRespectsTags)) ||
            (hasExcludedTags && !(excludesTagsNatively && randomRespectsTags))

        if (!needsSearch) return getRandomWallpaperUrl()
        return search(1).randomOrNull()?.imgSrc
    }

    /**
     * Get the URL of a random wallpaper.
     *
     * This does not always respect [selectedFilters] and [selectedTags] due to API restrictions,
     * but it attempts to respect these filters for generating the URL of a random wallpaper whenever possible.
     */
    open suspend fun getRandomWallpaperUrl(): String? = getWallpapers(1).firstOrNull()?.imgSrc

    /**
     * How a wallpaper source searches for [selectedTags].
     */
    enum class TagSearch {
        /** The source searches for the tags itself, e.g. with a search query. */
        NATIVE,

        /** The wallpapers are filtered in the app by their title, description and other info. */
        LOCAL
    }

    companion object {
        /** [search] loads further pages of the source until it found at least this many wallpapers. */
        private const val MIN_RESULTS_PER_SEARCH = 10

        /** How many pages of the source [search] loads at most at once when filtering by tags. */
        private const val MAX_SOURCE_PAGES_PER_SEARCH = 5
    }
}
