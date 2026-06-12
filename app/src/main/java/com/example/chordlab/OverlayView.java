package com.example.chordlab;

/**
 * ChordLab: Polyphonic Note and Chord Detection System
 * * This file is a core component of the ChordLab backend architecture,
 * handling AI processing, multimodal sensor fusion, and/or state management.
 *
 * @author Mikhaella Mari D. Tiozon
 * @version 1.0
 * @since 2026-04-17
 * * Note: The algorithmic logic, machine learning integration, and database
 * architecture contained within this file are the original intellectual
 * property of the author.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import java.util.List;

public class OverlayView extends View {

    private HandLandmarkerResult results;
    private int imageWidth = 1;
    private int imageHeight = 1;

    private Paint pointPaint;
    private Paint linePaint;

    private Paint boundingBoxPaint;

    // Scaling variables
    private float scaleFactor = 1f;
    private float leftOffset = 0f;
    private float topOffset = 0f;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public void setImageSourceInfo(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }

    private void initPaints() {
        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#C2185B"));
        pointPaint.setStrokeWidth(15f);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        boundingBoxPaint = new Paint();
        boundingBoxPaint.setColor(Color.RED);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(8f); // Thickness of the box
    }

    public void setResults(HandLandmarkerResult handLandmarkerResult) {
        this.results = handLandmarkerResult;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (results == null || results.landmarks().isEmpty()) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        scaleFactor = Math.max((float) viewWidth / imageWidth, (float) viewHeight / imageHeight);
        float scaledWidth = imageWidth * scaleFactor;
        float scaledHeight = imageHeight * scaleFactor;

        leftOffset = (viewWidth - scaledWidth) / 2f;
        topOffset = (viewHeight - scaledHeight) / 2f;

        List<NormalizedLandmark> handLandmarks = results.landmarks().get(0);

        if (handLandmarks.size() >= 21) {
            // 1. DRAW THE DYNAMIC BOUNDING BOX (THE GATEKEEPER'S VIEW)
            drawGatekeeperBox(canvas, handLandmarks);

            // 2. DRAW SKELETON AND POINTS
            drawSkeleton(canvas, handLandmarks);
            for (NormalizedLandmark landmark : handLandmarks) {
                canvas.drawCircle(
                        getCanvasX(landmark.x()),
                        getCanvasY(landmark.y()),
                        10f,
                        pointPaint
                );
            }
        }
    }

    private void drawGatekeeperBox(Canvas canvas, List<NormalizedLandmark> handLandmarks) {
        float minX = 1.0f, maxX = 0.0f, minY = 1.0f, maxY = 0.0f;

        // Find boundaries
        for (NormalizedLandmark landmark : handLandmarks) {
            if (landmark.x() < minX) minX = landmark.x();
            if (landmark.x() > maxX) maxX = landmark.x();
            if (landmark.y() < minY) minY = landmark.y();
            if (landmark.y() > maxY) maxY = landmark.y();
        }

        // Apply the same 25% padding as the InstrumentGatekeeper
        float padding = 0.25f;

        // Calculate the box edges in normalized coordinates
        float boxMinX = Math.max(0, minX - padding);
        float boxMaxX = Math.min(1, maxX + padding);
        float boxMinY = Math.max(0, minY - padding);
        float boxMaxY = Math.min(1, maxY + padding);

        // Convert to Canvas Pixels
        // Note: Because getCanvasX handles mirroring (1f - x),
        // the screen "left" corresponds to the larger raw X value.
        float left = getCanvasX(boxMaxX);
        float right = getCanvasX(boxMinX);
        float top = getCanvasY(boxMinY);
        float bottom = getCanvasY(boxMaxY);

        // Draw the rectangle
        canvas.drawRect(left, top, right, bottom, boundingBoxPaint);
    }

    private float getCanvasX(float normalizedX) {
        return ((1f - normalizedX) * imageWidth * scaleFactor) + leftOffset;
    }

    private float getCanvasY(float normalizedY) {
        return (normalizedY * imageHeight * scaleFactor) + topOffset;
    }

    private void drawSkeleton(Canvas canvas, List<NormalizedLandmark> landmarks) {
        // Wrist to Thumb
        connect(canvas, landmarks, 0, 1);
        connect(canvas, landmarks, 1, 2);
        connect(canvas, landmarks, 2, 3);
        connect(canvas, landmarks, 3, 4);

        // Fingers
        for (int i = 0; i < 4; i++) {
            int start = 5 + (i * 4);
            connect(canvas, landmarks, 0, start); // Connect base to wrist
            connect(canvas, landmarks, start, start + 1);
            connect(canvas, landmarks, start + 1, start + 2);
            connect(canvas, landmarks, start + 2, start + 3);
        }

        // Palm connection
        connect(canvas, landmarks, 5, 9);
        connect(canvas, landmarks, 9, 13);
        connect(canvas, landmarks, 13, 17);
        connect(canvas, landmarks, 17, 0);
    }

    private void connect(Canvas canvas, List<NormalizedLandmark> landmarks, int startIdx, int endIdx) {
        NormalizedLandmark start = landmarks.get(startIdx);
        NormalizedLandmark end = landmarks.get(endIdx);

        canvas.drawLine(
                getCanvasX(start.x()), getCanvasY(start.y()),
                getCanvasX(end.x()), getCanvasY(end.y()),
                linePaint
        );
    }
}