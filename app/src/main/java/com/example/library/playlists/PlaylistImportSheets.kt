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

// ----------------------------------------------------------------------------------------------
// 3. Add Playlist from URL / local M3U file
// ----------------------------------------------------------------------------------------------

@Composable
fun AddPlaylistFromUrlSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    isImporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAddFromUrl: (url: String, userAgent: String) -> Unit,
    onChooseLocalFile: () -> Unit,
    onInputChanged: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    var url by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }
    val dividerColor = secondaryText.copy(alpha = 0.35f)

    PlaylistBottomSheet(isDark, glassBlurTransparency, onDismiss) {
        Text(
            text = "Add Playlist",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                onInputChanged()
            },
            label = { Text("Playlist URL") },
            singleLine = true,
            enabled = !isImporting,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Supports M3U/M3U8 and web playlists such as YouTube",
            fontSize = 12.sp,
            color = secondaryText,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 14.dp)
        )
        OutlinedTextField(
            value = userAgent,
            onValueChange = {
                userAgent = it
                onInputChanged()
            },
            label = { Text("Custom User-Agent (optional)") },
            singleLine = true,
            enabled = !isImporting,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (url.isNotBlank() && !isImporting) onAddFromUrl(url, userAgent)
            }),
            modifier = Modifier.fillMaxWidth()
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                fontSize = 12.5.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = dividerColor)
            Text(
                text = "OR",
                fontSize = 12.sp,
                color = secondaryText,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = dividerColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.07f) else Neutral.D0F172A.copy(alpha = 0.06f))
                .clickable(enabled = !isImporting, onClick = onChooseLocalFile)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = primaryText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Choose Local M3U File",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SheetButtonsRow(
            isDark = isDark,
            confirmLabel = "Add from URL",
            confirmEnabled = url.isNotBlank() && !isImporting,
            isLoading = isImporting,
            onCancel = onDismiss,
            onConfirm = { onAddFromUrl(url, userAgent) }
        )
    }
}

// ----------------------------------------------------------------------------------------------
// 4. Add ZIP archive: a direct archive link (+ optional playlist name) / a ZIP or TAR file from the phone
// ----------------------------------------------------------------------------------------------

@Composable
fun AddArchivePlaylistSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    isImporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAddFromLink: (url: String, playlistName: String, userAgent: String) -> Unit,
    onChooseFile: () -> Unit,
    onInputChanged: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    var url by remember { mutableStateOf("") }
    var playlistName by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }
    val dividerColor = secondaryText.copy(alpha = 0.35f)

    PlaylistBottomSheet(isDark, glassBlurTransparency, onDismiss) {
        Text(
            text = "Add ZIP archive",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                onInputChanged()
            },
            label = { Text("Archive link (ZIP / TAR)") },
            singleLine = true,
            enabled = !isImporting,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Direct link to the archive. It is not downloaded: the videos play online from inside it",
            fontSize = 12.sp,
            color = secondaryText,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 14.dp)
        )
        OutlinedTextField(
            value = playlistName,
            onValueChange = {
                playlistName = it
                onInputChanged()
            },
            label = { Text("Playlist name (optional)") },
            singleLine = true,
            enabled = !isImporting,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = userAgent,
            onValueChange = {
                userAgent = it
                onInputChanged()
            },
            label = { Text("Custom User-Agent (optional)") },
            singleLine = true,
            enabled = !isImporting,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (url.isNotBlank() && !isImporting) onAddFromLink(url, playlistName, userAgent)
            }),
            modifier = Modifier.fillMaxWidth()
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                fontSize = 12.5.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = dividerColor)
            Text(
                text = "OR",
                fontSize = 12.sp,
                color = secondaryText,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = dividerColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.07f) else Neutral.D0F172A.copy(alpha = 0.06f))
                .clickable(enabled = !isImporting, onClick = onChooseFile)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIcon(
                drawableRes = R.drawable.viaura_winrar,
                contentDescription = null,
                tint = primaryText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Choose ZIP / TAR File",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
            )
        }
        Text(
            text = "The file stays where it is: nothing is copied or extracted",
            fontSize = 12.sp,
            color = secondaryText,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))
        SheetButtonsRow(
            isDark = isDark,
            confirmLabel = "Add from link",
            confirmEnabled = url.isNotBlank() && !isImporting,
            isLoading = isImporting,
            onCancel = onDismiss,
            onConfirm = { onAddFromLink(url, playlistName, userAgent) }
        )
    }
}
