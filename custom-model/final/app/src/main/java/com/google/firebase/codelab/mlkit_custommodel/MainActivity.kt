// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.codelab.mlkit_custommodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    /** Data structure holding pairs of <label, confidence> for each inference result */
    data class LabelConfidence(val label: String, val confidence: Float)

    /** Current image being displayed in our app's screen */
    private var selectedImage: Bitmap? = null

    /** List of JPG files in our assets folder */
    private val imagePaths by lazy {
        resources.assets.list("")!!.filter { it.endsWith(".jpg") }
    }

    /** Labels corresponding to the output of the vision model. */
    private val labelList by lazy {
        BufferedReader(InputStreamReader(resources.assets.open(LABEL_PATH))).lineSequence().toList()
    }

    /** Preallocated buffers for storing image data. */
    private val imageBuffer = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    // Gets the targeted width / height.
    private val targetedWidthHeight: Pair<Int, Int>
        get() {
            val targetWidth: Int
            val targetHeight: Int
            val maxWidthForPortraitMode = image_view.width
            val maxHeightForPortraitMode = image_view.height
            targetWidth = maxWidthForPortraitMode
            targetHeight = maxHeightForPortraitMode
            return Pair(targetWidth, targetHeight)
        }

    /** Input options used for our Firebase model interpreter */
    private val modelInputOutputOptions by lazy {
        val inputDims = arrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE)
        val outputDims = arrayOf(DIM_BATCH_SIZE, labelList.size)
        FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.BYTE, inputDims.toIntArray())
            .setOutputFormat(0, FirebaseModelDataType.BYTE, outputDims.toIntArray())
            .build()
    }

    /** Firebase model interpreter used for the local model from assets */
    private lateinit var modelInterpreter: FirebaseModelInterpreter

    /** Initialize a local model interpreter from assets file */
    private fun createLocalModelInterpreter(): FirebaseModelInterpreter {
        // Select the first available .tflite file as our local model
        val localModelName = resources.assets.list("")?.firstOrNull { it.endsWith(".tflite") }
            ?: throw(RuntimeException("Don't forget to add the tflite file to your assets folder"))
        Log.d(TAG, "Local model found: $localModelName")

        // Create an interpreter with the local model asset
        val localModel =
            FirebaseCustomLocalModel.Builder().setAssetFilePath(localModelName).build()
        val localInterpreter = FirebaseModelInterpreter.getInstance(
            FirebaseModelInterpreterOptions.Builder(localModel).build())!!
        Log.d(TAG, "Local model interpreter initialized")

        // Return the interpreter
        return localInterpreter
    }

    /** Initialize a remote model interpreter from Firebase server */
    private suspend fun createRemoteModelInterpreter(): FirebaseModelInterpreter {
        return suspendCancellableCoroutine { cont ->
            runOnUiThread {
                Toast.makeText(baseContext, "Downloading model...", Toast.LENGTH_LONG).show()
            }

            // Define conditions required for our model to be downloaded. We only request Wi-Fi.
            val conditions =
                FirebaseModelDownloadConditions.Builder().requireWifi().build()

            // Build a remote model object by specifying the name you assigned the model
            // when you uploaded it in the Firebase console.
            val remoteModel =
                FirebaseCustomRemoteModel.Builder(REMOTE_MODEL_NAME).build()
            val manager = FirebaseModelManager.getInstance()
            manager.download(remoteModel, conditions).addOnCompleteListener {
                if (!it.isSuccessful) cont.resumeWithException(
                    RuntimeException("Remote model failed to download", it.exception))

                val msg = "Remote model successfully downloaded"
                runOnUiThread { Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show() }
                Log.d(TAG, msg)

                val remoteInterpreter = FirebaseModelInterpreter.getInstance(
                    FirebaseModelInterpreterOptions.Builder(remoteModel).build())!!
                Log.d(TAG, "Remote model interpreter initialized")

                // Return the interpreter via continuation object
                cont.resume(remoteInterpreter)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val adapter = ArrayAdapter(
            this, 
            android.R.layout.simple_spinner_dropdown_item, 
            imagePaths.mapIndexed { idx, _ -> "Image ${idx + 1}" })

        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
        button_run.setOnClickListener { runModelInference() }

        // Disable the inference button until model is loaded
        button_run.isEnabled = false

        // Load the model interpreter in a coroutine
        lifecycleScope.launch(Dispatchers.IO) {
            //modelInterpreter = createLocalModelInterpreter()
            //modelInterpreter = createRemoteModelInterpreter()
            runOnUiThread { button_run.isEnabled = true }
        }

    }

    /** Uses model to make predictions and interpret output into likely labels. */
    private fun runModelInference() = selectedImage?.let { image ->

        // Create input data.
        val imgData = convertBitmapToByteBuffer(image)

        try {
            // Create model inputs from our image data.
            val modelInputs = FirebaseModelInputs.Builder().add(imgData).build()

            // Perform inference using our model interpreter.
            modelInterpreter.run(modelInputs, modelInputOutputOptions).continueWith {
                val inferenceOutput = it.result?.getOutput<Array<ByteArray>>(0)!!

                // Display labels on the screen using an overlay
                val topLabels = getTopLabels(inferenceOutput)
                graphic_overlay.clear()
                graphic_overlay.add(LabelGraphic(graphic_overlay, topLabels))
                topLabels
            }

        } catch (exc: FirebaseMLException) {
            val msg = "Error running model inference"
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            Log.e(TAG, msg, exc)
        }
    }

    /** Gets the top labels in the results. */
    @Synchronized
    private fun getTopLabels(inferenceOutput: Array<ByteArray>): List<String> {
        // Since we ran inference on a single image, inference output will have a single row.
        val imageInference = inferenceOutput.first()

        // The columns of the image inference correspond to the confidence for each label.
        return labelList.mapIndexed { idx, label ->
            LabelConfidence(label, (imageInference[idx] and 0xFF.toByte()) / 255.0f)

            // Sort the results in decreasing order of confidence and return only top 3.
        }.sortedBy { it.confidence }.reversed().map { "${it.label}:${it.confidence}" }
            .subList(0, min(labelList.size, RESULTS_TO_SHOW))
    }

    /** Writes Image data into a `ByteBuffer`. */
    @Synchronized
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(
                DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
        scaledBitmap.getPixels(
            imageBuffer, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        // Convert the image to int points.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val `val` = imageBuffer[pixel++]
                imgData.put((`val` shr 16 and 0xFF).toByte())
                imgData.put((`val` shr 8 and 0xFF).toByte())
                imgData.put((`val` and 0xFF).toByte())
            }
        }
        return imgData
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        graphic_overlay.clear()
        selectedImage = decodeBitmapAsset(this, imagePaths[position])
        if (selectedImage != null) {
            // Get the dimensions of the View
            val targetedSize = targetedWidthHeight

            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor = max(
                    selectedImage!!.width.toFloat() / targetWidth.toFloat(),
                    selectedImage!!.height.toFloat() / maxHeight.toFloat())

            val resizedBitmap = Bitmap.createScaledBitmap(
                    selectedImage!!,
                    (selectedImage!!.width / scaleFactor).toInt(),
                    (selectedImage!!.height / scaleFactor).toInt(),
                    true)

            image_view.setImageBitmap(resizedBitmap)
            selectedImage = resizedBitmap
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) = Unit

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        /** Name of the label file stored in Assets. */
        private const val LABEL_PATH = "labels.txt"

        /** Name of the remote model in Firebase. */
        private const val REMOTE_MODEL_NAME = "mobilenet_v1_224_quant"

        /** Number of results to show in the UI. */
        private const val RESULTS_TO_SHOW = 3

        /** Dimensions of inputs. */
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 3
        private const val DIM_IMG_SIZE_X = 224
        private const val DIM_IMG_SIZE_Y = 224

        /** Utility function for loading and resizing images from app asset folder. */
        fun decodeBitmapAsset(context: Context, filePath: String): Bitmap =
            context.assets.open(filePath).let { BitmapFactory.decodeStream(it) }
    }
}
