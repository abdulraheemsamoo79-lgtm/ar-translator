package com.arprime.translator.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arprime.translator.TranslatorApp
import com.arprime.translator.data.TranslationEntity
import com.arprime.translator.util.Prefs
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

sealed class TranslateState {
    object Idle : TranslateState()
    object Translating : TranslateState()
    data class Success(val text: String, val wasOffline: Boolean) : TranslateState()
    data class Error(val message: String) : TranslateState()
}

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as TranslatorApp).database.translationDao()
    private val prefs = Prefs(application)
    private var tts: TextToSpeech? = null
    private val modelManager = RemoteModelManager.getInstance()

    private val _sourceLang = MutableStateFlow("en")
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow("ur")
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _state = MutableStateFlow<TranslateState>(TranslateState.Idle)
    val state: StateFlow<TranslateState> = _state.asStateFlow()

    private val _downloadedModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModels: StateFlow<Set<String>> = _downloadedModels.asStateFlow()

    val history = dao.getAll()
    val favorites = dao.getFavorites()

    init {
        viewModelScope.launch {
            _sourceLang.value = prefs.sourceLang.first()
            _targetLang.value = prefs.targetLang.first()
        }
        refreshDownloadedModels()
        tts = TextToSpeech(application) { }
    }

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun swapLanguages() {
        val newSource = _targetLang.value
        val newTarget = _sourceLang.value
        _sourceLang.value = newSource
        _targetLang.value = newTarget
        persistLanguages()
    }

    fun setSourceLang(code: String) {
        _sourceLang.value = code
        persistLanguages()
    }

    fun setTargetLang(code: String) {
        _targetLang.value = code
        persistLanguages()
    }

    private fun persistLanguages() {
        viewModelScope.launch { prefs.setLanguages(_sourceLang.value, _targetLang.value) }
    }

    /** Detects the language of the current input and sets it as the source language. */
    fun detectAndSetSourceLanguage() {
        val text = _inputText.value
        if (text.isBlank()) return
        LanguageIdentification.getClient().identifyLanguage(text)
            .addOnSuccessListener { code ->
                if (code != "und" && TranslateLanguage.fromLanguageTag(code) != null) {
                    _sourceLang.value = code
                    persistLanguages()
                }
            }
    }

    fun translate(wifiOnlyDownload: Boolean = false) {
        val text = _inputText.value
        if (text.isBlank()) return

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(_sourceLang.value)
            .setTargetLanguage(_targetLang.value)
            .build()
        val translator = Translation.getClient(options)

        _state.value = TranslateState.Translating
        val wasAlreadyDownloaded = _downloadedModels.value.contains(_sourceLang.value) &&
            _downloadedModels.value.contains(_targetLang.value)

        val conditions = DownloadConditions.Builder().apply {
            if (wifiOnlyDownload) requireWifi()
        }.build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        _state.value = TranslateState.Success(translated, wasAlreadyDownloaded)
                        saveToHistory(text, translated, wasAlreadyDownloaded)
                        refreshDownloadedModels()
                        translator.close()
                    }
                    .addOnFailureListener {
                        _state.value = TranslateState.Error("Translation failed. Try again.")
                        translator.close()
                    }
            }
            .addOnFailureListener {
                _state.value = TranslateState.Error(
                    "Language pack not downloaded and you're offline. Connect once to download it, then it works offline forever."
                )
                translator.close()
            }
    }

    private fun saveToHistory(source: String, translated: String, offline: Boolean) {
        viewModelScope.launch {
            dao.insert(
                TranslationEntity(
                    sourceLangCode = _sourceLang.value,
                    targetLangCode = _targetLang.value,
                    sourceText = source,
                    translatedText = translated,
                    wasOffline = offline
                )
            )
        }
    }

    fun toggleFavorite(item: TranslationEntity) {
        viewModelScope.launch { dao.update(item.copy(isFavorite = !item.isFavorite)) }
    }

    fun deleteHistoryItem(item: TranslationEntity) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun clearHistory(keepFavorites: Boolean) {
        viewModelScope.launch {
            if (keepFavorites) dao.clearNonFavorites() else dao.clearAll()
        }
    }

    fun speak(text: String, langCode: String) {
        val locale = Locale(langCode)
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun refreshDownloadedModels() {
        viewModelScope.launch {
            try {
                val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
                _downloadedModels.value = models.map { it.language }.toSet()
            } catch (e: Exception) {
                // ignore — offline models list just stays as-is
            }
        }
    }

    fun deleteDownloadedModel(code: String) {
        viewModelScope.launch {
            val model = TranslateRemoteModel.Builder(code).build()
            try {
                modelManager.deleteDownloadedModel(model).await()
                refreshDownloadedModels()
            } catch (e: Exception) { }
        }
    }

    override fun onCleared() {
        tts?.shutdown()
        super.onCleared()
    }
}
