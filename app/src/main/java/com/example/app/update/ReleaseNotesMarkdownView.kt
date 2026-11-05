package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.update.AppUpdateInfo
import com.example.update.AppUpdater
import com.example.update.UpdateChannel

// ====================================================================================================
// Small Markdown renderer for the GitHub release notes (headings, bullets, numbered lists, bold, code, links, quotes)
// ====================================================================================================

private val mdComment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)

private val mdImage = Regex("""!\[[^\]]*]\([^)]*\)""")

private val mdHtmlTag = Regex("""</?[A-Za-z][^>]*>""")

private val mdHeading = Regex("""^#{1,6}\s.*$""")

private val mdBullet = Regex("""^(\s*)[-*+]\s+(.*)$""")

private val mdNumbered = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")

private val mdInline = Regex("""(\*\*(.+?)\*\*)|(__(.+?)__)|(`([^`]+)`)|(\[([^\]]+)]\(([^)]+)\))|(\*([^*\s][^*]*?)\*)""")

@Composable
internal fun ReleaseNotesMarkdown(text: String, primary: Color, secondary: Color, accent: Color) {
    val cleaned = remember(text) {
        text.replace("\r", "")
            .replace(mdComment, "")
            .replace(mdImage, "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(mdHtmlTag, "")
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        for (raw in cleaned.lines()) {
            val line = raw.trimEnd()
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> Spacer(modifier = Modifier.height(6.dp))
                mdHeading.matches(trimmed) -> {
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val title = trimmed.drop(level).trim()
                    val size = when (level) { 1 -> 22.sp; 2 -> 18.sp; else -> 15.5.sp }
                    Spacer(modifier = Modifier.height(if (level <= 2) 8.dp else 4.dp))
                    Text(
                        text = inlineMarkdown(title, primary, accent),
                        fontSize = size,
                        lineHeight = size * 1.25f,
                        fontWeight = if (level <= 2) FontWeight.Bold else FontWeight.SemiBold,
                        color = primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                trimmed.matches(Regex("""^([-*_])\1{2,}$""")) -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = primary.copy(alpha = 0.14f))
                    Spacer(modifier = Modifier.height(4.dp))
                }
                mdBullet.matches(line) -> {
                    val m = mdBullet.matchEntire(line)!!
                    MarkdownListRow(
                        marker = "•",
                        indent = m.groupValues[1].length,
                        text = m.groupValues[2],
                        primary = primary,
                        accent = accent
                    )
                }
                mdNumbered.matches(line) -> {
                    val m = mdNumbered.matchEntire(line)!!
                    MarkdownListRow(
                        marker = m.groupValues[2] + ".",
                        indent = m.groupValues[1].length,
                        text = m.groupValues[3],
                        primary = primary,
                        accent = accent
                    )
                }
                trimmed.startsWith(">") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = inlineMarkdown(trimmed.removePrefix(">").trim(), secondary, accent),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontStyle = FontStyle.Italic,
                            color = secondary
                        )
                    }
                }
                else -> Text(
                    text = inlineMarkdown(trimmed, primary, accent),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = primary,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkdownListRow(marker: String, indent: Int, text: String, primary: Color, accent: Color) {
    Row(modifier = Modifier.padding(start = (indent * 6).coerceAtMost(48).dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            text = marker,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = accent,
            modifier = Modifier.width(if (marker.length > 2) 26.dp else 18.dp)
        )
        Text(
            text = inlineMarkdown(text, primary, accent),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = primary,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun inlineMarkdown(text: String, primary: Color, accent: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in mdInline.findAll(text)) {
        if (m.range.first > last) append(text.substring(last, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
            g[3].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[4]) }
            g[5].isNotEmpty() -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = accent.copy(alpha = 0.14f), color = primary)
            ) { append(g[6]) }
            g[7].isNotEmpty() -> withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) { append(g[8]) }
            else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[11]) }
        }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}
