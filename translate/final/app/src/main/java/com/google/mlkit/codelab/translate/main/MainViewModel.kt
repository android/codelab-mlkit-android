/*
 * Copyright 2020 Google Inc. All Rights Reserved.
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

package com.google.mlkit.codelab.translate.main

import android.app.Application
import android.os.Handler
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.codelab.translate.util.Language
import com.google.mlkit.codelab.translate.util.ResultOrError
import com.google.mlkit.codelab.translate.util.SmoothedMutableLiveData
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.codelab.translate.main.MainFragment.Companion.DESIRED_HEIGHT_CROP_PERCENT
import com.google.mlkit.codelab.translate.main.MainFragment.Companion.DESIRED_WIDTH_CROP_PERCENT

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // TODO Instantiate LanguageIdentification
    val targetLang = MutableLiveData<Language>()
    val sourceText = SmoothedMutableLiveData<String>(SMOOTHING_DURATION)

    // We set desired crop percentages to avoid having to analyze the whole image from the live
    // camera feed. However, we are not guaranteed what aspect ratio we will get from the camera, so
    // we use the first frame we get back from the camera to update these crop percentages based on
    // the actual aspect ratio of images.
    val imageCropPercentages = MutableLiveData<Pair<Int, Int>>()
        .apply { value = Pair(DESIRED_HEIGHT_CROP_PERCENT, DESIRED_WIDTH_CROP_PERCENT) }
    val translatedText = MediatorLiveData<ResultOrError>()
    private val translating = MutableLiveData<Boolean>()
    val modelDownloading = SmoothedMutableLiveData<Boolean>(SMOOTHING_DURATION)

    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    private val translators =
        object : LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?
            ) {
                oldValue.close()
            }
        }

    val sourceLang = Transformations.switchMap(sourceText) { text ->
        val result = MutableLiveData<Language>()
        // TODO  Call the language identification method and assigns the result if it is not
        //  undefined (“und”)
        result
    }

    override fun onCleared() {
        // TODO Shut down ML Kit clients.
    }

    private fun translate(): Task<String> {
        // TODO Take the source language value, target language value, and the source text and
        //  perform the translation.
        //  If the chosen target language model has not been downloaded to the device yet,
        //  call downloadModelIfNeeded() and then proceed with the translation.
        return Tasks.forResult("") // replace this with your code
    }

    // Gets a list of all available translation languages.
    val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages()
        .map { Language(it) }

    init {
        modelDownloading.setValue(false)
        translating.value = false
        // Create a translation result or error object.
        val processTranslation =
            OnCompleteListener<String> { task ->
                if (task.isSuccessful) {
                    translatedText.value = ResultOrError(task.result, null)
                } else {
                    if (task.isCanceled) {
                        // Tasks are cancelled for reasons such as gating; ignore.
                        return@OnCompleteListener
                    }
                    translatedText.value = ResultOrError(null, task.exception)
                }
            }
        // Start translation if any of the following change: detected text, source lang, target lang.
        translatedText.addSource(sourceText) { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(sourceLang) { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(targetLang) { translate().addOnCompleteListener(processTranslation) }
    }

    companion object {
        // Amount of time (in milliseconds) to wait for detected text to settle
        private const val SMOOTHING_DURATION = 50L

        private const val NUM_TRANSLATORS = 1
    }
}
