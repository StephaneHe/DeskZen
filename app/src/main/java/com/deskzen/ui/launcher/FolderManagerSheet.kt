package com.deskzen.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deskzen.ui.theme.DeskZenDimens
import com.deskzen.ui.theme.SoloDeepBlack
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloError
import com.deskzen.ui.theme.SoloGlow
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloSurface
import com.deskzen.ui.theme.SoloTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerSheet(
    folders: List<Pair<String, Int>>,
    onAddFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onReDispatch: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SoloSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DeskZenDimens.spacingMd)
        ) {
            // Title
            Text(
                text = "Gestion des dossiers",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = SoloGlow,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${folders.size} dossiers — ${folders.sumOf { it.second }} apps classées",
                style = MaterialTheme.typography.bodySmall,
                color = SoloTextMuted
            )

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingMd))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SoloElectricBlue
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
                    Text("Ajouter")
                }

                Button(
                    onClick = onReDispatch,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoloPurple
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.padding(end = 4.dp))
                    Text("IA Dispatch")
                }
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))

            // Backup/Restore
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SoloTextMuted
                    )
                ) {
                    Text("Sauvegarder", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onImportBackup,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SoloTextMuted
                    )
                ) {
                    Text("Restaurer", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingMd))

            // Folder list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(folders) { (name, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                SoloDeepBlack.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$count apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoloPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { onRemoveFolder(name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = SoloError.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingMd))
        }
    }

    if (showAddDialog) {
        AddFolderDialog(
            onConfirm = { name ->
                onAddFolder(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun AddFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        title = {
            Text("Nouveau dossier", color = SoloGlow)
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                placeholder = { Text("Nom du dossier", color = SoloTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoloElectricBlue,
                    unfocusedBorderColor = SoloPurple.copy(alpha = 0.3f),
                    cursorColor = SoloElectricBlue,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) { Text("Créer", color = SoloElectricBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SoloTextMuted) }
        }
    )
}
