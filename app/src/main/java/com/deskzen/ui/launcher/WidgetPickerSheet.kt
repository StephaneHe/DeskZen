package com.deskzen.ui.launcher

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.theme.DeskZenDimens
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloGlow
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloSurface

data class WidgetInfo(
    val providerInfo: AppWidgetProviderInfo,
    val label: String,
    val icon: Drawable?,
    val appLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    widgets: List<WidgetInfo>,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) widgets
    else widgets.filter {
        it.label.contains(searchQuery, ignoreCase = true) ||
                it.appLabel.contains(searchQuery, ignoreCase = true)
    }

    // Group by app
    val grouped = filtered.groupBy { it.appLabel }.toSortedMap()

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
            Text(
                text = "Ajouter un widget",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = SoloGlow,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Rechercher...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SoloElectricBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Effacer", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoloElectricBlue,
                    unfocusedBorderColor = SoloPurple.copy(alpha = 0.3f),
                    cursorColor = SoloElectricBlue,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))

            LazyColumn(modifier = Modifier.height(450.dp)) {
                grouped.forEach { (appName, appWidgets) ->
                    item {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleSmall,
                            color = SoloPurple,
                            modifier = Modifier.padding(
                                top = DeskZenDimens.spacingMd,
                                bottom = DeskZenDimens.spacingXs
                            )
                        )
                    }
                    items(appWidgets) { widget ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWidgetSelected(widget.providerInfo) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                icon = widget.icon,
                                label = widget.label,
                                size = DeskZenDimens.drawerIconSize
                            )
                            Spacer(modifier = Modifier.width(DeskZenDimens.spacingMd))
                            Column {
                                Text(
                                    text = widget.label,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${widget.providerInfo.minWidth}×${widget.providerInfo.minHeight}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeskZenDimens.spacingMd))
        }
    }
}
