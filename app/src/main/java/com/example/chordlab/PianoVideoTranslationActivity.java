package com.example.chordlab;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class PianoVideoTranslationActivity extends AppCompatActivity {

    private LinearLayout processingLayout, chordTimelineContainer;
    private ScrollView resultsScrollView;
    private TextView txtProcessingStatus, txtSelectedChord, txtDurationTimeline;
    private ImageView imgTranslatedDiagram, btnBack;
    private Button btnUploadSimulate;

    // NEW: We now store an object that knows its start and end time!
    private List<ChordEvent> finalDetectedChords = new ArrayList<>();

    // Simple custom class to hold durational data
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

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    prepareUIForProcessing();
                    PianoVideoChordAnalyzer.resetAnalyzer();
                    startTranslationPipeline(videoUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piano_video_translation);

        bindViews();
        setupListeners();
        PianoVideoChordAnalyzer.initModels(this);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnUploadSimulate = findViewById(R.id.btnUploadSimulate);
        processingLayout = findViewById(R.id.processingLayout);
        txtProcessingStatus = findViewById(R.id.txtProcessingStatus);
        resultsScrollView = findViewById(R.id.resultsScrollView);
        chordTimelineContainer = findViewById(R.id.chordTimelineContainer);
        txtSelectedChord = findViewById(R.id.txtSelectedChord);
        imgTranslatedDiagram = findViewById(R.id.imgTranslatedDiagram);
        txtDurationTimeline = findViewById(R.id.txtDurationTimeline); // Bind new view
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnUploadSimulate.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            videoPickerLauncher.launch(intent);
        });
    }

    private void prepareUIForProcessing() {
        btnUploadSimulate.setEnabled(false);
        btnUploadSimulate.setText("Analyzing Video...");
        processingLayout.setVisibility(View.VISIBLE);
        resultsScrollView.setVisibility(View.GONE);
        chordTimelineContainer.removeAllViews();
        finalDetectedChords.clear();
    }

    private void startTranslationPipeline(Uri videoUri) {
        VideoAudioSlicer slicer = new VideoAudioSlicer();

        slicer.processVideo(this, videoUri, new VideoAudioSlicer.SlicerCallback() {
            @Override
            public void onProgressUpdate(String status) {
                runOnUiThread(() -> txtProcessingStatus.setText(status));
            }

            @Override
            public void onSliceReady(float[] audioChunk, float timestampSeconds) {
                String detected = PianoVideoChordAnalyzer.analyzeAudioSlice(audioChunk, PianoVideoTranslationActivity.this);

                if (detected != null) {
                    if (finalDetectedChords.isEmpty()) {
                        // Very first chord detected!
                        finalDetectedChords.add(new ChordEvent(detected, timestampSeconds, timestampSeconds + 1.0f));
                    } else {
                        ChordEvent lastEvent = finalDetectedChords.get(finalDetectedChords.size() - 1);

                        // If the AI detects the SAME chord, we just stretch the end time.
                        if (lastEvent.chordName.equals(detected)) {
                            lastEvent.endTime = timestampSeconds + 1.0f; // Each slice is 1 sec long
                        } else {
                            // If the AI detects a NEW chord, add a new block to the timeline
                            finalDetectedChords.add(new ChordEvent(detected, timestampSeconds, timestampSeconds + 1.0f));
                        }
                    }
                }
            }

            @Override
            public void onSlicingComplete() {
                runOnUiThread(() -> populateTimeline());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PianoVideoTranslationActivity.this, error, Toast.LENGTH_LONG).show();
                    resetUI();
                });
            }
        });
    }

    private void populateTimeline() {
        processingLayout.setVisibility(View.GONE);
        resultsScrollView.setVisibility(View.VISIBLE);
        btnUploadSimulate.setEnabled(true);
        btnUploadSimulate.setText("Select Another Video");

        if (finalDetectedChords.isEmpty()) {
            Toast.makeText(this, "No stable chords detected.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ChordEvent event : finalDetectedChords) {
            TextView chordBox = new TextView(this);
            // JUST the chord name, exactly like your sketch!
            chordBox.setText(event.chordName);
            chordBox.setTextColor(Color.WHITE);
            chordBox.setTextSize(16f);
            chordBox.setTypeface(null, Typeface.BOLD);
            chordBox.setBackgroundResource(R.drawable.bg_mode_selected);
            chordBox.setPadding(40, 24, 40, 24);
            chordBox.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chordBox.setLayoutParams(params);

            // Pass the whole event object to the display method
            chordBox.setOnClickListener(v -> displayTranslation(event));
            chordTimelineContainer.addView(chordBox);
        }

        // Show the first chord automatically
        displayTranslation(finalDetectedChords.get(0));
    }

    private void displayTranslation(ChordEvent event) {
        // 1. Update the Chord Title
        txtSelectedChord.setText(event.chordName);

        // 2. Format the time like [0:05 - 0:12] C major
        String timeString = String.format("[%s - %s] %s",
                formatTime(event.startTime),
                formatTime(event.endTime),
                event.chordName);

        txtDurationTimeline.setText(timeString);

        // 3. Update the Diagram
        int drawableResId = getTargetDrawable(event.chordName);
        if (drawableResId != 0) {
            imgTranslatedDiagram.setImageResource(drawableResId);
        } else {
            imgTranslatedDiagram.setImageDrawable(null);
        }
    }

    // Helper to turn seconds (65.5s) into a nice readable format (1:05)
    private String formatTime(float totalSeconds) {
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        return String.format("%d:%02d", minutes, seconds);
    }

    private void resetUI() {
        btnUploadSimulate.setEnabled(true);
        btnUploadSimulate.setText("Select Video");
        processingLayout.setVisibility(View.GONE);
    }

    private int getTargetDrawable(String targetName) {
        switch (targetName) {
            case "C major": return R.drawable.c_major_piano;
            case "D major": return R.drawable.d_major_piano;
            case "E major": return R.drawable.e_major_piano;
            case "F major": return R.drawable.f_major_piano;
            case "G major": return R.drawable.g_major_piano;
            case "A major": return R.drawable.a_major_piano;
            case "B major": return R.drawable.b_major_piano;
            default: return R.drawable.c_major_piano;
        }
    }
}