package com.deskzen.ui.launcher

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
import com.deskzen.ui.theme.DeskZenDimens
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloGlow
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloSurface
import com.deskzen.ui.theme.SoloTextMuted

@Composable
fun MoveToFolderDialog(
    packageName: String,
    appLabel: String,
    folders: List<String>,
    onMoveToFolder: (String) -> Unit,
    onAddToHomeScreen: () -> Unit,
    onOpenInfo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(appLabel, color = SoloGlow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // Add to home screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddToHomeScreen() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Home, null, tint = SoloElectricBlue)
                    Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                    Text("Raccourci sur l'écran", color = Color.White)
                }

                // Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInfo() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = SoloTextMuted)
                    Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                    Text("Informations", color = Color.White)
                }

                HorizontalDivider(
                    color = SoloPurple.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    "Déplacer vers...",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoloPurple,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.height(250.dp)
                ) {
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
