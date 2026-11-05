package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AudioFolderItem
import com.example.ui.screens.VideoFolder
import com.example.ui.screens.VideoFolderScanner
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.util.AudioLibraryCache
import com.example.util.FolderBlockListManager
import com.example.util.FolderDateUtils
import com.example.util.PrivateFolder
import com.example.util.PrivateMediaType
import com.example.util.PrivateVaultManager
import com.example.util.VideoLibraryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FolderMetadataCard(
    isDark: Boolean,
    name: String,
    countText: String,
    sizeText: String,
    createdDateText: String,
    isCustom: Boolean,
    glyphType: FolderGlyphType,
    statusBadge: String?,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isSelectable: Boolean = true,
    selectionTint: Color = AccentSkyBlue,
    onCardClick: (() -> Unit)? = null,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier
) {
    // Generous touch target with 48dp+ interactive area across whole card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) selectionTint else borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                enabled = onCardClick != null && isSelectable,
                onClick = { onCardClick?.invoke() }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernFolderIcon(
                isDark = isDark,
                size = 46.dp,
                isCustom = isCustom,
                glyphType = glyphType,
                showBadge = false
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!isSelectable) secondaryText.copy(alpha = 0.5f) else primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (statusBadge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (statusBadge == "Blocked") Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else AccentMid.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (statusBadge == "Blocked") Color(0xFFEF4444) else AccentMid
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                // EXACT 3 METADATA ITEMS: COUNT • SIZE • CREATED DATE
                Text(
                    text = "$countText • $sizeText • $createdDateText",
                    fontSize = 11.5.sp,
                    color = if (!isSelectable) secondaryText.copy(alpha = 0.5f) else secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // SELECTION CIRCLE ON THE RIGHT END OF FOLDER ITEM WITH 44dp MIN HITBOX
            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isSelectable) {
                        StyledIcon(imageVector = Icons.Default.Lock,
                            contentDescription = "Already in Vault",
                            tint = secondaryText.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        StyledIcon(imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) selectionTint else secondaryText.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedFolderRowItem(
    isDark: Boolean,
    name: String,
    typeLabel: String,
    isAudio: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onUnblock: () -> Unit
) {
    // Generous touch area across the row + 44dp touch area for unblock action
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.5f) else Neutral.F1F5F9)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ModernFolderIcon(
                isDark = isDark,
                size = 34.dp,
                glyphType = if (isAudio) FolderGlyphType.AUDIO else FolderGlyphType.VIDEO,
                showBadge = false
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = typeLabel,
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }
        }
        // Easy-to-tap 44dp button
        IconButton(
            onClick = onUnblock,
            modifier = Modifier.size(44.dp)
        ) {
            StyledIcon(imageVector = Icons.Default.Close,
                contentDescription = "Unblock",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PrivateFolderItemCard(
    isDark: Boolean,
    folder: PrivateFolder,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onRestore: () -> Unit
) {
    // Generous touch area across the card + 44dp restore button
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.6f) else Neutral.F1F5F9)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                ModernFolderIcon(
                    isDark = isDark,
                    size = 40.dp,
                    glyphType = if (folder.mediaType == PrivateMediaType.AUDIO) FolderGlyphType.AUDIO else FolderGlyphType.VIDEO,
                    showBadge = false
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = folder.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.itemCount} ${if (folder.mediaType == PrivateMediaType.VIDEO) "Videos" else "Tracks"} • ${folder.formattedSize} • Created: ${folder.formattedDate}",
                        fontSize = 11.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Easy-to-tap 44dp restore button
            IconButton(
                onClick = onRestore,
                modifier = Modifier.size(44.dp)
            ) {
                StyledIcon(imageVector = Icons.Outlined.Restore,
                    contentDescription = "Restore to public storage",
                    tint = AccentSkyBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
