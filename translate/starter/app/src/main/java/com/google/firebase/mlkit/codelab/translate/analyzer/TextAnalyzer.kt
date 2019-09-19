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

package com.google.firebase.mlkit.codelab.translate.analyzer

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.nio.ByteBuffer


class TextAnalyzer(val result: MutableLiveData<String>) : ImageAnalysis.Analyzer {
    // Don't analyze new frames until current frame has finished processing
    private var isBusy = false

    private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
    }

    private fun cropByteArray(array: ByteArray, cropRect: Rect): ByteArray {
        val croppedArray = ByteArray(cropRect.width() * cropRect.height())
        val imageWidth = 640
        var i = 0
        array.forEachIndexed { index, byte ->
            val x = index % imageWidth
            val y = index / imageWidth

            if (cropRect.left <= x && x < cropRect.right && cropRect.top <= y && y < cropRect.bottom) {
                croppedArray[i] = byte
                i++
            }
        }
        return croppedArray
    }

    /**
     * Helper extension function used to extract a byte array from an
     * image plane buffer
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(imageProxy: ImageProxy, degrees: Int) {
        val mediaImage = imageProxy.image

        val imageRotation = degreesToFirebaseRotation(degrees)
        if (mediaImage != null && !isBusy) {
            isBusy = true
            val buffer = mediaImage.planes[0].buffer
            // Extract image data from callback object
            val imageByteArray = buffer.toByteArray()
//            Log.d(TAG, "dim: " + mediaImage.width + "x" + mediaImage.height)
            // top and bottom are # pix from right edge, left and right are # pix from top edge
            val data = cropByteArray(imageByteArray, Rect(200, 25, 400, 615))

            val imageMetadata =
                FirebaseVisionImageMetadata.Builder()
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12).setHeight(590)
                    .setWidth(200).setRotation(imageRotation).build()
//            recognizeTextOnDevice(data, imageMetadata).addOnCompleteListener { isBusy = false }

        }
    }

    companion object {
        private const val TAG = "TextAnalyzer"
    }
}