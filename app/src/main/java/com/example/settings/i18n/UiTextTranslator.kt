package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import com.example.ui.state.AppLanguage
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/** Language the UI is currently shown in. Provided once at the top of the app (VideoPlayerTheme). */
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

/**
 * Text translations for the Settings screens. The texts live in assets/lang/ (one JSON file per language, key = the original
 * English text of the app, value = the translation), so they are NOT part of any Kotlin file. A text that has no entry
 * is shown unchanged.
 *
 * Keys that contain Kotlin templates (for example "Balance: ${'$'}{b.totalBalance}") are matched as patterns: the values of the
 * placeholders are copied into the translated sentence.
 */
object I18n {

    private class Table(
        val exact: Map<String, String>,
        val templates: List<Template>,
        val cache: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    )

    private class Template(val pattern: Pattern, val tokens: List<String>, val translation: String)

    private val tables = ConcurrentHashMap<AppLanguage, Table>()
    @Volatile private var appContext: Context? = null

    private val tokenRegex = Regex("""\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*""")

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun fileFor(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "lang/lang_english.json"
        AppLanguage.HINGLISH -> "lang/lang_hinglish.json"
        AppLanguage.HINDI -> "lang/lang_hindi_devanagari.json"
    }

    private fun load(lang: AppLanguage): Table {
        val exact = HashMap<String, String>()
        val templates = ArrayList<Template>()
        try {
            val ctx = appContext
            if (ctx != null) {
                val json = ctx.assets.open(fileFor(lang)).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = obj.optString(key, "")
                    if (value.isEmpty() || value == key) continue
                    exact[key] = value
                    val tokens = tokenRegex.findAll(key).map { it.value }.toList()
                    if (tokens.isNotEmpty()) {
                        buildTemplate(key, value, tokens)?.let { templates.add(it) }
                    }
                }
            }
        } catch (_: Throwable) {
            // missing / broken language file: texts simply stay as they are
        }
        return Table(exact, templates)
    }

    private fun buildTemplate(key: String, value: String, tokens: List<String>): Template? {
        val literal = tokenRegex.replace(key, "")
        if (!Regex("[A-Za-z]{2}").containsMatchIn(literal)) return null
        val sb = StringBuilder("^")
        var last = 0
        for (m in tokenRegex.findAll(key)) {
            if (m.range.first > last) sb.append(Pattern.quote(key.substring(last, m.range.first)))
            sb.append("(.*?)")
            last = m.range.last + 1
        }
        if (last < key.length) sb.append(Pattern.quote(key.substring(last)))
        sb.append("$")
        return try {
            Template(Pattern.compile(sb.toString(), Pattern.DOTALL), tokens, value)
        } catch (_: Throwable) {
            null
        }
    }

    /** Translation of [text] for [lang]; the text itself when there is none. */
    fun t(lang: AppLanguage, text: String): String {
        if (text.isEmpty()) return text
        val table = tables.getOrPut(lang) { load(lang) }
        table.exact[text]?.let { return it }
        if (table.templates.isEmpty() || text.length < 3) return text
        table.cache[text]?.let { return it }
        var result = text
        for (tpl in table.templates) {
            val m = tpl.pattern.matcher(text)
            if (m.matches()) {
                val values = HashMap<String, String>()
                for (i in tpl.tokens.indices) values.putIfAbsent(tpl.tokens[i], m.group(i + 1) ?: "")
                result = tokenRegex.replace(tpl.translation) { values[it.value] ?: it.value }
                break
            }
        }
        if (table.cache.size < 3000) table.cache[text] = result
        return result
    }
}
