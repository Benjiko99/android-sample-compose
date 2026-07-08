package uno.lux.sample.data.settings

/**
 * The languages the app ships translations for. [SYSTEM] carries no tag: it means "follow the
 * device's language list", which the per-app language APIs express as an empty locale list.
 */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    CZECH("cs");

    companion object {
        /**
         * Parses a comma-separated BCP 47 language-tag list — what the per-app language APIs hand
         * back, e.g. `"cs-CZ,en"` — to the language the app is running in. Only the first tag's
         * language subtag matters; a list that is empty, absent, or names a language we don't
         * ship resolves to [SYSTEM].
         */
        fun fromLanguageTags(tags: String?): AppLanguage {
            val language = tags?.substringBefore(',')?.substringBefore('-')?.lowercase()

            return entries.firstOrNull { it.languageTag == language } ?: SYSTEM
        }
    }
}
