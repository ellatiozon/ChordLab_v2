package com.example.chordlab;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

public class VideoTranslatorActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private LinearLayout btnBackToDashboard; // now a LinearLayout in the redesigned XML
    private CardView     cardUploadArea;
    private TextView     tvUploadStatusText, tvSelectedFileName, tvResultsDisplay;
    private TextView     tvDetectedKey, tvChordCount;
    private AppCompatButton btnProcessAudio;
    private ProgressBar  progressBar;
    private LinearLayout layoutProgress;       // wraps spinner + "Analyzing..." label
    private LinearLayout layoutResultSummary;  // summary stat cards (Key + Chords Found)
    private View         resultsDivider;

    // Uri of the audio file selected by the user
    private Uri selectedAudioUri = null;

    // ── File picker launcher ─────────────────────────────────────────────────
    private final ActivityResultLauncher<String> audioPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedAudioUri = uri;
                            String fileName  = getFileNameFromUri(uri);

                            tvUploadStatusText.setText("File Ready! ✓");
                            tvSelectedFileName.setText(fileName);

                            btnProcessAudio.setEnabled(true);
                            btnProcessAudio.setAlpha(1.0f);

                            Toast.makeText(this, "Audio selected: " + fileName,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_translator);

        initializeViews();
        setupListeners();
    }

    // ── View binding ─────────────────────────────────────────────────────────
    private void initializeViews() {
        btnBackToDashboard  = findViewById(R.id.btnBackToDashboard);
        cardUploadArea      = findViewById(R.id.cardUploadArea);
        tvUploadStatusText  = findViewById(R.id.tvUploadStatusText);
        tvSelectedFileName  = findViewById(R.id.tvSelectedFileName);
        btnProcessAudio     = findViewById(R.id.btnProcessAudio);
        progressBar         = findViewById(R.id.progressBar);
        layoutProgress      = findViewById(R.id.layoutProgress);
        tvResultsDisplay    = findViewById(R.id.tvResultsDisplay);
        tvDetectedKey       = findViewById(R.id.tvDetectedKey);
        tvChordCount        = findViewById(R.id.tvChordCount);
        layoutResultSummary = findViewById(R.id.layoutResultSummary);
        resultsDivider      = findViewById(R.id.resultsDivider);
    }

    // ── Click listeners ──────────────────────────────────────────────────────
    private void setupListeners() {
        // Back to Dashboard
        btnBackToDashboard.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Open system audio file picker
        cardUploadArea.setOnClickListener(v ->
                audioPickerLauncher.launch("audio/*"));

        // Process the selected audio
        btnProcessAudio.setOnClickListener(v -> {
            if (selectedAudioUri != null) {
                simulateProcessingState();
            }
        });
    }

    // ── Processing simulation ─────────────────────────────────────────────────
    private void simulateProcessingState() {
        // Show progress, hide results, disable button
        setProcessingUI(true);
        tvResultsDisplay.setText("Analyzing audio frequencies and detecting musical notes...");
        tvResultsDisplay.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Hide result summary cards while processing
        layoutResultSummary.setVisibility(View.GONE);
        resultsDivider.setVisibility(View.GONE);

        // ── HOOK POINT FOR YOUR FRIENDS ──────────────────────────────────────
        // Replace this postDelayed block with your actual AI model call or API request.
        // Use 'selectedAudioUri' to read the raw audio bytes and send to your backend.
        // When results come back, call showResults(key, chordProgression).
        // ─────────────────────────────────────────────────────────────────────
        layoutProgress.postDelayed(() -> {
            setProcessingUI(false);
            showResults("C", "[ C Major ] → [ G Major ] → [ A Minor ] → [ F Major ]", 4);
            Toast.makeText(this, "Analysis complete!", Toast.LENGTH_SHORT).show();
        }, 3000);
    }

    /**
     * Displays the detection results in the results card.
     *
     * @param key             Detected key (e.g. "C")
     * @param progression     Human-readable chord progression string
     * @param chordsFound     Number of distinct chords detected
     */
    private void showResults(String key, String progression, int chordsFound) {
        // Populate summary stat cards
        tvDetectedKey.setText(key);
        tvChordCount.setText(String.valueOf(chordsFound));

        // Show summary and divider
        layoutResultSummary.setVisibility(View.VISIBLE);
        resultsDivider.setVisibility(View.VISIBLE);

        // Show the progression in the main text area
        tvResultsDisplay.setText("🎉 Detection complete!\n\n" + progression);
        tvResultsDisplay.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvResultsDisplay.setTextColor(getResources().getColor(R.color.text_dark, null));
    }

    /**
     * Toggles the UI between processing mode and idle mode.
     */
    private void setProcessingUI(boolean isProcessing) {
        layoutProgress.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        btnProcessAudio.setEnabled(!isProcessing);
        btnProcessAudio.setAlpha(isProcessing ? 0.5f : 1.0f);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result : "Unknown file";
    }
}