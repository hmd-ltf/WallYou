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
     * Only relevant if [supportsTags] is `true`.
     */
    open val supportsTagExclusion: Boolean = false

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
     * Whether the API supports searching by tags, i.e. a search query.
     */
    open val supportsTags: Boolean = false

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
     */
    open var selectedTags: List<String> = emptyList()

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
     * Load a list of wallpapers by respecting. [selectedTags] and [selectedFilters].
     *
     * @param page the page of the search results, starting at 1 (e.g. 1, 2, 3, ...)
     */
    abstract suspend fun getWallpapers(page: Int): List<Wallpaper>

    /**
     * Get the URL of a random wallpaper.
     *
     * This does not always respect [selectedFilters] and [selectedTags] due to API restrictions,
     * but it attempts to respect these filters for generating the URL of a random wallpaper whenever possible.
     */
    open suspend fun getRandomWallpaperUrl(): String? = getWallpapers(1).firstOrNull()?.imgSrc
}
