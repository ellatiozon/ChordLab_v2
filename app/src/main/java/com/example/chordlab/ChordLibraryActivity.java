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
    private TextView tvHeaderTitle, tvPromptText;
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
    private final String[] chordQualities = {"Major", "Minor"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_library);

        // HARDWARE VOLUME FIX: Forces hardware volume buttons to control music/media volume directly
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);

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
        tvPromptText         = findViewById(R.id.tvPromptText);
        layoutRootNotes      = findViewById(R.id.layoutRootNotes);
        layoutQualities      = findViewById(R.id.layoutQualities);
        layoutPianoMode      = findViewById(R.id.layoutPianoMode);
        scrollQualities      = findViewById(R.id.scrollQualities);
        divider2             = findViewById(R.id.divider2);
        ivChordDiagram       = findViewById(R.id.ivChordDiagram);
        btnPlayChordSound    = findViewById(R.id.btnPlayChordSound);
        btnBackToDashboard   = findViewById(R.id.btnBackToDashboard);
        ivFingeringGuide     = findViewById(R.id.ivFingeringGuide);

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
            // Fetch exact asset name mapped to user's convention
            String soundResourceName = getResourceName(selectedInstrument, currentRootNote, currentQuality, currentPianoMode);

            int resId = getResources().getIdentifier(soundResourceName, "raw", getPackageName());

            if (resId != 0) {
                android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, resId);
                mediaPlayer.setVolume(1.0f, 1.0f); // Max Volume
                mediaPlayer.setOnCompletionListener(android.media.MediaPlayer::release);
                mediaPlayer.start();
            } else {
                Toast.makeText(this, "Audio file not found: " + soundResourceName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── PROGRAMMATIC SELECTION RENDERERS ────────────────────────────────────
    private void generatePianoModeButtons() {
        layoutPianoMode.removeAllViews();
        String[] modes = {"Note", "Chord"};
        for (String mode : modes) {
            AppCompatButton btn = createSelectionButton(mode, mode.equals(currentPianoMode));
            btn.setOnClickListener(v -> {
                currentPianoMode = mode;

                // Safety Fix: If D# is selected and user switches to Chord mode, reset to C
                // because D# Major/Minor do not exist.
                if (currentPianoMode.equals("Chord") && currentRootNote.equals("D#")) {
                    currentRootNote = "C";
                }

                generatePianoModeButtons();
                generateRootNoteButtons(); // Regenerate notes to hide/show D#

                if (currentPianoMode.equals("Note")) {
                    scrollQualities.setVisibility(View.GONE);
                    divider2.setVisibility(View.GONE);
                } else {
                    scrollQualities.setVisibility(View.VISIBLE);
                    divider2.setVisibility(View.VISIBLE);
                }

                generateQualityButtons();
                updateChordDisplay();
            });
            layoutPianoMode.addView(btn);
        }
    }

    private void generateRootNoteButtons() {
        layoutRootNotes.removeAllViews();

        // CUSTOM INSTRUMENT FILTERING
        String[] notesToRender;
        if (selectedInstrument.equals("guitar")) {
            notesToRender = new String[]{"A", "C", "D", "E", "G"};
        } else if (selectedInstrument.equals("ukulele")) {
            notesToRender = new String[]{"A", "C", "D", "E", "F", "G"};
        } else {
            if (currentPianoMode.equals("Chord")) {
                // Exclude D# completely from Piano Chords
                notesToRender = new String[]{"C", "C#", "D", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
            } else {
                // Include all 12 notes (including D# Note)
                notesToRender = rootNotes;
            }
        }

        for (String note : notesToRender) {
            AppCompatButton btn = createSelectionButton(note, note.equals(currentRootNote));
            btn.setOnClickListener(v -> {
                currentRootNote = note;

                // Safety check: Reset quality to Major if switching to a restricted root note
                if (selectedInstrument.equals("guitar")) {
                    // Guitar hides minor for C and G
                    if ((currentRootNote.equals("C") || currentRootNote.equals("G")) && currentQuality.equals("Minor")) {
                        currentQuality = "Major";
                    }
                } else if (selectedInstrument.equals("ukulele")) {
                    // Ukulele ONLY hides minor for F
                    if (currentRootNote.equals("F")) {
                        currentQuality = "Major";
                    }
                }

                generateRootNoteButtons();
                generateQualityButtons();
                updateChordDisplay();
            });
            layoutRootNotes.addView(btn);
        }
    }

    private void generateQualityButtons() {
        layoutQualities.removeAllViews();

        // CUSTOM QUALITY FILTERING
        String[] qualitiesToRender;
        if (selectedInstrument.equals("guitar") && (currentRootNote.equals("C") || currentRootNote.equals("G"))) {
            qualitiesToRender = new String[]{"Major"}; // Only Major for Guitar C & G
        } else if (selectedInstrument.equals("ukulele") && currentRootNote.equals("F")) {
            qualitiesToRender = new String[]{"Major"}; // FIXED: Only hide Minor for Ukulele F note (G note can be Minor)
        } else {
            qualitiesToRender = chordQualities; // Major and Minor for everything else
        }

        for (String quality : qualitiesToRender) {
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
        // Fetch exact asset name mapped to user's convention
        String imageName = getResourceName(selectedInstrument, currentRootNote, currentQuality, currentPianoMode);

        int resourceId = getResources().getIdentifier(imageName, "drawable", getPackageName());

        if (resourceId != 0) {
            ivChordDiagram.setImageResource(resourceId);
        } else {
            ivChordDiagram.setImageResource(R.drawable.placeholder_chord);
        }
    }

    // ── CUSTOM ASSET NAMING MAPPER ──────────────────────────────────────────
    private String getResourceName(String instrument, String note, String quality, String pianoMode) {
        String cleanQuality = quality.toLowerCase(); // "major" or "minor"

        if (instrument.equals("piano")) {
            if (pianoMode.equals("Note")) {
                switch (note) {
                    case "C":  return "c_note";
                    case "C#": return "csharp_dflat_note";
                    case "D":  return "d_note";
                    case "D#": return "dsharp_eflat_note";
                    case "E":  return "e_note";
                    case "F":  return "f_note";
                    case "F#": return "fsharp_gflat_note";
                    case "G":  return "g_note";
                    case "G#": return "gsharp_aflat_note";
                    case "A":  return "a_note";
                    case "A#": return "asharp_bflat_note";
                    case "B":  return "b_note";
                }
            } else { // Piano Chord Mode
                switch (note) {
                    case "C":  return "c_" + cleanQuality;
                    case "C#": return "c_sharp_" + cleanQuality;
                    case "D":  return "d_" + cleanQuality;
                    // D# is omitted from UI during Chord mode, so it won't trigger here
                    case "E":  return "e_" + cleanQuality;
                    case "F":  return "f_" + cleanQuality;
                    case "F#": return "f_sharp_" + cleanQuality;
                    case "G":  return "g_" + cleanQuality;
                    case "G#": return "a_flat_" + cleanQuality;
                    case "A":  return "a_" + cleanQuality;
                    case "A#": return "b_flat_" + cleanQuality;
                    case "B":  return "b_" + cleanQuality;
                }
            }
        }

        // Mapping specifically for Guitar and Ukulele formulas
        String mappedNote = "";
        switch (note) {
            case "C":  mappedNote = "c"; break;
            case "C#": mappedNote = "c_sharp"; break;
            case "D":  mappedNote = "d"; break;
            case "D#": mappedNote = "d_sharp"; break;
            case "E":  mappedNote = "e"; break;
            case "F":  mappedNote = "f"; break;
            case "F#": mappedNote = "f_sharp"; break;
            case "G":  mappedNote = "g"; break;
            case "G#": mappedNote = "g_sharp"; break;
            case "A":  mappedNote = "a"; break;
            case "A#": mappedNote = "a_sharp"; break;
            case "B":  mappedNote = "b"; break;
        }

        if (instrument.equals("guitar")) {
            return mappedNote + "_" + cleanQuality + "_guitar";
        } else if (instrument.equals("ukulele")) {
            return mappedNote + "_" + cleanQuality + "_uku";
        }

        return "";
    }

    private int dpToPx() { return (int) (12 * getResources().getDisplayMetrics().density); }
    private float spToPx() { return 16f; }
}