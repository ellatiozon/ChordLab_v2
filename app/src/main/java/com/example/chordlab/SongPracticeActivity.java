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

    // We removed 'final' so users can dynamically change the speed
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

        // ─── Speed Control Listeners ───
        findViewById(R.id.btnSlower).setOnClickListener(v -> changeSpeed(-1));
        findViewById(R.id.btnFaster).setOnClickListener(v -> changeSpeed(1));

        // 1. Grab the chosen instrument
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentInstrument = prefs.getString("instrument", "Guitar");

        // 2. Get the song title passed from SongListActivity
        String songTitle = getIntent().getStringExtra("SONG_TITLE");
        if (songTitle == null) {
            songTitle = "Happy Birthday";
        }

        if (tvSongTitleHeader != null) {
            tvSongTitleHeader.setText(songTitle);
        }

        // 3. Build dynamic song text matching both the song AND the selected instrument!
        String rawSongText = "";

        switch (songTitle) {
            case "Tenzionado":
                rawSongText = "Instrument: " + currentInstrument + " · Key: G\n\n" +
                        getChordSpecsForTenzionado() + "\n\n" +
                        "         [G]                  [Em]\n" +
                        "Isang gabi na naman na lalong lumalamig\n" +
                        "         [C]                  [D]\n" +
                        "At ang aking mga mata'y nagnanais makasilip";
                break;

            case "A Thousand Miles":
                rawSongText = "Instrument: " + currentInstrument + " · Key: B\n\n" +
                        getChordSpecsForThousandMiles() + "\n\n" +
                        "         [B]                  [E]\n" +
                        "Making my way downtown, walking fast\n" +
                        "         [F#]                 [E]\n" +
                        "Faces pass and I'm homebound";
                break;

            case "Perfect":
                rawSongText = "Instrument: " + currentInstrument + " · Key: Ab (Capo 1)\n\n" +
                        getChordSpecsForPerfect() + "\n\n" +
                        "[Intro]\n" +
                        "          [G]\n\n" +
                        "[Verse 1]\n" +
                        "          [G]        [Em]\n" +
                        "I found a love for me\n" +
                        "              [C]                            [D]\n" +
                        "Darling, just dive right in, and follow my lead\n" +
                        "                [G]          [Em]\n" +
                        "Well, I found a girl beautiful and sweet\n" +
                        "        [C]                                     [D]\n" +
                        "I never knew you were the someone waiting for me\n\n" +
                        "[Pre-Chorus]\n" +
                        "                                [G]\n" +
                        "Cause we were just kids when we fell in love\n" +
                        "            [Em]                      [C]                [G]  [D]\n" +
                        "Not knowing what it was, I will not give you up this ti-ime\n" +
                        "             [G]                           [Em]\n" +
                        "Darling just kiss me slow, your heart is all I own\n" +
                        "            [C]                     [D]\n" +
                        "And in your eyes you're holding mine\n\n" +
                        "[Chorus]\n" +
                        "      [Em]   [C]             [G]          [D]              [Em]\n" +
                        "Baby, I'm dancing in the dark, with you between my arms\n" +
                        "[C]                [G]     [D]                [Em]\n" +
                        "Barefoot on the grass, listening to our favourite song\n" +
                        "          [C]                [G]                 [D]              [Em]\n" +
                        "When you said you looked a mess, I whispered underneath my breath\n" +
                        "         [C]                [G]        [D]          [G]\n" +
                        "But you heard it, darling you look perfect tonight\n\n" +
                        "[Verse 2]\n" +
                        "                [G]                    [Em]\n" +
                        "Well, I found a woman, stronger than anyone I know\n" +
                        "              [C]                                          [D]\n" +
                        "She shares my dreams, I hope that someday I'll share her home\n" +
                        "           [G]             [Em]\n" +
                        "I found a love, to carry more than just my secrets\n" +
                        "         [C]                              [D]\n" +
                        "To carry love, to carry children of our own\n\n" +
                        "[Pre-Chorus]\n" +
                        "                             [G]                     [Em]\n" +
                        "We are still kids, but we're so in love, fighting against all odds\n" +
                        "             [C]               [G]  [D]\n" +
                        "I know we'll be alright this ti-ime\n" +
                        "             [G]                              [Em]\n" +
                        "Darling just hold my hand, be my girl, I'll be your man\n" +
                        "         [C]               [D]\n" +
                        "I see my future in your eyes\n\n" +
                        "[Chorus]\n" +
                        "      [Em]   [C]              [G]         [D]              [Em]\n" +
                        "Baby, I'm dancing in the dark, with you between my arms\n" +
                        "[C]                [G]     [D]                [Em]\n" +
                        "Barefoot on the grass, listening to our favourite song\n" +
                        "        [C]                [G]                [D]\n" +
                        "When I saw you in that dress, looking so beautiful\n" +
                        "  [Em]       [C]                  [G]        [D]          [G]\n" +
                        "I don't deserve this, darling you look perfect tonight\n\n" +
                        "[Outro]\n" +
                        "  [G]       [C]           [D]                   [G]\n" +
                        "I don't deserve this, you look perfect tonight";
                break;

            case "Happy Birthday":
            default:
                rawSongText = "Instrument: " + currentInstrument + " · Key: A\n\n" +
                        getChordSpecsForHappyBirthday() + "\n\n" +
                        "         [A]                  [E]\n" +
                        "Happy Birthday to you\n" +
                        "         [E]                  [A]\n" +
                        "Happy Birthday to you\n" +
                        "         [A7]                 [D]\n" +
                        "Happy Birthday dear (name)\n" +
                        "         [A]         [E]      [A]\n" +
                        "Happy Birthday to you";
                break;
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
        // Keep speed within boundaries
        if (scrollPixels > MAX_SPEED) scrollPixels = MAX_SPEED;
        if (scrollPixels < MIN_SPEED) scrollPixels = MIN_SPEED;

        // Update the display text
        tvSpeedDisplay.setText(scrollPixels + "x");
    }

    // ─── AUTO SCROLL LOGIC ──────────────────────────────────────────────────
    private void toggleAutoScroll() {
        isAutoScrolling = !isAutoScrolling;
        if (isAutoScrolling) {
            btnAutoScroll.setImageResource(android.R.drawable.ic_media_pause);
            layoutSpeedControls.setVisibility(View.VISIBLE); // Reveal speed controls
            scrollHandler.post(autoScrollRunnable);
        } else {
            btnAutoScroll.setImageResource(android.R.drawable.ic_media_play);
            layoutSpeedControls.setVisibility(View.GONE); // Hide speed controls
            scrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private final Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoScrolling && songScrollView != null) {
                songScrollView.smoothScrollBy(0, scrollPixels); // Now uses dynamic variable!
                scrollHandler.postDelayed(this, SCROLL_DELAY_MS);
            }
        }
    };


    // ─── DYNAMIC CHORD SPEC HELPERS ─────────────────────────────────────────

    private String getChordSpecsForHappyBirthday() {
        if (currentInstrument.equals("Ukulele")) {
            return "(Ukulele Chords)\n[A]  2100\n[E]  1202\n[A7] 0100\n[D]  2220";
        } else if (currentInstrument.equals("Piano")) {
            return "(Piano Keys)\n[A]  A-C#-E\n[E]  E-G#-B\n[A7] A-C#-E-G\n[D]  D-F#-A";
        } else {
            return "(Guitar Frets)\n[A]  x02220\n[E]  022100\n[A7] x02020\n[D]  xx0232";
        }
    }

    private String getChordSpecsForTenzionado() {
        if (currentInstrument.equals("Ukulele")) {
            return "(Ukulele Chords)\n[G]  0232\n[Em] 0231\n[C]  0003\n[D]  2220";
        } else if (currentInstrument.equals("Piano")) {
            return "(Piano Keys)\n[G]  G-B-D\n[Em] E-G-B\n[C]  C-E-G\n[D]  D-F#-A";
        } else {
            return "(Guitar Frets)\n[G]  320033\n[Em] 022000\n[C]  x32010\n[D]  xx0232";
        }
    }

    private String getChordSpecsForThousandMiles() {
        if (currentInstrument.equals("Ukulele")) {
            return "(Ukulele Chords)\n[B]  4322\n[E]  1202\n[F#] 3121";
        } else if (currentInstrument.equals("Piano")) {
            return "(Piano Keys)\n[B]  B-D#-F#\n[E]  E-G#-B\n[F#] F#-A#-C#";
        } else {
            return "(Guitar Frets)\n[B]  x24442\n[E]  022100\n[F#] 244322";
        }
    }

    private String getChordSpecsForPerfect() {
        if (currentInstrument.equals("Ukulele")) {
            return "(Ukulele Chords)\n[G]  0232\n[Em] 0432\n[C]  0003\n[D]  2220";
        } else if (currentInstrument.equals("Piano")) {
            return "(Piano Keys)\n[G]  G-B-D\n[Em] E-G-B\n[C]  C-E-G\n[D]  D-F#-A";
        } else {
            return "(Guitar Frets)\n[G]  320033\n[Em] 022000\n[C]  x32010\n[D]  xx0232";
        }
    }

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
        // Clean chord name for filenames
        String cleanChord = chordName.toLowerCase().replace("#", "_sharp");
        String cleanInstrument = currentInstrument.toLowerCase();

        String imageName = cleanInstrument + "_" + cleanChord;
        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentInstrument + " - Chord " + chordName);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scrollHandler.removeCallbacks(autoScrollRunnable);
    }
}