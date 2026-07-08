package uno.lux.sample.data.settings

/**
 * The languages the app ships translations for. There is deliberately no "follow the system" entry:
 * with only two languages the device's language is often one we don't ship, which would leave such
 * a choice meaning "English" without saying so. Instead the system language seeds the choice once,
 * on first launch — see [AppLocaleRepository.resolveInitialLanguage].
 */
enum class AppLanguage(val languageTag: String) {
    ENGLISH("en"),
    CZECH("cs");

    companion object {
        /** The language for a device that prefers nothing we ship. */
        val Default: AppLanguage = ENGLISH

        /**
         * The first language we ship out of a comma-separated BCP 47 tag list, in the list's own
         * order of preference — so a device set to `"de-DE,cs-CZ,en"` resolves to [CZECH], the
         * best of its preferences that we can actually render. Region subtags are ignored, and
         * `null` means the list named nothing we ship (or was empty).
         */
        fun fromLanguageTags(tags: String?): AppLanguage? =
            tags?.split(',')
                ?.map { it.substringBefore('-').trim().lowercase() }
                ?.firstNotNullOfOrNull { tag -> entries.firstOrNull { it.languageTag == tag } }
    }
}
