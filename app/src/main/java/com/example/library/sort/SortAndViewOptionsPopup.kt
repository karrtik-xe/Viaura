package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import com.example.R

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortAndViewOptionsPopup(
    isDark: Boolean,
    sortField: SortField,
    sortDirection: SortDirection,
    viewMode: ViewMode,
    layoutMode: LayoutMode,
    visibleFields: VisibleFields,
    isInsideFolder: Boolean = false,
    isMusicMode: Boolean = false,
    isRecentsMode: Boolean = false,
    isPlaylistMode: Boolean = false,
    isOnlineMode: Boolean = false,
    isOnlinePlaylist: Boolean = false,
    isOnlineAudioFolder: Boolean = false,
    isOnlineVideoFolder: Boolean = false,
    isDownloadsMode: Boolean = false,
    appScale: Float = 75f,
    glassBlurTransparency: Float = 85f,
    onSortFieldChange: (SortField) -> Unit,
    onSortDirectionChange: (SortDirection) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onVisibleFieldsChange: (VisibleFields) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val accentBlue = if (isDark) AccentSkyBlue else AccentSkyBlueStrong
    val activeTintBg = if (isDark) AccentSkyBlueStrong.copy(alpha = 0.28f) else AccentContainerLight.copy(alpha = 0.85f)
    val pillBorderColor = if (isDark) AccentSkyBlue.copy(alpha = 0.70f) else AccentSkyBlueStrong.copy(alpha = 0.65f)
    val dividerColor = if (isDark) AccentSkyBlue.copy(alpha = 0.25f) else AccentSkyBlue.copy(alpha = 0.30f)
    val alphaRatio = (glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)
    val dialogSurface = glassPanelColor(isDark, glassBlurTransparency)
    val dialogBorderBrush = if (isDark) LiquidGlassDefaults.DarkGlassBorderBrush else LiquidGlassDefaults.LightGlassBorderBrush

    // Calculate dynamic scaled density so the entire Sort & View Options UI scales with appScale
    val currentDensity = LocalDensity.current
    val currentAppScale = appScale.coerceIn(1f, 100f)
    val scaleFactor = (0.85f + 0.15f * ((currentAppScale - 1f) / 99f)).coerceIn(0.85f, 1.00f)
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, scaleFactor) {
        Density(
            density = currentDensity.density * scaleFactor,
            fontScale = currentDensity.fontScale * scaleFactor
        )
    }

    var isFieldsExpanded by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            val view = LocalView.current
            DisposableEffect(view) {
                configureEdgeToEdgeDialogWindow(
                    view = view,
                    isLightStatusBars = !isDark,
                    isLightNavBars = !isDark
                )
                onDispose {}
            }

            // Clean transparent backdrop with touch dismissal (no dark background UI)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
            // Liquid Glass Dialog Card
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .then(
                        if (LocalLiquidGlass.current) {
                            Modifier.glassOuterShadow(RoundedCornerShape(28.dp), 20.dp, Color.Black.copy(alpha = 0.35f), 4.dp)
                        } else {
                            Modifier.shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = Color.Black.copy(alpha = 0.30f),
                                spotColor = Color.Black.copy(alpha = 0.40f)
                            )
                        }
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(dialogSurface)
                    .border(1.2.dp, dialogBorderBrush, RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title Header
                    Text(
                        text = "Sort & View Options",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        color = dividerColor,
                        thickness = 1.dp
                    )

                    // 1. SECTION: SORT BY
                    Text(
                        text = "Sort by",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sort cards adapt to the current level:
                    // - Online Audio Folder: ONLY Date and Title (Duration and Size removed)
                    // - Count: Folder views only (Home Video root, Home Audio root, Online Audio root)
                    // - Duration: Media items inside folders & playlists (positioned directly after Title)
                    // - Size: Available in all views except Online Playlists (Sections 11 & 12)
                    val isOnlineMediaFolder = isOnlineAudioFolder || isOnlineVideoFolder
                    val showCountSort = !isInsideFolder && !isPlaylistMode && !isRecentsMode && !isOnlineMediaFolder
                    val showDurationSort = (isInsideFolder || isPlaylistMode) && !isOnlineMediaFolder
                    val showSizeSort = !isOnlinePlaylist && !isOnlineMediaFolder && !isRecentsMode

                    LaunchedEffect(isOnlineMediaFolder, showDurationSort, showCountSort, showSizeSort) {
                        if (isOnlineMediaFolder) {
                            if (sortField != SortField.DATE && sortField != SortField.TITLE) {
                                onSortFieldChange(SortField.DATE)
                            }
                        } else {
                            if (!showDurationSort && sortField == SortField.DURATION) {
                                onSortFieldChange(SortField.TITLE)
                            }
                            if (!showCountSort && sortField == SortField.COUNT) {
                                onSortFieldChange(SortField.TITLE)
                            }
                            if (!showSizeSort && sortField == SortField.SIZE) {
                                onSortFieldChange(SortField.TITLE)
                            }
                        }
                    }

                    if (isOnlineMediaFolder) {
                        // Online Audio folder: Keep ONLY 2 options: "Date" and "Title"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SortTypeCard(
                                label = "Date", icon = R.drawable.viaura_calendar,
                                isSelected = sortField == SortField.DATE, primaryText = primaryText,
                                secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                isDark = isDark, modifier = Modifier.weight(1f),
                                onClick = { onSortFieldChange(SortField.DATE) }
                            )
                            SortTypeCard(
                                label = "Title", icon = R.drawable.viaura_text,
                                isSelected = sortField == SortField.TITLE, primaryText = primaryText,
                                secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                isDark = isDark, modifier = Modifier.weight(1f),
                                onClick = { onSortFieldChange(SortField.TITLE) }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SortTypeCard(
                                label = "Title", icon = R.drawable.viaura_text,
                                isSelected = sortField == SortField.TITLE, primaryText = primaryText,
                                secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                isDark = isDark, modifier = Modifier.weight(1f),
                                onClick = { onSortFieldChange(SortField.TITLE) }
                            )
                            if (showDurationSort) {
                                SortTypeCard(
                                    label = "Duration", icon = R.drawable.viaura_clock_circle,
                                    isSelected = sortField == SortField.DURATION, primaryText = primaryText,
                                    secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                    isDark = isDark, modifier = Modifier.weight(1f),
                                    onClick = { onSortFieldChange(SortField.DURATION) }
                                )
                            }
                            SortTypeCard(
                                label = "Date", icon = R.drawable.viaura_calendar,
                                isSelected = sortField == SortField.DATE, primaryText = primaryText,
                                secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                isDark = isDark, modifier = Modifier.weight(1f),
                                onClick = { onSortFieldChange(SortField.DATE) }
                            )
                            if (showSizeSort) {
                                SortTypeCard(
                                    label = "Size", icon = R.drawable.viaura_size,
                                    isSelected = sortField == SortField.SIZE, primaryText = primaryText,
                                    secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                    isDark = isDark, modifier = Modifier.weight(1f),
                                    onClick = { onSortFieldChange(SortField.SIZE) }
                                )
                            }
                            if (showCountSort) {
                                SortTypeCard(
                                    label = "Count", icon = R.drawable.viaura_count,
                                    isSelected = sortField == SortField.COUNT, primaryText = primaryText,
                                    secondaryText = secondaryText, accentBlue = accentBlue, activeBg = activeTintBg,
                                    isDark = isDark, modifier = Modifier.weight(1f),
                                    onClick = { onSortFieldChange(SortField.COUNT) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2-Option Direction Pill Toggle (e.g. Smallest / Largest or A-Z / Z-A)
                    val (leftLabel, rightLabel, isLeftSelected) = when (sortField) {
                        SortField.TITLE -> Triple("A-Z", "Z-A", sortDirection == SortDirection.ASCENDING)
                        SortField.DURATION -> Triple("Longest", "Shortest", sortDirection == SortDirection.DESCENDING)
                        SortField.DATE -> Triple("Newest", "Oldest", sortDirection == SortDirection.DESCENDING)
                        SortField.SIZE -> Triple("Smallest", "Largest", sortDirection == SortDirection.ASCENDING)
                        SortField.COUNT -> Triple("Fewest", "Most", sortDirection == SortDirection.ASCENDING)
                    }

                    SortDirectionPillToggle(
                        leftLabel = leftLabel,
                        rightLabel = rightLabel,
                        isLeftSelected = isLeftSelected,
                        accentBlue = accentBlue,
                        activeBg = activeTintBg,
                        borderColor = pillBorderColor,
                        primaryText = primaryText,
                        onLeftClick = {
                            when (sortField) {
                                SortField.TITLE -> onSortDirectionChange(SortDirection.ASCENDING)
                                SortField.DURATION -> onSortDirectionChange(SortDirection.DESCENDING)
                                SortField.DATE -> onSortDirectionChange(SortDirection.DESCENDING)
                                SortField.SIZE -> onSortDirectionChange(SortDirection.ASCENDING)
                                SortField.COUNT -> onSortDirectionChange(SortDirection.ASCENDING)
                            }
                        },
                        onRightClick = {
                            when (sortField) {
                                SortField.TITLE -> onSortDirectionChange(SortDirection.DESCENDING)
                                SortField.DURATION -> onSortDirectionChange(SortDirection.ASCENDING)
                                SortField.DATE -> onSortDirectionChange(SortDirection.ASCENDING)
                                SortField.SIZE -> onSortDirectionChange(SortDirection.DESCENDING)
                                SortField.COUNT -> onSortDirectionChange(SortDirection.DESCENDING)
                            }
                        }
                    )

                    if (!isOnlineAudioFolder && !isOnlineVideoFolder && !isRecentsMode) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                            color = dividerColor,
                            thickness = 1.dp
                        )

                        // 2. SECTION: VIEW MODE
                        Text(
                            text = "View Mode",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ViewModePillToggle(
                            selectedMode = viewMode,
                            accentBlue = accentBlue,
                            activeBg = activeTintBg,
                            borderColor = pillBorderColor,
                            primaryText = primaryText,
                            onSelectMode = onViewModeChange
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        color = dividerColor,
                        thickness = 1.dp
                    )

                    // 3. SECTION: LAYOUT
                    Text(
                        text = "Layout",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LayoutPillToggle(
                        selectedLayout = layoutMode,
                        accentBlue = accentBlue,
                        activeBg = activeTintBg,
                        borderColor = pillBorderColor,
                        primaryText = primaryText,
                        onSelectLayout = onLayoutModeChange
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        color = dividerColor,
                        thickness = 1.dp
                    )

                    // 4. SECTION: FIELDS (Expandable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isFieldsExpanded = !isFieldsExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fields",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        StyledIcon(
                                imageVector = if (isFieldsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isFieldsExpanded) "Collapse Fields" else "Expand Fields",
                            tint = primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isFieldsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isOnlineVideoFolder) {
                                // ONLINE VIDEO FOLDER: Thumbnails, Duration, Date, Progress Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FieldChip(
                                        label = "Thumbnails",
                                        isActive = visibleFields.showThumbnails,
                                        modifier = Modifier.weight(1.15f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                    )
                                    FieldChip(
                                        label = "Duration",
                                        isActive = visibleFields.showDuration,
                                        modifier = Modifier.weight(1.0f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FieldChip(
                                        label = "Date",
                                        isActive = visibleFields.showDate,
                                        modifier = Modifier.weight(0.9f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                    )
                                    FieldChip(
                                        label = "Progress Bar",
                                        isActive = visibleFields.showProgressBar,
                                        modifier = Modifier.weight(1.35f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                    )
                                }
                                if (isDownloadsMode) {
                                    // Downloads: video subtitle indicator + audio lyrics indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Subtitle Indicator",
                                            isActive = visibleFields.showSubtitleIndicator,
                                            modifier = Modifier.weight(1.55f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showSubtitleIndicator = !visibleFields.showSubtitleIndicator)) }
                                        )
                                        FieldChip(
                                            label = "Lyrics",
                                            isActive = visibleFields.showLyrics,
                                            modifier = Modifier.weight(1.0f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showLyrics = !visibleFields.showLyrics)) }
                                        )
                                    }
                                }
                            } else if (isOnlineAudioFolder) {
                                // ONLINE AUDIO FOLDER:
                                // Keep ONLY: Thumbnails, Duration, Date, Progress Bar
                                // COMPLETELY REMOVE: Size, Path, Subtitle Indicator, Framerate, Resolution, Video Count, New Badge, Full Name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FieldChip(
                                        label = "Thumbnails",
                                        isActive = visibleFields.showThumbnails,
                                        modifier = Modifier.weight(1.15f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                    )
                                    FieldChip(
                                        label = "Duration",
                                        isActive = visibleFields.showDuration,
                                        modifier = Modifier.weight(1.0f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FieldChip(
                                        label = "Date",
                                        isActive = visibleFields.showDate,
                                        modifier = Modifier.weight(0.9f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                    )
                                    FieldChip(
                                        label = "Progress Bar",
                                        isActive = visibleFields.showProgressBar,
                                        modifier = Modifier.weight(1.35f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                    )
                                }
                            } else if (isRecentsMode) {
                                // RECENT HISTORY (video & audio side): Thumbnails, Duration, Date, Progress Bar, Path
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FieldChip(
                                        label = "Thumbnails",
                                        isActive = visibleFields.showThumbnails,
                                        modifier = Modifier.weight(1.15f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                    )
                                    FieldChip(
                                        label = "Duration",
                                        isActive = visibleFields.showDuration,
                                        modifier = Modifier.weight(1.0f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                    )
                                    FieldChip(
                                        label = "Date",
                                        isActive = visibleFields.showDate,
                                        modifier = Modifier.weight(0.9f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FieldChip(
                                        label = "Progress Bar",
                                        isActive = visibleFields.showProgressBar,
                                        modifier = Modifier.weight(1.35f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                    )
                                    FieldChip(
                                        label = "Path",
                                        isActive = visibleFields.showPath,
                                        modifier = Modifier.weight(0.9f),
                                        onClick = { onVisibleFieldsChange(visibleFields.copy(showPath = !visibleFields.showPath)) }
                                    )
                                }
                            } else if (isOnlinePlaylist) {
                                // SECTIONS 11 & 12: ONLINE PLAYLISTS
                                if (!isInsideFolder) {
                                    if (isMusicMode) {
                                        // 12. ONLINE PLAYLIST (AUDIO) - Root / Folder View
                                        // Fields: Total Media, Total Duration, Date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Total Media",
                                                isActive = visibleFields.showVideoCount,
                                                modifier = Modifier.weight(1.1f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showVideoCount = !visibleFields.showVideoCount)) }
                                            )
                                            FieldChip(
                                                label = "Total Duration",
                                                isActive = visibleFields.showDuration,
                                                modifier = Modifier.weight(1.25f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                            )
                                            FieldChip(
                                                label = "Date",
                                                isActive = visibleFields.showDate,
                                                modifier = Modifier.weight(0.85f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                            )
                                        }
                                    } else {
                                        // 11. ONLINE PLAYLIST (VIDEO) - Root / Folder View
                                        // Fields: Total Media, Total Duration, Date, Resolution
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Total Media",
                                                isActive = visibleFields.showVideoCount,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showVideoCount = !visibleFields.showVideoCount)) }
                                            )
                                            FieldChip(
                                                label = "Total Duration",
                                                isActive = visibleFields.showDuration,
                                                modifier = Modifier.weight(1.2f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Date",
                                                isActive = visibleFields.showDate,
                                                modifier = Modifier.weight(0.85f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                            )
                                            FieldChip(
                                                label = "Resolution",
                                                isActive = visibleFields.showResolution,
                                                modifier = Modifier.weight(1.15f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showResolution = !visibleFields.showResolution)) }
                                            )
                                        }
                                    }
                                } else {
                                    if (isMusicMode) {
                                        // 12. ONLINE PLAYLIST (AUDIO) - Inside Playlist (Audio Files)
                                        // Fields: Thumbnails, Duration, Extension (lyrics), Date, Audio Type, Progress Bar, Path
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Thumbnails",
                                                isActive = visibleFields.showThumbnails,
                                                modifier = Modifier.weight(1.25f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                            )
                                            FieldChip(
                                                label = "Duration",
                                                isActive = visibleFields.showDuration,
                                                modifier = Modifier.weight(1.05f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                            )
                                            FieldChip(
                                                label = "Extension",
                                                isActive = visibleFields.showExtension,
                                                modifier = Modifier.weight(1.05f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showExtension = !visibleFields.showExtension)) }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Date",
                                                isActive = visibleFields.showDate,
                                                modifier = Modifier.weight(0.9f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                            )
                                            FieldChip(
                                                label = "Audio Type",
                                                isActive = visibleFields.showAudioType,
                                                modifier = Modifier.weight(1.15f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showAudioType = !visibleFields.showAudioType)) }
                                            )
                                            FieldChip(
                                                label = "Progress Bar",
                                                isActive = visibleFields.showProgressBar,
                                                modifier = Modifier.weight(1.3f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Path",
                                                isActive = visibleFields.showPath,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showPath = !visibleFields.showPath)) }
                                            )
                                        }
                                    } else {
                                        // 11. ONLINE PLAYLIST (VIDEO) - Inside Playlist (Video Files)
                                        // Fields: Thumbnails, Duration, Framerate, Date, Resolution, Progress Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Thumbnails",
                                                isActive = visibleFields.showThumbnails,
                                                modifier = Modifier.weight(1.25f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                            )
                                            FieldChip(
                                                label = "Duration",
                                                isActive = visibleFields.showDuration,
                                                modifier = Modifier.weight(1.05f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                            )
                                            FieldChip(
                                                label = "Framerate",
                                                isActive = visibleFields.showFramerate,
                                                modifier = Modifier.weight(1.05f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showFramerate = !visibleFields.showFramerate)) }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FieldChip(
                                                label = "Date",
                                                isActive = visibleFields.showDate,
                                                modifier = Modifier.weight(0.85f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                            )
                                            FieldChip(
                                                label = "Resolution",
                                                isActive = visibleFields.showResolution,
                                                modifier = Modifier.weight(1.15f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showResolution = !visibleFields.showResolution)) }
                                            )
                                            FieldChip(
                                                label = "Progress Bar",
                                                isActive = visibleFields.showProgressBar,
                                                modifier = Modifier.weight(1.35f),
                                                onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                            )
                                        }
                                    }
                                }
                            } else if (!isInsideFolder) {
                                // ROOT / FOLDER VIEWS:
                                // Section 1 (Home Video), Section 2 (Home Audio), Section 5 Root (Playlist Video),
                                // Section 6 Root (Playlist Audio), Section 8 (Online Audio), Recents Root
                                if (isMusicMode) {
                                    // SECTIONS 2, 6 (Root), 8: AUDIO FOLDERS / ROOT
                                    // Fields: Path, Total Media, Total Duration, Folder Size, Date
                                    // HATAO: Full Name, Resolution, New Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Path",
                                            isActive = visibleFields.showPath,
                                            modifier = Modifier.weight(0.85f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showPath = !visibleFields.showPath)) }
                                        )
                                        FieldChip(
                                            label = "Total Media",
                                            isActive = visibleFields.showVideoCount,
                                            modifier = Modifier.weight(1.25f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showVideoCount = !visibleFields.showVideoCount)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Total Duration",
                                            isActive = visibleFields.showDuration,
                                            modifier = Modifier.weight(1.35f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                        )
                                        FieldChip(
                                            label = "Folder Size",
                                            isActive = visibleFields.showSize,
                                            modifier = Modifier.weight(1.15f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showSize = !visibleFields.showSize)) }
                                        )
                                        FieldChip(
                                            label = "Date",
                                            isActive = visibleFields.showDate,
                                            modifier = Modifier.weight(0.8f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                        )
                                    }
                                } else {
                                    // SECTIONS 1, 5 (Root), 7: VIDEO FOLDERS / ROOT (and Recents Root)
                                    // Fields: Path, Total Media, Total Duration, Folder Size, Date, Resolution
                                    // HATAO: Full Name, New Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Path",
                                            isActive = visibleFields.showPath,
                                            modifier = Modifier.weight(0.85f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showPath = !visibleFields.showPath)) }
                                        )
                                        FieldChip(
                                            label = "Total Media",
                                            isActive = visibleFields.showVideoCount,
                                            modifier = Modifier.weight(1.25f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showVideoCount = !visibleFields.showVideoCount)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Total Duration",
                                            isActive = visibleFields.showDuration,
                                            modifier = Modifier.weight(1.35f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                        )
                                        FieldChip(
                                            label = "Folder Size",
                                            isActive = visibleFields.showSize,
                                            modifier = Modifier.weight(1.15f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showSize = !visibleFields.showSize)) }
                                        )
                                        FieldChip(
                                            label = "Date",
                                            isActive = visibleFields.showDate,
                                            modifier = Modifier.weight(0.8f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Resolution",
                                            isActive = visibleFields.showResolution,
                                            modifier = Modifier.weight(1.2f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showResolution = !visibleFields.showResolution)) }
                                        )
                                    }
                                }
                            } else {
                                // INSIDE FOLDER / ITEM VIEWS:
                                // Section 3 (Video Inside), Section 4 (Audio Inside), Section 5 Inside (Playlist Video),
                                // Section 6 Inside (Playlist Audio), Section 10 (Online Audio Inside), Recents Items
                                if (isMusicMode) {
                                    // SECTIONS 4, 6 (Inside), 10: AUDIO FILES
                                    // Fields: Thumbnails, Duration, Lyrics, Date, Size, Audio Type (stereo etc), Progress Bar, Path
                                    // HATAO: Full Name
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Thumbnails",
                                            isActive = visibleFields.showThumbnails,
                                            modifier = Modifier.weight(1.25f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                        )
                                        FieldChip(
                                            label = "Duration",
                                            isActive = visibleFields.showDuration,
                                            modifier = Modifier.weight(1.05f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                        )
                                        FieldChip(
                                            label = "Lyrics",
                                            isActive = visibleFields.showLyrics,
                                            modifier = Modifier.weight(1.05f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showLyrics = !visibleFields.showLyrics)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Date",
                                            isActive = visibleFields.showDate,
                                            modifier = Modifier.weight(0.9f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                        )
                                        FieldChip(
                                            label = "Size",
                                            isActive = visibleFields.showSize,
                                            modifier = Modifier.weight(0.9f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showSize = !visibleFields.showSize)) }
                                        )
                                        FieldChip(
                                            label = "Audio Type",
                                            isActive = visibleFields.showAudioType,
                                            modifier = Modifier.weight(1.15f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showAudioType = !visibleFields.showAudioType)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Progress Bar",
                                            isActive = visibleFields.showProgressBar,
                                            modifier = Modifier.weight(1.3f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                        )
                                        FieldChip(
                                            label = "Path",
                                            isActive = visibleFields.showPath,
                                            modifier = Modifier.weight(0.9f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showPath = !visibleFields.showPath)) }
                                        )
                                        FieldChip(
                                            label = "Extension",
                                            isActive = visibleFields.showAudioExtension,
                                            modifier = Modifier.weight(1.1f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showAudioExtension = !visibleFields.showAudioExtension)) }
                                        )
                                    }
                                } else {
                                    // SECTIONS 3, 5 (Inside), 9: VIDEO FILES (and Recents Items)
                                    // Fields: Thumbnails, Duration, Extension, Subtitle Indicator, Framerate, Date, Size, Resolution, Progress Bar, Path
                                    // HATAO: Full Name
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Thumbnails",
                                            isActive = visibleFields.showThumbnails,
                                            modifier = Modifier.weight(1.25f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showThumbnails = !visibleFields.showThumbnails)) }
                                        )
                                        FieldChip(
                                            label = "Duration",
                                            isActive = visibleFields.showDuration,
                                            modifier = Modifier.weight(1.05f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDuration = !visibleFields.showDuration)) }
                                        )
                                        FieldChip(
                                            label = "Extension",
                                            isActive = visibleFields.showExtension,
                                            modifier = Modifier.weight(1.05f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showExtension = !visibleFields.showExtension)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Subtitle Indicator",
                                            isActive = visibleFields.showSubtitleIndicator,
                                            modifier = Modifier.weight(1.55f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showSubtitleIndicator = !visibleFields.showSubtitleIndicator)) }
                                        )
                                        FieldChip(
                                            label = "Framerate",
                                            isActive = visibleFields.showFramerate,
                                            modifier = Modifier.weight(1.1f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showFramerate = !visibleFields.showFramerate)) }
                                        )
                                        FieldChip(
                                            label = "Date",
                                            isActive = visibleFields.showDate,
                                            modifier = Modifier.weight(0.8f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showDate = !visibleFields.showDate)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Size",
                                            isActive = visibleFields.showSize,
                                            modifier = Modifier.weight(0.9f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showSize = !visibleFields.showSize)) }
                                        )
                                        FieldChip(
                                            label = "Resolution",
                                            isActive = visibleFields.showResolution,
                                            modifier = Modifier.weight(1.15f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showResolution = !visibleFields.showResolution)) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FieldChip(
                                            label = "Progress Bar",
                                            isActive = visibleFields.showProgressBar,
                                            modifier = Modifier.weight(1.4f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showProgressBar = !visibleFields.showProgressBar)) }
                                        )
                                        FieldChip(
                                            label = "Path",
                                            isActive = visibleFields.showPath,
                                            modifier = Modifier.weight(0.9f),
                                            onClick = { onVisibleFieldsChange(visibleFields.copy(showPath = !visibleFields.showPath)) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Done Button placed cleanly on the bottom right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("sort_done_button")
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGradient)
                                .clickable { onDismiss() }
                                .padding(horizontal = 18.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Done",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
}
