package com.deskzen.ui.suggestions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.deskzen.domain.model.AppInfo
import com.deskzen.domain.model.SuggestionStatus
import com.deskzen.domain.model.ThemeSuggestion
import com.deskzen.ui.components.AppIcon
import com.deskzen.ui.components.ConfidenceBadge
import com.deskzen.ui.components.DeskZenTopBar
import com.deskzen.ui.components.EmptyState
import com.deskzen.ui.theme.DeskZenDimens

@Composable
fun SuggestionsScreen(
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            DeskZenTopBar(
                title = "Suggestions IA",
                actions = {
                    IconButton(onClick = { viewModel.generateSuggestions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regénérer")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is SuggestionsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Analyse en cours...")
                }
            }

            is SuggestionsUiState.Success -> {
                if (state.suggestions.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Aucune suggestion",
                        subtitle = "Installez plus d'applications pour recevoir des suggestions",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(DeskZenDimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(DeskZenDimens.spacingMd),
                        modifier = Modifier.padding(padding)
                    ) {
                        items(
                            items = state.suggestions,
                            key = { it.themeName }
                        ) { suggestion ->
                            ThemeCard(
                                suggestion = suggestion,
                                onToggleTheme = { viewModel.onToggleTheme(suggestion.themeName) },
                                onRemoveApp = { pkg ->
                                    viewModel.onToggleApp(suggestion.themeName, pkg)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))
                            Button(
                                onClick = { /* Apply suggestions */ },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.suggestions.any {
                                    it.status != SuggestionStatus.REJECTED
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                                Text("Appliquer les suggestions")
                            }
                        }
                    }
                }
            }

            is SuggestionsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    suggestion: ThemeSuggestion,
    onToggleTheme: () -> Unit,
    onRemoveApp: (String) -> Unit
) {
    val isRejected = suggestion.status == SuggestionStatus.REJECTED
    val alpha = if (isRejected) 0.5f else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRejected) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = DeskZenDimens.cardElevation)
    ) {
        Column(modifier = Modifier.padding(DeskZenDimens.spacingMd)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${suggestion.themeIcon} ${suggestion.themeName}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                ConfidenceBadge(confidence = suggestion.confidence)
                Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isRejected) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = if (isRejected) "Rejeter" else "Accepter",
                        tint = if (isRejected) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Text(
                text = "${suggestion.apps.size} applications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (!isRejected) {
                Spacer(modifier = Modifier.height(DeskZenDimens.spacingSm))
                suggestion.apps.forEach { app ->
                    AppSuggestionRow(
                        appInfo = app,
                        onRemove = { onRemoveApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppSuggestionRow(
    appInfo: AppInfo,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DeskZenDimens.spacingXs)
    ) {
        AppIcon(
            icon = appInfo.icon,
            label = appInfo.label,
            size = DeskZenDimens.appIconSmall
        )
        Spacer(modifier = Modifier.width(DeskZenDimens.spacingSm))
        Text(
            text = appInfo.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Retirer",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
