package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.lyrics.LyricsProvider
import com.example.domain.lyrics.LyricsSourceType
import com.example.ui.theme.AccentSkyBlue

@Composable
fun LyricsProviderPickerDialog(
    visible: Boolean,
    activeProvider: LyricsProvider?,
    sourceType: LyricsSourceType,
    hasLocalLyrics: Boolean,
    providers: List<LyricsProvider>,
    onDismiss: () -> Unit,
    onChooseLocal: () -> Unit,
    onChooseProvider: (LyricsProvider?) -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics source", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                    if (hasLocalLyrics) {
                        item(key = "local") {
                            PickerRow(
                                title = "Local / embedded lyrics",
                                detail = "Sidecar, embedded tags, or imported lyrics",
                                selected = sourceType != LyricsSourceType.ONLINE,
                                onClick = onChooseLocal,
                            )
                        }
                    }
                    item(key = "auto") {
                        PickerRow(
                            title = "Automatic best match",
                            detail = "Race all providers; prefer word timing, then line timing",
                            selected = false,
                            onClick = { onChooseProvider(null) },
                        )
                    }
                    items(providers, key = { it.name }) { provider ->
                        PickerRow(
                            title = provider.label,
                            detail = provider.detail,
                            selected = activeProvider == provider && sourceType == LyricsSourceType.ONLINE,
                            onClick = { onChooseProvider(provider) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun PickerRow(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentSkyBlue.copy(alpha = 0.13f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) AccentSkyBlue else Color.Unspecified)
            Text(detail, fontSize = 11.sp, lineHeight = 14.sp, color = Color.Gray)
        }
        if (selected) Text("✓", color = AccentSkyBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}
