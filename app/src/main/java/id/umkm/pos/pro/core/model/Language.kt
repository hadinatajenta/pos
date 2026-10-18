package id.umkm.pos.pro.core.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    INDONESIAN(code = "id", displayName = "Indonesian", nativeName = "Bahasa Indonesia"),
    ENGLISH(code = "en", displayName = "English", nativeName = "English"),
    CHINESE(code = "zh", displayName = "Chinese", nativeName = "中文 (简体)");

    companion object {
        val DEFAULT: AppLanguage = INDONESIAN

        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
    }
}
