package com.deskzen.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.deskzen.ui.theme.DeskZenDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    sortMode: SortMode,
    onSortModeChanged: (SortMode) -> Unit,
    showSystemApps: Boolean,
    onToggleSystemApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = DeskZenDimens.spacingMd)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher une application...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Rechercher")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DeskZenDimens.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(DeskZenDimens.spacingSm)
        ) {
            SortMode.entries.forEach { mode ->
                SortModeChip(
                    mode = mode,
                    isSelected = mode == sortMode,
                    onClick = { onSortModeChanged(mode) }
                )
            }
            FilterChip(
                selected = showSystemApps,
                onClick = onToggleSystemApps,
                label = { Text("Système") }
            )
        }
    }
}

@Composable
fun SortModeChip(
    mode: SortMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (mode) {
        SortMode.ALPHABETICAL -> "A-Z"
        SortMode.INSTALL_DATE -> "Date"
        SortMode.CATEGORY -> "Catégorie"
        SortMode.LAST_USED -> "Récent"
    }
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) }
    )
}
