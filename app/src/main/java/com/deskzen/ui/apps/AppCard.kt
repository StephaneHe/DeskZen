package com.deskzen.ui.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.deskzen.domain.model.AppInfo
import com.deskzen.ui.components.AppBadge
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.theme.DeskZenDimens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = DeskZenDimens.cardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(DeskZenDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                icon = appInfo.icon,
                label = appInfo.label,
                size = DeskZenDimens.appIconLarge,
                badge = if (appInfo.isOnHomeScreen) AppBadge.SHORTCUT else AppBadge.NONE
            )
            Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                appInfo.category?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                appInfo.versionName?.let {
                    Text(
                        text = "v$it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
