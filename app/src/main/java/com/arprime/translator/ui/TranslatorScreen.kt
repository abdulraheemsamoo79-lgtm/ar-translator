package com.arprime.translator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arprime.translator.data.LanguageList

private enum class PickerTarget { NONE, SOURCE, TARGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(onOpenHistory: () -> Unit) {
    val vm: TranslatorViewModel = viewModel()
    val sourceLang by vm.sourceLang.collectAsState()
    val targetLang by vm.targetLang.collectAsState()
    val inputText by vm.inputText.collectAsState()
    val state by vm.state.collectAsState()
    val downloadedModels by vm.downloadedModels.collectAsState()
    val clipboard = LocalClipboardManager.current

    var pickerTarget by remember { mutableStateOf(PickerTarget.NONE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR Translator", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Language selector row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LangChip(
                    label = LanguageList.labelFor(sourceLang),
                    isDownloaded = downloadedModels.contains(sourceLang),
                    modifier = Modifier.weight(1f)
                ) { pickerTarget = PickerTarget.SOURCE }

                IconButton(onClick = { vm.swapLanguages() }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages")
                }

                LangChip(
                    label = LanguageList.labelFor(targetLang),
                    isDownloaded = downloadedModels.contains(targetLang),
                    modifier = Modifier.weight(1f)
                ) { pickerTarget = PickerTarget.TARGET }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { vm.onInputChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Type text to translate\u2026") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                trailingIcon = {
                    if (inputText.isNotBlank()) {
                        IconButton(onClick = { vm.onInputChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.detectAndSetSourceLanguage() }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Detect language")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.translate() }, enabled = inputText.isNotBlank()) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Translate")
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is TranslateState.Translating -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TranslateState.Success -> {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (s.wasOffline) "Translated offline" else "Translated",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(s.text, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(12.dp))
                            Row {
                                IconButton(onClick = { vm.speak(s.text, targetLang) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen")
                                }
                                IconButton(onClick = { clipboard.setText(AnnotatedString(s.text)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }
                is TranslateState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                TranslateState.Idle -> {}
            }
        }
    }

    if (pickerTarget != PickerTarget.NONE) {
        LanguagePickerDialog(
            downloadedCodes = downloadedModels,
            onDismiss = { pickerTarget = PickerTarget.NONE },
            onSelect = { code ->
                if (pickerTarget == PickerTarget.SOURCE) vm.setSourceLang(code) else vm.setTargetLang(code)
                pickerTarget = PickerTarget.NONE
            }
        )
    }
}

@Composable
private fun LangChip(label: String, isDownloaded: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            if (isDownloaded) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = "Available offline",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
