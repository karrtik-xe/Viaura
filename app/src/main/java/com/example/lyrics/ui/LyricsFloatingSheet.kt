package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

/**
 * One floating glass sheet for the lyrics pickers (Translate lyrics, Lyrics source): the same island card as the
 * "Add to Playlist" sheet (single piece, no split at the bottom). Liquid Glass OFF = normal panel colour + thin border,
 * Liquid Glass ON = frosted liquid panel (glassPanelColor / glassCardBorder handle both).
 */
@Composable
fun LyricsFloatingSheet(
    title: String,
    subtitle: String? = null,
    isDark: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetBg = glassPanelColor(isDark, LocalPanelTransparency.current)
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    // Same bottom sheet as the playlist "Create Playlist" options: it slides up from the bottom edge (no centred / floating card).
    PlaylistBottomSheet(
        isDark = isDark,
        glassBlurTransparency = LocalPanelTransparency.current,
        onDismiss = onDismiss
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                StyledIcon(imageVector = Icons.Outlined.Close, contentDescription = "Close", tint = primaryText, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, fontSize = 11.5.sp, color = secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // The sheet body scrolls, so the lists inside need a fixed height of their own (their weight(1f) fills it).
        val listHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp
        Column(modifier = Modifier.fillMaxWidth().height(listHeight)) {
            content()
        }
    }
}
