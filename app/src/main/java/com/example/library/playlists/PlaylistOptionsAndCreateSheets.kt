package com.example.ui.components

import com.example.ui.theme.Neutral

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

/** Same translucent "liquid glass" container colour the app's other bottom sheets use. */
@Composable
private fun playlistSheetContainerColor(isDark: Boolean, glassBlurTransparency: Float): Color {
    return glassPanelColor(isDark, glassBlurTransparency)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistBottomSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sliderFactor = (glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = playlistSheetContainerColor(isDark, glassBlurTransparency),
        // the dim behind follows the "UI Glass Blur & Opacity" slider, like the Settings sheet
        scrimColor = Color.Black.copy(alpha = 0.35f * sliderFactor),
        // the handle is drawn inside the glass layer below, so the sheen / specular edge cover the whole sheet
        dragHandle = null,
        // no sheet-level bottom inset: the glass layer runs down to the real screen edge (the navigation-bar strip was
        // left as a separate, cut-off band under the sheet). The content below keeps navigationBarsPadding().
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }
    ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
        LiquidDialogBlur()
        // Liquid Glass ON: frosted sheen + specular edge over the whole sheet (OFF: unchanged look)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (LocalLiquidGlass.current) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
                .liquidGlassEdge(sheetShape, isDark)
        ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(secondaryText.copy(alpha = 0.4f))
            )
        }
        PanelScaled {
        // imePadding keeps the fields and the buttons above the keyboard; the column scrolls if it does not fit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // one glass surface only: the sheet container itself (a second edge-drawn panel inside it made a duplicate,
                // floating-looking sheet with a cut bottom)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp)
        ) {
            content()
        }
    }
        }
}
}

// ----------------------------------------------------------------------------------------------
// 1. Playlist Options: Create empty playlist / Add Playlist from URL
// ----------------------------------------------------------------------------------------------

@Composable
fun PlaylistOptionsSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit,
    onCreateEmpty: () -> Unit,
    onAddFromUrl: () -> Unit,
    onAddArchive: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    PlaylistBottomSheet(isDark, glassBlurTransparency, onDismiss) {
        Text(
            text = "Playlist Options",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        PlaylistOptionRow(
            isDark = isDark,
            title = "Create empty playlist",
            subtitle = "Create a new blank playlist",
            icon = { tint ->
                StyledIcon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onCreateEmpty
        )
        Spacer(modifier = Modifier.height(10.dp))
        PlaylistOptionRow(
            isDark = isDark,
            title = "Add Playlist from URL",
            subtitle = "Import a playlist from a web URL",
            icon = { tint ->
                StyledIcon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onAddFromUrl
        )
        Spacer(modifier = Modifier.height(10.dp))
        PlaylistOptionRow(
            isDark = isDark,
            title = "Add ZIP archive",
            subtitle = "Play the videos inside a ZIP / TAR file or link",
            icon = { tint ->
                StyledIcon(
                    drawableRes = R.drawable.viaura_winrar,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onAddArchive
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PlaylistOptionRow(
    isDark: Boolean,
    title: String,
    subtitle: String,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val rowBg = if (isDark) Color.White.copy(alpha = 0.07f) else Neutral.D0F172A.copy(alpha = 0.06f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(AccentSkyBlue)
        Spacer(modifier = Modifier.width(18.dp))
        Column {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = primaryText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 13.sp, color = secondaryText)
        }
    }
}

// ----------------------------------------------------------------------------------------------
// 2. Create Empty Playlist (name + OK / Cancel)
// ----------------------------------------------------------------------------------------------

@Composable
fun CreateEmptyPlaylistSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    title: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Throwable) {}
    }

    fun submit() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            showError = true
        } else {
            onCreate(trimmed)
        }
    }

    PlaylistBottomSheet(isDark, glassBlurTransparency, onDismiss) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                showError = false
            },
            label = { Text("Playlist name") },
            singleLine = true,
            isError = showError,
            supportingText = if (showError) { { Text("Enter a playlist name") } } else null,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        Spacer(modifier = Modifier.height(18.dp))
        SheetButtonsRow(
            isDark = isDark,
            confirmLabel = "OK",
            confirmEnabled = true,
            onCancel = onDismiss,
            onConfirm = { submit() }
        )
    }
}

// ----------------------------------------------------------------------------------------------
// shared Cancel / confirm row
// ----------------------------------------------------------------------------------------------

@Composable
internal fun SheetButtonsRow(
    isDark: Boolean,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false
) {
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onCancel)
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(text = "Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AccentSkyBlue)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(CircleShape)
                .then(
                    if (confirmEnabled) Modifier.background(AccentGradient)
                    else Modifier.background(secondaryText.copy(alpha = 0.18f))
                )
                .clickable(enabled = confirmEnabled, onClick = onConfirm)
                .padding(horizontal = 34.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = secondaryText
                )
            } else {
                Text(
                    text = confirmLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (confirmEnabled) Color.White else secondaryText.copy(alpha = 0.7f)
                )
            }
        }
    }
}
