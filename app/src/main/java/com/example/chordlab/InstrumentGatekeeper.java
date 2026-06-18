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
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;

public class InstrumentGatekeeper {

    private Interpreter tflite;
    private ImageProcessor imageProcessor;
    private android.graphics.RectF lastBoundingBox = null;

    // ── THE HEARTBEAT SYSTEM ──
    private long lastCheckTime = 0;
    private boolean lastVerificationResult = false;
    private final long HEARTBEAT_INTERVAL_MS = 2000; // Check every 2 seconds

    public InstrumentGatekeeper(Context context, String modelFilename) {
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, modelFilename);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Keep it lightweight
            tflite = new Interpreter(tfliteModel, options);

            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                    .build();

        } catch (IOException e) {
            Log.e("Gatekeeper", "Error loading model: " + modelFilename, e);
        }
    }

    // Called by the Activity when a new chord loads to force an immediate re-check!
    public void resetHeartbeat() {
        lastCheckTime = 0;
        lastVerificationResult = false;
    }

    public boolean verifyInstrument(Bitmap fullFrame, List<NormalizedLandmark> handLandmarks) {
        if (tflite == null || fullFrame == null) return false;

        // 1. FAST MATH: Calculate boundaries EVERY frame for smooth UI tracking
        float minX = 1.0f, maxX = 0.0f, minY = 1.0f, maxY = 0.0f;
        for (NormalizedLandmark landmark : handLandmarks) {
            if (landmark.x() < minX) minX = landmark.x();
            if (landmark.x() > maxX) maxX = landmark.x();
            if (landmark.y() < minY) minY = landmark.y();
            if (landmark.y() > maxY) maxY = landmark.y();
        }

        float handWidth = maxX - minX;
        float handHeight = maxY - minY;

        float xPadding = handWidth * 0.50f;
        float yBottomPadding = handHeight * 0.50f;
        float yTopPadding = handHeight * 2.50f;

        float normStartX = Math.max(0, minX - xPadding);
        float normStartY = Math.max(0, minY - yTopPadding);
        float normEndX = Math.min(1, maxX + xPadding);
        float normEndY = Math.min(1, maxY + yBottomPadding);

        lastBoundingBox = new android.graphics.RectF(normStartX, normStartY, normEndX, normEndY);

        // 2. THE HEARTBEAT THROTTLE
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime < HEARTBEAT_INTERVAL_MS) {
            // It hasn't been 2 seconds yet. Save battery and return the cached result!
            return lastVerificationResult;
        }

        // 3. HEAVY AI: It is time to poll the camera again.
        lastCheckTime = currentTime;

        int width = fullFrame.getWidth();
        int height = fullFrame.getHeight();

        int startX = (int) (normStartX * width);
        int startY = (int) (normStartY * height);
        int endX = (int) (normEndX * width);
        int endY = (int) (normEndY * height);

        int cropWidth = endX - startX;
        int cropHeight = endY - startY;

        if (cropWidth <= 0 || cropHeight <= 0) {
            lastVerificationResult = false;
            return false;
        }

        Bitmap croppedBitmap = Bitmap.createBitmap(fullFrame, startX, startY, cropWidth, cropHeight);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(croppedBitmap);
        tensorImage = imageProcessor.process(tensorImage);

        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
        tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());

        float[] probabilities = probabilityBuffer.getFloatArray();

        // Cache the result so we remember it for the next 2 seconds
        lastVerificationResult = probabilities[1] > 0.60f;
        return lastVerificationResult;
    }

    public android.graphics.RectF getBoundingBox() {
        return lastBoundingBox;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}