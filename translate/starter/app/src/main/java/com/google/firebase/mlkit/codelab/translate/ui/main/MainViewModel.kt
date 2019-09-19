/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.google.firebase.mlkit.codelab.translate.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateModelManager
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import com.google.firebase.mlkit.codelab.translate.R
import com.google.firebase.mlkit.codelab.translate.util.Language
import com.google.firebase.mlkit.codelab.translate.util.ResultOrError
import com.google.firebase.mlkit.codelab.translate.util.SmoothedMutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager: FirebaseTranslateModelManager =
        FirebaseTranslateModelManager.getInstance()
    var targetLang = MutableLiveData<Language>()
    var sourceText = SmoothedMutableLiveData<String>(SMOOTHING_DURATION)
    var translatedText = MediatorLiveData<ResultOrError>()
    var availableModels = MutableLiveData<List<String>>()

    var sourceLang = Transformations.switchMap(sourceText) { text ->
        // TODO: Handle error reporting
        // TODO: Very first word doesn't seem to be identified. (show processing delay?)
        val result = MutableLiveData<Language>()

        // TODO: Add code here to use ML Kit to identify the language of some text.

        result
    }

    // TODO: Replace the content of this function to use ML Kit to translate the text.
    fun translate(): Task<String> {
        return Tasks.forResult("") // replace this with your code
    }

    // Gets a list of all available translation languages.
    val availableLanguages: List<Language> = FirebaseTranslateLanguage.getAllLanguages()
        .map { Language(FirebaseTranslateLanguage.languageCodeForLanguage(it)) }

    init {
        // Create a translation result or error object.
        val processTranslation =
            OnCompleteListener<String> { task ->
                if (task.isSuccessful) {
                    translatedText.value = ResultOrError(task.result, null)
                } else {
                    translatedText.value = ResultOrError(null, task.exception)
                }
                // Update the list of downloaded models as more may have been
                // automatically downloaded due to requested translation.
                fetchDownloadedModels()
                // TODO: Maybe don't need to fetch
            }
        // Start translation if any of the following change: detected text, source lang, target lang.
        translatedText.addSource(sourceText) { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(sourceLang) { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(targetLang) { translate().addOnCompleteListener(processTranslation) }

        // Update the list of downloaded models.
        fetchDownloadedModels()
    }

    // Updates the list of downloaded models available for local translation.
    private fun fetchDownloadedModels() {
        modelManager.getAvailableModels(FirebaseApp.getInstance())
            .addOnSuccessListener { remoteModels ->
                availableModels.value =
                    remoteModels.sortedBy { it.languageCode }.map { it.languageCode }
            }
    }

    companion object {
        // Amount of time (in milliseconds) to wait for detected text to settle
        private const val SMOOTHING_DURATION = 50
    }
}
