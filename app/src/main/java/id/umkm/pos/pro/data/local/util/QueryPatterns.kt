package id.umkm.pos.pro.data.local.util

internal fun String?.toContainsLikePattern(): String {
    val keyword = this?.trim().orEmpty()
    if (keyword.isEmpty()) return LIKE_ANY
    val escaped = keyword
        .replace(ESCAPE_CHARACTER, ESCAPE_CHARACTER + ESCAPE_CHARACTER)
        .replace(PERCENT_WILDCARD, ESCAPE_CHARACTER + PERCENT_WILDCARD)
        .replace(UNDERSCORE_WILDCARD, ESCAPE_CHARACTER + UNDERSCORE_WILDCARD)
    return "%$escaped%"
}

private const val LIKE_ANY = "%"
private const val ESCAPE_CHARACTER = "\\"
private const val PERCENT_WILDCARD = "%"
private const val UNDERSCORE_WILDCARD = "_"
