package com.arprime.translator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arprime.translator.data.LanguageList
import com.arprime.translator.data.TranslationEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm: TranslatorViewModel = viewModel()
    val history by vm.history.collectAsState(initial = emptyList())
    val clipboard = LocalClipboardManager.current
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val items = if (showFavoritesOnly) history.filter { it.isFavorite } else history

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFavoritesOnly = !showFavoritesOnly }) {
                        Icon(
                            if (showFavoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorites only"
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear history")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (showFavoritesOnly) "No favorites yet" else "No translations yet")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(items, key = { it.id }) { item: TranslationEntity ->
                    HistoryRow(
                        item = item,
                        onToggleFavorite = { vm.toggleFavorite(item) },
                        onDelete = { vm.deleteHistoryItem(item) },
                        onSpeak = { vm.speak(item.translatedText, item.targetLangCode) },
                        onCopy = { clipboard.setText(AnnotatedString(item.translatedText)) }
                    )
                    Divider()
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear history") },
            text = { Text("Remove all translations except your favorites?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory(keepFavorites = true)
                    showClearDialog = false
                }) { Text("Clear, keep favorites") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.clearHistory(keepFavorites = false)
                    showClearDialog = false
                }) { Text("Clear everything") }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    item: TranslationEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    ListItem(
        overlineContent = {
            Text("${LanguageList.labelFor(item.sourceLangCode)} \u2192 ${LanguageList.labelFor(item.targetLangCode)}" +
                if (item.wasOffline) "  \u00b7  offline" else "")
        },
        headlineContent = { Text(item.translatedText, maxLines = 2) },
        supportingContent = {
            Text(
                item.sourceText + "  \u00b7  " + DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT
                ).format(Date(item.timestamp)),
                maxLines = 1
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite"
                    )
                }
                IconButton(onClick = onSpeak) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen")
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}
