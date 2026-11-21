package net.youapps.wallpaper_apis

object TextUtils {
    private val markdownRegex = Regex("[*_~`#]")

    fun removeMarkdownSymbols(text: String) = text.replace(markdownRegex, "")

    /**
     * Split the value of a multi select filter (see [WallpaperApi.multiSelectFilters]) into its options.
     */
    fun splitFilterValues(value: String?): List<String> =
        value.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Join the options of a multi select filter (see [WallpaperApi.multiSelectFilters]) into a single value.
     */
    fun joinFilterValues(values: Collection<String>): String = values.joinToString(",")
}
