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

package com.google.firebase.codelab.mlkit;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.firebase.codelab.mlkit.GraphicOverlay.Graphic;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;

import java.util.List;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class CloudTextGraphic extends Graphic {
    private static final int TEXT_COLOR = Color.GREEN;
    private static final float TEXT_SIZE = 60.0f;
    private static final float STROKE_WIDTH = 5.0f;

    private final Paint rectPaint;
    private final Paint textPaint;
    private final FirebaseVisionDocumentText.Word word;
    private final GraphicOverlay overlay;

    CloudTextGraphic(GraphicOverlay overlay, FirebaseVisionDocumentText.Word word) {
        super(overlay);

        this.word = word;
        this.overlay = overlay;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (word == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }

        float x = overlay.getWidth() / 4.0f;
        float y = overlay.getHeight() / 4.0f;

        StringBuilder wordStr = new StringBuilder();
        Rect wordRect = word.getBoundingBox();
        canvas.drawRect(wordRect, rectPaint);
        List<FirebaseVisionDocumentText.Symbol> symbols = word.getSymbols();
        for (int m = 0; m < symbols.size(); m++) {
            wordStr.append(symbols.get(m).getText());
        }
        canvas.drawText(wordStr.toString(), wordRect.left, wordRect.bottom, textPaint);
    }
}
