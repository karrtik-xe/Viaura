package com.example.ai

/** Per-track state shown in the subtitle track sheet while/after AI translation. */
sealed class TrackTranslationStatus {
    data object Idle : TrackTranslationStatus()
    data class Translating(val progressPercent: Int, val etaSeconds: Int? = null) : TrackTranslationStatus()
    data class Completed(val languageLabel: String, val translatedTrackId: Int) : TrackTranslationStatus()
    data class Failed(val message: String) : TrackTranslationStatus()
}

/** World-language list for "Select Translate Language". Code follows BCP-47 where practical. */
object TranslateLanguages {
    data class Entry(val code: String, val label: String)

    val ALL: List<Entry> = listOf(
        Entry("en", "English"),
        Entry("hi", "Hindi (हिन्दी)"),
        Entry("es", "Spanish (Español)"),
        Entry("fr", "French (Français)"),
        Entry("de", "German (Deutsch)"),
        Entry("it", "Italian (Italiano)"),
        Entry("pt", "Portuguese (Português)"),
        Entry("pt-BR", "Portuguese (Brasil)"),
        Entry("ru", "Russian (Русский)"),
        Entry("ja", "Japanese (日本語)"),
        Entry("ko", "Korean (한국어)"),
        Entry("zh", "Chinese (简体中文)"),
        Entry("zh-TW", "Chinese (繁體中文)"),
        Entry("ar", "Arabic (العربية)"),
        Entry("bn", "Bengali (বাংলা)"),
        Entry("ur", "Urdu (اردو)"),
        Entry("mr", "Marathi (मराठी)"),
        Entry("te", "Telugu (తెలుగు)"),
        Entry("ta", "Tamil (தமிழ்)"),
        Entry("gu", "Gujarati (ગુજરાતી)"),
        Entry("kn", "Kannada (ಕನ್ನಡ)"),
        Entry("ml", "Malayalam (മലയാളം)"),
        Entry("pa", "Punjabi (ਪੰਜਾਬੀ)"),
        Entry("ne", "Nepali (नेपाली)"),
        Entry("tr", "Turkish (Türkçe)"),
        Entry("vi", "Vietnamese (Tiếng Việt)"),
        Entry("th", "Thai (ไทย)"),
        Entry("id", "Indonesian (Bahasa Indonesia)"),
        Entry("ms", "Malay (Bahasa Melayu)"),
        Entry("fa", "Persian (فارسی)"),
        Entry("pl", "Polish (Polski)"),
        Entry("nl", "Dutch (Nederlands)"),
        Entry("sv", "Swedish (Svenska)"),
        Entry("no", "Norwegian (Norsk)"),
        Entry("da", "Danish (Dansk)"),
        Entry("fi", "Finnish (Suomi)"),
        Entry("el", "Greek (Ελληνικά)"),
        Entry("he", "Hebrew (עברית)"),
        Entry("uk", "Ukrainian (Українська)"),
        Entry("cs", "Czech (Čeština)"),
        Entry("sk", "Slovak (Slovenčina)"),
        Entry("ro", "Romanian (Română)"),
        Entry("hu", "Hungarian (Magyar)"),
        Entry("bg", "Bulgarian (Български)"),
        Entry("hr", "Croatian (Hrvatski)"),
        Entry("sr", "Serbian (Српски)"),
        Entry("sl", "Slovenian (Slovenščina)"),
        Entry("lt", "Lithuanian (Lietuvių)"),
        Entry("lv", "Latvian (Latviešu)"),
        Entry("et", "Estonian (Eesti)"),
        Entry("si", "Sinhala (සිංහල)"),
        Entry("my", "Burmese (မြန်မာ)"),
        Entry("km", "Khmer (ភាសាខ្មែរ)"),
        Entry("lo", "Lao (ພາສາລາວ)"),
        Entry("sw", "Swahili (Kiswahili)"),
        Entry("am", "Amharic (አማርኛ)"),
        Entry("af", "Afrikaans"),
        Entry("sq", "Albanian (Shqip)"),
        Entry("az", "Azerbaijani (Azərbaycan)"),
        Entry("ka", "Georgian (ქართული)"),
        Entry("hy", "Armenian (Հայերեն)"),
        Entry("kk", "Kazakh (Қазақша)"),
        Entry("uz", "Uzbek (Oʻzbek)"),
        Entry("mn", "Mongolian (Монгол)"),
        Entry("tl", "Filipino (Tagalog)"),
        Entry("is", "Icelandic (Íslenska)"),
        Entry("mt", "Maltese (Malti)"),
        Entry("cy", "Welsh (Cymraeg)"),
        Entry("ga", "Irish (Gaeilge)"),
        Entry("eu", "Basque (Euskara)"),
        Entry("ca", "Catalan (Català)"),
        Entry("gl", "Galician (Galego)")
    )
}
