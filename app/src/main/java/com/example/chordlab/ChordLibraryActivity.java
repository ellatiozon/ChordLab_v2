package com.example.chordlab;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class ChordLibraryActivity extends AppCompatActivity {

    // ── UI REFERENCES ───────────────────────────────────────────────────────
    private TextView tvHeaderTitle, tvChordDisplayTitle, tvPromptText;
    private LinearLayout layoutRootNotes, layoutQualities, layoutPianoMode;
    private HorizontalScrollView scrollQualities;
    private View divider2;
    private ImageView ivChordDiagram, btnPlayChordSound, btnBackToDashboard, ivFingeringGuide;

    // ── INTERNAL STATE tracking constants ──────────────────────────────────
    private String selectedInstrument = "guitar"; // Default fallback state
    private String currentRootNote = "C";         // Default matching selection asset
    private String currentQuality = "Major";      // Default selection text
    private String currentPianoMode = "Chord";    // "Note" or "Chord" (Only used for Piano)

    // Lists for generating selectors dynamically
    private final String[] rootNotes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    // USER REQUESTED CHANGE: Trimmed to only Major and Minor
    private final String[] chordQualities = {"Major", "Minor"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_library);

        Intent incomingIntent = getIntent();
        if (incomingIntent != null && incomingIntent.hasExtra("selected_instrument")) {
            selectedInstrument = incomingIntent.getStringExtra("selected_instrument").toLowerCase();
        }

        initializeViews();
        setupStaticListeners();

        // USER REQUESTED CHANGE: Check if piano to render Note/Chord toggle & hide guide
        if (selectedInstrument.equals("piano")) {
            layoutPianoMode.setVisibility(View.VISIBLE);
            tvPromptText.setText("CHOOSE A TARGET");
            generatePianoModeButtons();

            // Hide the fingering guide for Piano
            ivFingeringGuide.setVisibility(View.GONE);
        } else {
            // Ensure the guide is visible for Guitar and Ukulele
            ivFingeringGuide.setVisibility(View.VISIBLE);
        }

        generateRootNoteButtons();
        generateQualityButtons();
        updateChordDisplay();
    }

    private void initializeViews() {
        tvHeaderTitle        = findViewById(R.id.tvHeaderTitle);
        tvChordDisplayTitle  = findViewById(R.id.tvChordDisplayTitle);
        tvPromptText         = findViewById(R.id.tvPromptText);
        layoutRootNotes      = findViewById(R.id.layoutRootNotes);
        layoutQualities      = findViewById(R.id.layoutQualities);
        layoutPianoMode      = findViewById(R.id.layoutPianoMode);
        scrollQualities      = findViewById(R.id.scrollQualities);
        divider2             = findViewById(R.id.divider2);
        ivChordDiagram       = findViewById(R.id.ivChordDiagram);
        btnPlayChordSound    = findViewById(R.id.btnPlayChordSound);
        btnBackToDashboard   = findViewById(R.id.btnBackToDashboard);
        ivFingeringGuide     = findViewById(R.id.ivFingeringGuide); // Added reference here

        String displayInstrument = selectedInstrument.substring(0, 1).toUpperCase() + selectedInstrument.substring(1);
        tvHeaderTitle.setText(displayInstrument + " Library");
    }

    private void setupStaticListeners() {
        btnBackToDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(ChordLibraryActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        btnPlayChordSound.setOnClickListener(v -> {
            String soundResourceName;
            if (selectedInstrument.equals("piano") && currentPianoMode.equals("Note")) {
                soundResourceName = "piano_note_" + currentRootNote.toLowerCase().replace("#", "_sharp");
            } else {
                soundResourceName = selectedInstrument + "_" + currentRootNote.toLowerCase().replace("#", "_sharp") + "_" + currentQuality.toLowerCase();
            }
            Toast.makeText(this, "Playing: " + soundResourceName + ".mp3", Toast.LENGTH_SHORT).show();
        });
    }

    // ── PROGRAMMATIC SELECTION RENDERERS ────────────────────────────────────
    // USER REQUESTED CHANGE: Handles Piano Note vs Chord Toggle
    private void generatePianoModeButtons() {
        layoutPianoMode.removeAllViews();
        String[] modes = {"Note", "Chord"};
        for (String mode : modes) {
            AppCompatButton btn = createSelectionButton(mode, mode.equals(currentPianoMode));
            btn.setOnClickListener(v -> {
                currentPianoMode = mode;
                generatePianoModeButtons(); // Update highlight

                // If Note is selected, hide Major/Minor qualities
                if (currentPianoMode.equals("Note")) {
                    scrollQualities.setVisibility(View.GONE);
                    divider2.setVisibility(View.GONE);
                } else {
                    scrollQualities.setVisibility(View.VISIBLE);
                    divider2.setVisibility(View.VISIBLE);
                }
                updateChordDisplay();
            });
            layoutPianoMode.addView(btn);
        }
    }

    private void generateRootNoteButtons() {
        layoutRootNotes.removeAllViews();
        for (String note : rootNotes) {
            AppCompatButton btn = createSelectionButton(note, note.equals(currentRootNote));
            btn.setOnClickListener(v -> {
                currentRootNote = note;
                generateRootNoteButtons();
                updateChordDisplay();
            });
            layoutRootNotes.addView(btn);
        }
    }

    private void generateQualityButtons() {
        layoutQualities.removeAllViews();
        for (String quality : chordQualities) {
            AppCompatButton btn = createSelectionButton(quality, quality.equals(currentQuality));
            btn.setOnClickListener(v -> {
                currentQuality = quality;
                generateQualityButtons();
                updateChordDisplay();
            });
            layoutQualities.addView(btn);
        }
    }

    private AppCompatButton createSelectionButton(String text, boolean isSelected) {
        AppCompatButton button = new AppCompatButton(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 12, 0);
        button.setLayoutParams(params);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(spToPx());

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dpToPx());

        if (isSelected) {
            shape.setColor(Color.parseColor("#90CAF9"));
            button.setTextColor(Color.WHITE);
        } else {
            shape.setColor(Color.WHITE);
            shape.setStroke(3, Color.parseColor("#E91E63"));
            button.setTextColor(Color.parseColor("#E91E63"));
        }
        button.setBackground(shape);
        return button;
    }

    // ── DYNAMIC IMAGE ASSET LOADING MATRIX ──────────────────────────────────
    private void updateChordDisplay() {
        String cleanNoteName = currentRootNote.toLowerCase().replace("#", "_sharp");
        String cleanQualityName = currentQuality.toLowerCase();
        String imageName;

        // USER REQUESTED CHANGE: Logic route for Piano Notes vs Chords
        if (selectedInstrument.equals("piano") && currentPianoMode.equals("Note")) {
            tvChordDisplayTitle.setText(currentRootNote + " Note");
            // Targets images like: piano_note_c, piano_note_c_sharp, etc.
            imageName = "piano_note_" + cleanNoteName;
        } else {
            tvChordDisplayTitle.setText(currentRootNote + " " + currentQuality);
            // Targets images like: guitar_c_major, piano_g_sharp_minor, etc.
            imageName = selectedInstrument + "_" + cleanNoteName + "_" + cleanQualityName;
        }

        int resourceId = getResources().getIdentifier(imageName, "drawable", getPackageName());

        if (resourceId != 0) {
            ivChordDiagram.setImageResource(resourceId);
        } else {
            ivChordDiagram.setImageResource(R.drawable.placeholder_chord);
        }
    }

    // Density pixel unit helper methods
    private int dpToPx() { return (int) (12 * getResources().getDisplayMetrics().density); }
    private float spToPx() { return 16f; }
}