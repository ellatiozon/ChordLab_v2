package com.example.chordlab;

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

    public InstrumentGatekeeper(Context context) {
        try {
            // Load the model you exported from Google Colab
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, "instrument_gatekeeper.tflite");
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Optimize for mobile CPU
            tflite = new Interpreter(tfliteModel, options);

            // Preprocessor to automatically format the image for MobileNetV2
            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                    .build();

        } catch (IOException e) {
            Log.e("Gatekeeper", "Error loading model", e);
        }
    }

    /**
     * Crops the camera frame around the hand and runs the classifier.
     * @return true if Ukulele is detected, false otherwise.
     */
    public boolean verifyUkulele(Bitmap fullFrame, List<NormalizedLandmark> handLandmarks) {
        if (tflite == null || fullFrame == null) return false;

        // 1. Calculate the dynamic bounding box around the hand
        float minX = 1.0f, maxX = 0.0f, minY = 1.0f, maxY = 0.0f;
        for (NormalizedLandmark landmark : handLandmarks) {
            if (landmark.x() < minX) minX = landmark.x();
            if (landmark.x() > maxX) maxX = landmark.x();
            if (landmark.y() < minY) minY = landmark.y();
            if (landmark.y() > maxY) maxY = landmark.y();
        }

        // 2. Add padding to capture the fretboard beneath the fingers
        float padding = 0.25f; // 25% larger box
        int width = fullFrame.getWidth();
        int height = fullFrame.getHeight();

        int startX = (int) (Math.max(0, minX - padding) * width);
        int startY = (int) (Math.max(0, minY - padding) * height);
        int endX = (int) (Math.min(1, maxX + padding) * width);
        int endY = (int) (Math.min(1, maxY + padding) * height);

        int cropWidth = endX - startX;
        int cropHeight = endY - startY;

        if (cropWidth <= 0 || cropHeight <= 0) return false;

        // 3. Crop and Resize the Bitmap
        // 3. Crop and Resize the Bitmap
        Bitmap croppedBitmap = Bitmap.createBitmap(fullFrame, startX, startY, cropWidth, cropHeight);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);

        // FIX 2: Load the cropped image, not the full frame!
        tensorImage.load(croppedBitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // 4. Run Inference
        // FIX 1: Allocate memory for 3 classes, not 2!
        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 3}, DataType.FLOAT32);
        tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());

        // 5. Evaluate Results
        float[] probabilities = probabilityBuffer.getFloatArray();

        // Index 2 is now the Ukulele (Background=0, Guitar=1, Ukulele=2)
        float ukuleleConfidence = probabilities[2];

        // We only open the gate if it's highly confident it's a Ukulele
        return ukuleleConfidence > 0.60f; // 60% confidence threshold
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}