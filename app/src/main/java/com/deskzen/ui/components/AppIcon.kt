package com.deskzen.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.deskzen.ui.theme.DeskZenDimens

enum class AppBadge { SHORTCUT, AI_SUGGESTED, NONE }

@Composable
fun AppIcon(
    icon: Drawable?,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = DeskZenDimens.appIconLarge,
    badge: AppBadge = AppBadge.NONE,
    notificationCount: Int = 0
) {
    Box(modifier = modifier) {
        if (icon != null) {
            val bitmap = remember(icon) {
                icon.toBitmap().asImageBitmap()
            }
            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = label,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = label,
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Notification badge (top-end)
        if (notificationCount > 0) {
            val countText = if (notificationCount > 99) "99+" else notificationCount.toString()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .widthIn(min = 18.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        // Existing badge system (bottom-end)
        if (badge != AppBadge.NONE) {
            val badgeIcon = when (badge) {
                AppBadge.SHORTCUT -> Icons.Default.Check
                AppBadge.AI_SUGGESTED -> Icons.Default.AutoAwesome
                AppBadge.NONE -> null
            }
            val badgeColor = when (badge) {
                AppBadge.SHORTCUT -> MaterialTheme.colorScheme.primary
                AppBadge.AI_SUGGESTED -> MaterialTheme.colorScheme.tertiary
                AppBadge.NONE -> MaterialTheme.colorScheme.surface
            }

            badgeIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(badgeColor, CircleShape)
                        .padding(2.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
