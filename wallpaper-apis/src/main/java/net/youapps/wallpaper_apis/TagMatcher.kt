package net.youapps.wallpaper_apis

/**
 * Matches tags against the text of wallpapers (title, description, ...), for wallpaper sources
 * without their own search, see [WallpaperApi.tagSearch].
 */
object TagMatcher {
    /** Tags starting with this are excluded from the search, e.g. `-cars`. */
    const val EXCLUDE_PREFIX = "-"

    private val nonWordRegex = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Lowercase the [text] and replace everything except letters and digits with single spaces,
     * so that e.g. `Forest_Path-2024.jpg` becomes ` forest path 2024 jpg `.
     */
    fun normalize(text: String): String =
        " " + text.lowercase().replace(nonWordRegex, " ").trim() + " "

    fun isExcluded(tag: String) = tag.startsWith(EXCLUDE_PREFIX)

    fun includedTags(tags: List<String>) = tags.filterNot { isExcluded(it) }.filter { it.isNotBlank() }

    fun excludedTags(tags: List<String>) =
        tags.filter { isExcluded(it) }.map { it.removePrefix(EXCLUDE_PREFIX) }.filter { it.isNotBlank() }

    /**
     * Whether the [normalizedText] (see [normalize]) contains the [tag]. Words of the tag have to
     * appear in this order at the start of words in the text, so that `mountain` also finds
     * `Mountains`, but `cat` doesn't find `education`.
     */
    fun containsTag(normalizedText: String, tag: String): Boolean {
        val normalizedTag = normalize(tag).trimEnd()
        if (normalizedTag.isBlank()) return true
        return normalizedText.contains(normalizedTag)
    }

    /**
     * The text of the [wallpaper] that tags are matched against.
     */
    fun searchableText(wallpaper: Wallpaper): String = normalize(
        listOfNotNull(
            wallpaper.title,
            wallpaper.description,
            wallpaper.category,
            wallpaper.author,
            wallpaper.tags.joinToString(" "),
            // the file name or link often describe the image, e.g. /photos/forest-path-123/
            wallpaper.url,
            wallpaper.imgSrc.substringAfterLast("/")
        ).joinToString(" ")
    )

    /**
     * Whether the [wallpaper] contains all [included] tags and none of the [excluded] tags.
     */
    fun matches(wallpaper: Wallpaper, included: List<String>, excluded: List<String>): Boolean {
        if (included.isEmpty() && excluded.isEmpty()) return true

        val text = searchableText(wallpaper)
        return included.all { containsTag(text, it) } && excluded.none { containsTag(text, it) }
    }
}
