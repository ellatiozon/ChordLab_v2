package com.example.chordlab;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class PianoVideoChordAnalyzer {

    private static Interpreter tfliteMajors;

    // ONLY the 8 labels from your original Majors model
    private static final String[] MAJOR_LABELS = {
            "A major", "B major", "Background Noise", "C major",
            "D major", "E major", "F major", "G major"
    };

    public static void initModel(Context context) {
        if (tfliteMajors != null) return;
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Keeps the UI completely smooth
            tfliteMajors = new Interpreter(loadModelFile(context, "piano_video_majors_spectrogram.tflite"), options);
            Log.d("ChordLab_MVP", "Engine Initialized: Majors Only.");
        } catch (Exception e) {
            Log.e("ChordLab_MVP", "Failed to load model: " + e.getMessage());
        }
    }

    public static String translateAudioSlice(float[] rawAudioChunk, Context context) {
        if (tfliteMajors == null) {
            initModel(context);
            if (tfliteMajors == null) return "Model Missing";
        }

        // 1. Format the audio chunk
        float[] verifiedBuffer = new float[16000];
        if (rawAudioChunk != null) {
            System.arraycopy(rawAudioChunk, 0, verifiedBuffer, 0, Math.min(rawAudioChunk.length, 16000));
        }

        // 2. Amplitude Gate (Silence Filter)
        float maxAmplitude = 0.0f;
        for (float val : verifiedBuffer) {
            float absVal = Math.abs(val);
            if (absVal > maxAmplitude) maxAmplitude = absVal;
        }

        // If it's too quiet, instantly return noise
        if (maxAmplitude < 0.05f) {
            return "Background Noise";
        }

        // 3. Normalize for the model
        float[][] inputTensor = new float[1][16000];
        for (int i = 0; i < verifiedBuffer.length; i++) {
            inputTensor[0][i] = verifiedBuffer[i] / maxAmplitude;
        }

        // 4. Run Inference
        float[][] outputDistribution = new float[1][MAJOR_LABELS.length];
        tfliteMajors.run(inputTensor, outputDistribution);

        // 5. Find the winner
        int bestIdx = -1;
        float highestConfidence = -1.0f;

        for (int i = 0; i < MAJOR_LABELS.length; i++) {
            if (outputDistribution[0][i] > highestConfidence) {
                highestConfidence = outputDistribution[0][i];
                bestIdx = i;
            }
        }

        String predictedChord = MAJOR_LABELS[bestIdx];

        // Strict 60% confidence threshold to prevent flickering
        if (highestConfidence < 0.60f) {
            return "Background Noise";
        }

        Log.d("ChordLab_MVP", "Detected: " + predictedChord + " (" + (highestConfidence * 100) + "%)");
        return predictedChord;
    }

    private static MappedByteBuffer loadModelFile(Context context, String modelName) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}