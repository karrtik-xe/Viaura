package com.example.ai

/**
 * Cloud AI providers selectable from Settings > Advanced > AI Features > Provider.
 * `Default` lets the user paste any OpenAI-compatible base URL/key without
 * Viaura assuming a specific vendor.
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    /** OpenAI-compatible base URL used to build /models and /chat/completions calls. */
    val defaultBaseUrl: String
) {
    DEFAULT("default", "Default", ""),
    GOOGLE("google", "Google", "https://generativelanguage.googleapis.com/v1beta/openai"),
    OPENCODE("opencode", "OpenCode", "https://opencode.ai/zen/v1"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1"),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("anthropic", "Anthropic", "https://api.anthropic.com/v1"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1"),
    TOGETHER("together", "Together", "https://api.together.xyz/v1");

    companion object {
        fun fromId(id: String?): AiProvider = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** A model returned by a provider's /models discovery call, or picked by hand. */
data class AiModelInfo(
    val id: String,
    val label: String = id
)

/**
 * Picks a model automatically out of a provider's discovered list, so the user isn't forced to
 * hand-pick one. "Best" here means "most likely to actually work for translation" rather than
 * "most capable": a free-tier/flash/mini-class model that responds is strictly better than a
 * flagship model the account can't afford, since an unaffordable model just fails every request
 * with HTTP 402 (Payment Required). Order matters: the first pattern that matches any model wins.
 */
object BestModelSelector {
    private val preferenceOrder: List<Regex> = listOf(
        Regex(""":free$""", RegexOption.IGNORE_CASE),   // OpenRouter free-tier variants, e.g. "...:free"
        Regex("""flash""", RegexOption.IGNORE_CASE),     // Gemini flash: fast, cheap, generous free quota
        Regex("""8b|9b|small|lite|mini""", RegexOption.IGNORE_CASE),
        Regex("""haiku""", RegexOption.IGNORE_CASE),
        Regex("""gpt-4o(?!-)|gpt-4o$""", RegexOption.IGNORE_CASE),
        Regex("""sonnet""", RegexOption.IGNORE_CASE),
        Regex("""pro""", RegexOption.IGNORE_CASE)
    )

    fun pickBest(models: List<AiModelInfo>): AiModelInfo? {
        if (models.isEmpty()) return null
        for (pattern in preferenceOrder) {
            models.firstOrNull { pattern.containsMatchIn(it.id) }?.let { return it }
        }
        return models.first()
    }
}

/** Result of tapping "Verify Key". */
sealed class ApiKeyVerificationState {
    data object Idle : ApiKeyVerificationState()
    data object Verifying : ApiKeyVerificationState()
    data class Verified(val models: List<AiModelInfo>) : ApiKeyVerificationState()
    data class Failed(val message: String) : ApiKeyVerificationState()
}
