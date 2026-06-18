package com.example.chordlab;

/**
 * ChordLab: Polyphonic Note and Chord Detection System
 * This file is a core component of the ChordLab backend architecture,
 * handling AI processing, multimodal sensor fusion, and/or state management.
 *
 * @author Mikhaella Mari D. Tiozon
 * @version 1.0
 * @since 2026-04-17
 *
 * Note: The algorithmic logic, machine learning integration, and database
 * architecture contained within this file are the original intellectual
 * property of the author.
 */

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PianoVideoTranslationActivity extends AppCompatActivity {

    private LinearLayout btnBackToDashboard, layoutProgress, layoutResultSummary;
    private CardView cardUploadArea;
    private TextView tvUploadStatusText, tvSelectedFileName, tvDetectedKey, tvChordCount, tvResultsDisplay;
    private Button btnProcessAudio;
    private View resultsDivider;

    private Uri selectedMediaUri = null;
    private List<ChordEvent> finalDetectedChords = new ArrayList<>();

    private int noiseToleranceCounter = 0;
    private final int MAX_TOLERANCE = 2;

    private static class ChordEvent {
        String chordName;
        float startTime;
        float endTime;

        ChordEvent(String chordName, float startTime, float endTime) {
            this.chordName = chordName;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private final ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedMediaUri = result.getData().getData();

                    String fileName = getFileName(selectedMediaUri);
                    tvSelectedFileName.setText(fileName);
                    tvUploadStatusText.setText("Audio Selected");
                    tvUploadStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));

                    btnProcessAudio.setEnabled(true);
                    btnProcessAudio.setAlpha(1.0f);

                    resetResultsUI();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piano_video_translation);

        bindViews();
        setupListeners();

        PianoVideoChordAnalyzer.initModel(this);
    }

    private void bindViews() {
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard);
        cardUploadArea = findViewById(R.id.cardUploadArea);
        tvUploadStatusText = findViewById(R.id.tvUploadStatusText);
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName);
        btnProcessAudio = findViewById(R.id.btnProcessAudio);
        layoutProgress = findViewById(R.id.layoutProgress);
        layoutResultSummary = findViewById(R.id.layoutResultSummary);
        tvDetectedKey = findViewById(R.id.tvDetectedKey);
        tvChordCount = findViewById(R.id.tvChordCount);
        tvResultsDisplay = findViewById(R.id.tvResultsDisplay);
        resultsDivider = findViewById(R.id.resultsDivider);
    }

    private void setupListeners() {
        btnBackToDashboard.setOnClickListener(v -> finish());

        cardUploadArea.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("*/*");
            String[] mimeTypes = {"audio/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            audioPickerLauncher.launch(intent);
        });

        btnProcessAudio.setOnClickListener(v -> {
            if (selectedMediaUri != null) {
                startTranslationPipeline(selectedMediaUri);
            }
        });
    }

    private void resetResultsUI() {
        layoutResultSummary.setVisibility(View.GONE);
        resultsDivider.setVisibility(View.GONE);
        tvResultsDisplay.setText("Upload a piano audio track and tap\n'Recognize Chords' to detect chords.");
        tvResultsDisplay.setTextColor(0xFFAAAAAA); // Gray
        finalDetectedChords.clear();
    }

    private void startTranslationPipeline(Uri mediaUri) {
        btnProcessAudio.setEnabled(false);
        btnProcessAudio.setAlpha(0.5f);
        cardUploadArea.setEnabled(false);
        layoutProgress.setVisibility(View.VISIBLE);
        resetResultsUI();

        VideoAudioSlicer slicer = new VideoAudioSlicer();

        slicer.processVideo(this, mediaUri, new VideoAudioSlicer.SlicerCallback() {
            @Override
            public void onProgressUpdate(String status) {
            }

            private final java.util.LinkedList<String> rollingBuffer = new java.util.LinkedList<>();
            private static final int BUFFER_SIZE = 5; // 5 frames = 1 second of stable memory

            @Override
            public void onSliceReady(float[] audioChunk, float timestampSeconds) {
                String detected = PianoVideoChordAnalyzer.translateAudioSlice(audioChunk, PianoVideoTranslationActivity.this);
                if (detected == null || detected.equals("Model Missing")) {
                    detected = "Background Noise";
                }

                rollingBuffer.add(detected);
                if (rollingBuffer.size() > BUFFER_SIZE) {
                    rollingBuffer.removeFirst();
                }

                if (rollingBuffer.size() < 3) return;

                String stableWinner = "Background Noise";
                int maxCount = 0;

                for (String chord : rollingBuffer) {
                    int count = java.util.Collections.frequency(rollingBuffer, chord);
                    if (count > maxCount) {
                        maxCount = count;
                        stableWinner = chord;
                    }
                }

                if (maxCount < 2) return;

                if (stableWinner.equals("Background Noise")) return;

                float exactTime = Math.round(timestampSeconds * 5) / 5.0f;

                if (finalDetectedChords.isEmpty()) {
                    finalDetectedChords.add(new ChordEvent(stableWinner, exactTime, exactTime + 0.2f));
                } else {
                    ChordEvent lastEvent = finalDetectedChords.get(finalDetectedChords.size() - 1);

                    if (lastEvent.chordName.equals(stableWinner)) {
                        lastEvent.endTime = exactTime + 0.2f;
                    } else {
                        if (exactTime < lastEvent.endTime) exactTime = lastEvent.endTime;
                        finalDetectedChords.add(new ChordEvent(stableWinner, exactTime, exactTime + 0.2f));
                    }
                }
            }

            @Override
            public void onSlicingComplete() {
                runOnUiThread(() -> populateDashboard());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    layoutProgress.setVisibility(View.GONE);
                    btnProcessAudio.setEnabled(true);
                    btnProcessAudio.setAlpha(1.0f);
                    cardUploadArea.setEnabled(true);
                    Toast.makeText(PianoVideoTranslationActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void populateDashboard() {
        layoutProgress.setVisibility(View.GONE);
        btnProcessAudio.setEnabled(true);
        btnProcessAudio.setAlpha(1.0f);
        cardUploadArea.setEnabled(true);

        if (finalDetectedChords.isEmpty()) {
            tvResultsDisplay.setText("No stable piano chords detected in this audio.");
            return;
        }

        StringBuilder timelineText = new StringBuilder();
        Set<String> uniqueChords = new HashSet<>();

        String dominantChord = "";
        float maxDuration = 0;

        for (ChordEvent event : finalDetectedChords) {
            uniqueChords.add(event.chordName);

            float duration = event.endTime - event.startTime;
            if (duration > maxDuration && !event.chordName.contains("Noise")) {
                maxDuration = duration;
                dominantChord = event.chordName;
            }

            timelineText.append(String.format("[%s - %s]   %s\n",
                    formatTime(event.startTime),
                    formatTime(event.endTime),
                    event.chordName));
        }

        layoutResultSummary.setVisibility(View.VISIBLE);
        resultsDivider.setVisibility(View.VISIBLE);

        tvChordCount.setText(String.valueOf(uniqueChords.size()));

        if (!dominantChord.isEmpty()) {
            String rootKey = dominantChord.split(" ")[0];
            tvDetectedKey.setText(rootKey);
        } else {
            tvDetectedKey.setText("-");
        }

        tvResultsDisplay.setText(timelineText.toString().trim());
        tvResultsDisplay.setTextColor(0xFF333333);
    }

    private String formatTime(float totalSeconds) {
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        int milliseconds = Math.round((totalSeconds - (int)totalSeconds) * 10);

        if (milliseconds == 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%d:%02d.%d", minutes, seconds, milliseconds);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result : "audio_file";
    }
}