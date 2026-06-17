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

    // NEW: We now pass the specific model filename when we create the Gatekeeper
    public InstrumentGatekeeper(Context context, String modelFilename) {
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, modelFilename);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            tflite = new Interpreter(tfliteModel, options);

            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                    .build();

        } catch (IOException e) {
            Log.e("Gatekeeper", "Error loading model: " + modelFilename, e);
        }
    }

    public boolean verifyInstrument(Bitmap fullFrame, List<NormalizedLandmark> handLandmarks) {
        if (tflite == null || fullFrame == null) return false;

        float minX = 1.0f, maxX = 0.0f, minY = 1.0f, maxY = 0.0f;
        for (NormalizedLandmark landmark : handLandmarks) {
            if (landmark.x() < minX) minX = landmark.x();
            if (landmark.x() > maxX) maxX = landmark.x();
            if (landmark.y() < minY) minY = landmark.y();
            if (landmark.y() > maxY) maxY = landmark.y();
        }

        float handWidth = maxX - minX;
        float handHeight = maxY - minY;

        // Using our new Asymmetrical "Peg Hunter" math!
        float xPadding = handWidth * 0.50f;
        float yBottomPadding = handHeight * 0.50f;
        float yTopPadding = handHeight * 2.50f;

        int width = fullFrame.getWidth();
        int height = fullFrame.getHeight();

        int startX = (int) (Math.max(0, minX - xPadding) * width);
        int startY = (int) (Math.max(0, minY - yTopPadding) * height);
        int endX = (int) (Math.min(1, maxX + xPadding) * width);
        int endY = (int) (Math.min(1, maxY + yBottomPadding) * height);

        int cropWidth = endX - startX;
        int cropHeight = endY - startY;

        if (cropWidth <= 0 || cropHeight <= 0) return false;

        Bitmap croppedBitmap = Bitmap.createBitmap(fullFrame, startX, startY, cropWidth, cropHeight);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(croppedBitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // REVERTED TO 2 CLASSES: Background (0) and Target Instrument (1)
        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
        tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());

        float[] probabilities = probabilityBuffer.getFloatArray();

        // Index 1 is whatever instrument this specific model was trained on
        return probabilities[1] > 0.60f;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}