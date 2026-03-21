package com.deskzen.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.deskzen.domain.model.AppInfo
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.theme.DeskZenDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    appInfo: AppInfo,
    onCreateShortcut: () -> Unit,
    onRemoveShortcut: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DeskZenDimens.spacingMd)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    icon = appInfo.icon,
                    label = appInfo.label,
                    size = DeskZenDimens.appIconMedium
                )
                Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                Column {
                    Text(
                        text = appInfo.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingLg))

            // Actions
            if (appInfo.isOnHomeScreen) {
                TextButton(
                    onClick = { onRemoveShortcut(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                    Text("Retirer de l'écran d'accueil")
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                TextButton(
                    onClick = { onCreateShortcut(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                    Text("Ajouter à l'écran d'accueil")
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            TextButton(
                onClick = { onOpenApp(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                Text("Ouvrir l'application")
                Spacer(modifier = Modifier.weight(1f))
            }

            TextButton(
                onClick = { onOpenAppSettings(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                Text("Informations de l'app")
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingMd))
        }
    }
}
