package com.example.chordlab;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

public class PianoVideoChordAnalyzer {

    // We keep all 3 models in memory for instant comparison
    private static Interpreter majorTflite;
    private static Interpreter minorTflite;
    private static Interpreter accTflite;

    private static final int WINDOW_SIZE = 4;
    private static final int REQUIRED_VOTES = 2;
    private static LinkedList<String> videoSlidingWindow = new LinkedList<>();

    // The exact labels from your old setup
    private static final String[] MAJOR_LABELS = {"A major", "B major", "Background Noise", "C major", "D major", "E major", "F major", "G major"};
    private static final String[] MINOR_LABELS = {"A minor", "B minor", "Background Noise", "C minor", "D minor", "E minor", "F minor", "G minor"};
    private static final String[] ACC_LABELS = {"Background Noise", "Bb major", "Bb minor", "C# major", "C# minor", "Eb major", "Eb minor", "F# major", "F# minor", "G# major", "G# minor"};

    // 1. Initialize all 3 models once
    public static void initModels(Context context) {
        try {
            if (majorTflite == null) majorTflite = new Interpreter(loadModelFile(context, "piano_chords_majors_spectrogram.tflite"));
            if (minorTflite == null) minorTflite = new Interpreter(loadModelFile(context, "piano_chords_minors_spectrogram.tflite"));
            if (accTflite == null) accTflite = new Interpreter(loadModelFile(context, "piano_chords_flats_sharps_spectrogram.tflite"));
        } catch (Exception e) {
            Log.e("ChordLab_Fusion", "Error loading models: " + e.getMessage());
        }
    }

    private static MappedByteBuffer loadModelFile(Context context, String modelName) throws Exception {
        AssetFileDescriptor fd = context.getAssets().openFd(modelName);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        return fis.getChannel().map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    public static void resetAnalyzer() {
        videoSlidingWindow.clear();
    }

    // 2. The Core Fusion Logic
    public static String analyzeAudioSlice(float[] audioBuffer, Context context) {
        initModels(context);
        if (majorTflite == null || minorTflite == null || accTflite == null) return null;

        int inputSize = 16000;
        float[][] input = new float[1][inputSize];
        float maxAmplitude = 0.0f;

        for (float val : audioBuffer) {
            float absVal = Math.abs(val);
            if (absVal > maxAmplitude) maxAmplitude = absVal;
        }

        // Too quiet = instant noise vote
        if (maxAmplitude < 0.05f) {
            return processVote("Noise");
        }

        // Normalize and pad array to exactly 16000
        for (int i = 0; i < inputSize; i++) {
            if (i < audioBuffer.length) input[0][i] = audioBuffer[i] / maxAmplitude;
            else input[0][i] = 0.0f;
        }

        float[][] majorOut = new float[1][MAJOR_LABELS.length];
        float[][] minorOut = new float[1][MINOR_LABELS.length];
        float[][] accOut = new float[1][ACC_LABELS.length];

        try {
            // Run all 3 models simultaneously on the same 1-second chunk
            majorTflite.run(input, majorOut);
            minorTflite.run(input, minorOut);
            accTflite.run(input, accOut);
        } catch (Exception e) {
            Log.e("ChordLab_Fusion", "Inference Crash: " + e.getMessage());
            return null;
        }

        // Find best guess from Major Model
        int bestMajorIdx = 0; float maxMajor = 0;
        for (int i=0; i<majorOut[0].length; i++) { if (majorOut[0][i] > maxMajor) { maxMajor = majorOut[0][i]; bestMajorIdx = i; } }

        // Find best guess from Minor Model
        int bestMinorIdx = 0; float maxMinor = 0;
        for (int i=0; i<minorOut[0].length; i++) { if (minorOut[0][i] > maxMinor) { maxMinor = minorOut[0][i]; bestMinorIdx = i; } }

        // Find best guess from Accidental Model
        int bestAccIdx = 0; float maxAcc = 0;
        for (int i=0; i<accOut[0].length; i++) { if (accOut[0][i] > maxAcc) { maxAcc = accOut[0][i]; bestAccIdx = i; } }

        // 3. The Showdown: Which model is the most confident?
        float ultimateConfidence = maxMajor;
        String ultimateWinner = MAJOR_LABELS[bestMajorIdx];

        if (maxMinor > ultimateConfidence) {
            ultimateConfidence = maxMinor;
            ultimateWinner = MINOR_LABELS[bestMinorIdx];
        }
        if (maxAcc > ultimateConfidence) {
            ultimateConfidence = maxAcc;
            ultimateWinner = ACC_LABELS[bestAccIdx];
        }

        // Log the final decision to help us debug
        Log.d("ChordLab_Fusion", "Fusion Winner: " + ultimateWinner + " | Confidence: " + ultimateConfidence);

        // 4. Send the winner to the Voting Filter
        if (ultimateWinner.equalsIgnoreCase("Background Noise") || ultimateConfidence < 0.40f) {
            return processVote("Noise");
        } else {
            return processVote(ultimateWinner);
        }
    }

    // 5. The Voting Logic (Ensures we don't pick up random glitches)
    private static String processVote(String currentWinner) {
        videoSlidingWindow.add(currentWinner);
        while (videoSlidingWindow.size() > WINDOW_SIZE) {
            videoSlidingWindow.removeFirst();
        }

        String topCandidate = videoSlidingWindow.getLast();
        if (!topCandidate.equals("Noise")) {
            int voteCount = 0;
            for (String frame : videoSlidingWindow) {
                if (frame.equals(topCandidate)) voteCount++;
            }
            if (voteCount >= REQUIRED_VOTES) {
                videoSlidingWindow.clear();
                return topCandidate; // We have a confirmed chord!
            }
        }
        return null;
    }
}