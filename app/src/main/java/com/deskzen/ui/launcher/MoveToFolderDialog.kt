package com.deskzen.ui.launcher

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deskzen.ui.theme.DeskZenDimens
import com.deskzen.ui.theme.SoloCyan
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloGlow
import com.deskzen.ui.theme.SoloGold
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloSurface
import com.deskzen.ui.theme.SoloTextMuted

@Composable
fun MoveToFolderDialog(
    packageName: String,
    appLabel: String,
    folders: List<String>,
    shortcuts: List<ShortcutInfo> = emptyList(),
    isLocked: Boolean = false,
    onMoveToFolder: (String) -> Unit,
    onAddToHomeScreen: () -> Unit,
    onOpenInfo: () -> Unit,
    onLaunchShortcut: (ShortcutInfo) -> Unit = {},
    onToggleLock: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(appLabel, color = SoloGlow, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isLocked) {
                    Text("🔒", fontSize = 14.sp)
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.height(380.dp)) {
                // App shortcuts
                if (shortcuts.isNotEmpty()) {
                    item {
                        Text(
                            "Raccourcis",
                            style = MaterialTheme.typography.labelLarge,
                            color = SoloCyan,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(shortcuts) { shortcut ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLaunchShortcut(shortcut)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.OpenInNew, null, tint = SoloCyan, modifier = Modifier.width(20.dp))
                            Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                            Text(
                                text = shortcut.shortLabel?.toString() ?: shortcut.id,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    item {
                        HorizontalDivider(color = SoloPurple.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Actions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToHomeScreen(); onDismiss() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, null, tint = SoloElectricBlue)
                        Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                        Text("Raccourci sur l'écran", color = Color.White)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenInfo(); onDismiss() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = SoloTextMuted)
                        Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                        Text("Informations", color = Color.White)
                    }
                }

                // Lock/unlock
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleLock(); onDismiss() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            null,
                            tint = SoloGold
                        )
                        Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                        Text(
                            if (isLocked) "Déverrouiller (l'IA peut déplacer)" else "Verrouiller ici (l'IA ne touchera plus)",
                            color = SoloGold
                        )
                    }
                }

                item {
                    HorizontalDivider(color = SoloPurple.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Déplacer vers...",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoloPurple,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(folders) { folderName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMoveToFolder(folderName) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = folderName,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SoloTextMuted)
            }
        }
    )
}
