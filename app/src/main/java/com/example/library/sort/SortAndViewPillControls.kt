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

/**
 * Top Card for Sort Field (Title, Date, Size)
 */
@Composable
internal fun SortTypeCard(
    label: String,
    icon: Int,
    isSelected: Boolean,
    primaryText: Color,
    secondaryText: Color,
    accentBlue: Color,
    activeBg: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val inactiveBg = if (isDark) Neutral.D1E293B.copy(alpha = 0.40f) else Neutral.F1F5F9.copy(alpha = 0.70f)
    val cardBorder = if (isSelected) Color(0x40FFFFFF) else if (isDark) Neutral.D334155.copy(alpha = 0.4f) else Neutral.E2E8F0

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) Modifier.background(AccentGradient)
                else Modifier.background(inactiveBg)
            )
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StyledIcon(
                                drawableRes = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else primaryText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else primaryText
            )
        }
    }
}

/**
 * 2-Option Direction Pill (e.g. ^ Smallest | v Largest)
 */
@Composable
internal fun SortDirectionPillToggle(
    leftLabel: String,
    rightLabel: String,
    isLeftSelected: Boolean,
    accentBlue: Color,
    activeBg: Color,
    borderColor: Color,
    primaryText: Color,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .border(1.2.dp, borderColor, shape)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left option
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (isLeftSelected) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable(onClick = onLeftClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StyledIcon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (isLeftSelected) Color.White else primaryText,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = leftLabel,
                    fontSize = 13.sp,
                    fontWeight = if (isLeftSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isLeftSelected) Color.White else primaryText
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = borderColor,
            thickness = 1.2.dp
        )

        // Right option
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (!isLeftSelected) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable(onClick = onRightClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StyledIcon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (!isLeftSelected) Color.White else primaryText,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rightLabel,
                    fontSize = 13.sp,
                    fontWeight = if (!isLeftSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (!isLeftSelected) Color.White else primaryText
                )
            }
        }
    }
}

/**
 * 3-Option View Mode Pill (Folder, Tree, Library)
 */
@Composable
internal fun ViewModePillToggle(
    selectedMode: ViewMode,
    accentBlue: Color,
    activeBg: Color,
    borderColor: Color,
    primaryText: Color,
    onSelectMode: (ViewMode) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .border(1.2.dp, borderColor, shape)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Folder
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (selectedMode == ViewMode.FOLDER) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable { onSelectMode(ViewMode.FOLDER) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedMode == ViewMode.FOLDER) {
                    StyledIcon(
                                imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Folder",
                    fontSize = 13.sp,
                    fontWeight = if (selectedMode == ViewMode.FOLDER) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selectedMode == ViewMode.FOLDER) Color.White else primaryText
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = borderColor,
            thickness = 1.2.dp
        )

        // Tree
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (selectedMode == ViewMode.TREE) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable { onSelectMode(ViewMode.TREE) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedMode == ViewMode.TREE) {
                    StyledIcon(
                                imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Tree",
                    fontSize = 13.sp,
                    fontWeight = if (selectedMode == ViewMode.TREE) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selectedMode == ViewMode.TREE) Color.White else primaryText
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = borderColor,
            thickness = 1.2.dp
        )

        // Library
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (selectedMode == ViewMode.LIBRARY) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable { onSelectMode(ViewMode.LIBRARY) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedMode == ViewMode.LIBRARY) {
                    StyledIcon(
                                imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Library",
                    fontSize = 13.sp,
                    fontWeight = if (selectedMode == ViewMode.LIBRARY) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selectedMode == ViewMode.LIBRARY) Color.White else primaryText
                )
            }
        }
    }
}

/**
 * 2-Option Layout Pill (List | Grid)
 */
@Composable
internal fun LayoutPillToggle(
    selectedLayout: LayoutMode,
    accentBlue: Color,
    activeBg: Color,
    borderColor: Color,
    primaryText: Color,
    onSelectLayout: (LayoutMode) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .border(1.2.dp, borderColor, shape)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (selectedLayout == LayoutMode.LIST) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable { onSelectLayout(LayoutMode.LIST) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StyledIcon(
                                imageVector = Icons.Outlined.ViewList,
                    contentDescription = "List",
                    tint = if (selectedLayout == LayoutMode.LIST) Color.White else primaryText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "List",
                    fontSize = 13.sp,
                    fontWeight = if (selectedLayout == LayoutMode.LIST) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selectedLayout == LayoutMode.LIST) Color.White else primaryText
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = borderColor,
            thickness = 1.2.dp
        )

        // Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (selectedLayout == LayoutMode.GRID) Modifier.background(AccentGradient)
                    else Modifier.background(Color.Transparent)
                )
                .clickable { onSelectLayout(LayoutMode.GRID) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StyledIcon(
                                imageVector = Icons.Outlined.GridView,
                    contentDescription = "Grid",
                    tint = if (selectedLayout == LayoutMode.GRID) Color.White else primaryText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Grid",
                    fontSize = 13.sp,
                    fontWeight = if (selectedLayout == LayoutMode.GRID) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selectedLayout == LayoutMode.GRID) Color.White else primaryText
                )
            }
        }
    }
}

/**
 * Metadata Field Chip matching screenshot design with AccentGradient
 */
@Composable
internal fun FieldChip(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .then(
                if (isActive) {
                    Modifier
                        .background(AccentGradient)
                        .border(1.dp, Color(0x40FFFFFF), shape)
                } else {
                    Modifier
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.40f))
                        .border(1.dp, colorScheme.outline.copy(alpha = 0.50f), shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color.White else colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
