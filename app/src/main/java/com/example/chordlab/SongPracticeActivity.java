package com.example.chordlab;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SongPracticeActivity extends AppCompatActivity {

    private ScrollView songScrollView;
    private FloatingActionButton btnAutoScroll;
    private String currentInstrument = "Guitar"; // Default fallback

    // Auto-scroll Speed UI
    private View layoutSpeedControls;
    private TextView tvSpeedDisplay;

    // Auto-scroll variables
    private Handler scrollHandler = new Handler();
    private boolean isAutoScrolling = false;
    private final int SCROLL_DELAY_MS = 50;
    private int scrollPixels = 2;
    private final int MIN_SPEED = 1;
    private final int MAX_SPEED = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_practice);

        songScrollView = findViewById(R.id.songScrollView);
        TextView tvSongContent = findViewById(R.id.tvSongContent);
        TextView tvSongTitleHeader = findViewById(R.id.tvSongTitleHeader);
        btnAutoScroll = findViewById(R.id.btnAutoScroll);

        layoutSpeedControls = findViewById(R.id.layoutSpeedControls);
        tvSpeedDisplay = findViewById(R.id.tvSpeedDisplay);

        findViewById(R.id.btnBackSongList).setOnClickListener(v -> finish());
        findViewById(R.id.btnSlower).setOnClickListener(v -> changeSpeed(-1));
        findViewById(R.id.btnFaster).setOnClickListener(v -> changeSpeed(1));

        // Grab instrument preference
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentInstrument = prefs.getString("instrument", "Guitar");

        // Grab song title
        String songTitle = getIntent().getStringExtra("SONG_TITLE");
        if (songTitle == null) songTitle = "Happy Birthday";

        if (tvSongTitleHeader != null) {
            tvSongTitleHeader.setText(songTitle);
        }

        // ─── NEW JSON LOGIC ───
        // 1. Ask the database for the song
        SongData selectedSong = SongDatabase.getSongByTitle(this, songTitle);

        String rawSongText = "Song not found!";

        if (selectedSong != null) {
            // 2. Ask the song for the specific instrument details
            SongData.InstrumentDetails details = selectedSong.instruments.get(currentInstrument);

            if (details != null) {
                // 3. Piece it together perfectly
                rawSongText = "Instrument: " + currentInstrument + " · Key: " + details.key + "\n\n" +
                        details.specs + "\n\n" +
                        details.content;
            } else {
                rawSongText = "Tabs for " + currentInstrument + " are not available yet for this song.";
            }
        }

        if (tvSongContent != null) {
            tvSongContent.setText(formatChords(rawSongText));
            tvSongContent.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (btnAutoScroll != null) {
            btnAutoScroll.setOnClickListener(v -> toggleAutoScroll());
        }
    }

    // ─── SPEED CONTROL LOGIC ────────────────────────────────────────────────
    private void changeSpeed(int delta) {
        scrollPixels += delta;
        if (scrollPixels > MAX_SPEED) scrollPixels = MAX_SPEED;
        if (scrollPixels < MIN_SPEED) scrollPixels = MIN_SPEED;
        tvSpeedDisplay.setText(scrollPixels + "x");
    }

    // ─── AUTO SCROLL LOGIC ──────────────────────────────────────────────────
    private void toggleAutoScroll() {
        isAutoScrolling = !isAutoScrolling;
        if (isAutoScrolling) {
            btnAutoScroll.setImageResource(android.R.drawable.ic_media_pause);
            layoutSpeedControls.setVisibility(View.VISIBLE);
            scrollHandler.post(autoScrollRunnable);
        } else {
            btnAutoScroll.setImageResource(android.R.drawable.ic_media_play);
            layoutSpeedControls.setVisibility(View.GONE);
            scrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private final Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoScrolling && songScrollView != null) {
                songScrollView.smoothScrollBy(0, scrollPixels);
                scrollHandler.postDelayed(this, SCROLL_DELAY_MS);
            }
        }
    };

    // ─── CHORD FORMATTER ────────────────────────────────────────────────────
    private SpannableStringBuilder formatChords(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(text);

        int offset = 0;
        while (matcher.find()) {
            int start = matcher.start() - offset;
            int end = matcher.end() - offset;
            String chordName = matcher.group(1);

            builder.replace(start, end, chordName);
            int newEnd = start + chordName.length();

            builder.setSpan(new BackgroundColorSpan(Color.parseColor("#E0E0E0")), start, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    showChordImageDialog(chordName);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(Color.BLACK);
                    ds.setFakeBoldText(true);
                }
            };
            builder.setSpan(clickableSpan, start, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            offset += 2;
        }
        return builder;
    }

    // ─── AUTOMATIC IMAGE POPUP DIALOG ───────────────────────────────────────
    private void showChordImageDialog(String chordName) {
        String cleanInstrument = currentInstrument.toLowerCase();

        // 1. CHORD PARSER: Break down strings like "C#m" into Note and Quality
        String note = "C";
        String quality = "Major";
        String pianoMode = "Chord";

        String parsed = chordName.trim();
        if (parsed.length() > 0) {
            // Extract the Root letter (e.g., "C", "D")
            note = String.valueOf(parsed.charAt(0)).toUpperCase();

            // Check for sharps or flats right after the root letter
            if (parsed.length() > 1) {
                char accidental = parsed.charAt(1);
                if (accidental == '#') {
                    note += "#";
                } else if (accidental == 'b') {
                    // Safe Translation: If a song specifies a flat, map it to our Sharp system
                    switch (note) {
                        case "D": note = "C#"; break;
                        case "E": note = "D#"; break;
                        case "G": note = "F#"; break;
                        case "A": note = "G#"; break;
                        case "B": note = "A#"; break;
                    }
                }
            }
        }

        // Determine Piano Mode & Quality
        if (parsed.toLowerCase().contains("note")) {
            pianoMode = "Note";
        } else if (parsed.toLowerCase().contains("m") && !parsed.toLowerCase().contains("maj")) {
            quality = "Minor";
        }

        // 2. Fetch exact asset name mapped to your custom convention
        String imageName = getResourceName(cleanInstrument, note, quality, pianoMode);

        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentInstrument + " - " + chordName);

        if (imageResId != 0) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(imageResId);
            imageView.setPadding(30, 30, 30, 30);
            builder.setView(imageView);
        } else {
            builder.setMessage("Chart layout for " + chordName + " is ready!\nImport an image named '" + imageName + "' to display it here.");
        }

        builder.setPositiveButton("Got it", null);
        builder.show();
    }

    // ─── CUSTOM ASSET NAMING MAPPER (Same as ChordLibrary) ──────────────────
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
                    case "G#": return "gsharp_a_flat"; // Exact string match
                    case "A":  return "a_note";
                    case "A#": return "asharp_bflat_note"; // Exact string match
                    case "B":  return "b_note";
                }
            } else { // Piano Chord Mode
                switch (note) {
                    case "C":  return "c_" + cleanQuality;
                    case "C#": return "c_sharp_" + cleanQuality;
                    case "D":  return "d_" + cleanQuality;
                    // Fallback for songs that might include a D# chord notation
                    case "D#": return "d_sharp_" + cleanQuality;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scrollHandler.removeCallbacks(autoScrollRunnable);
    }
}