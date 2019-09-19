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

package com.google.firebase.mlkit.codelab.translate.util

import androidx.lifecycle.MutableLiveData

/**
 * A {@link MutableLiveData} that only emits change events when the underlying data has been stable
 * for the configured amount of time.
 *
 * @param duration time delay to wait in milliseconds
 */
class SmoothedMutableLiveData<T>(private val duration: Int) : MutableLiveData<T>() {
    private var startTime = System.currentTimeMillis()
    private var pendingValue: T? = null

    override fun setValue(value: T) {
        if (value != pendingValue) {
            resetInterval()
            pendingValue = value
        } else {
            if (hasIntervalElapsed()) {
                resetInterval()
                super.setValue(value)
            }
        }
    }

    private fun hasIntervalElapsed(): Boolean {
        return System.currentTimeMillis() - startTime > duration
    }

    private fun resetInterval() {
        startTime = System.currentTimeMillis()
    }
}