package com.example.chordlab;

/**
 * ChordLab: Polyphonic Note and Chord Detection System
 * Core component of the ChordLab backend architecture,
 * handling AI processing, multimodal sensor fusion, and/or state management.
 *
 * @author Mikhaella Mari D. Tiozon
 * @version 1.0
 * @since 2026-04-17
 * * Note: The algorithmic logic, machine learning integration, and database
 * architecture contained within this file are the original intellectual
 * property of the author.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity {

    private LinearLayout cardGuitar, cardPiano, cardSax;
    private LinearLayout selectedInstrumentCard = null;

    private String selectedInstrument = "Guitar";

    private DatabaseHelper myDb;
    private ProgressBar progressGuitar;
    private TextView tvProgressLevel, tvProgressExp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDb = new DatabaseHelper(this);

        // ── 1. INITIALIZE WIDGETS & USER WELCOME TEXT ──
        cardGuitar = findViewById(R.id.cardGuitar);
        cardPiano  = findViewById(R.id.cardPiano);
        cardSax    = findViewById(R.id.cardUkelele); // Synchronized UI element ID

        progressGuitar  = findViewById(R.id.progressGuitar);
        tvProgressLevel = findViewById(R.id.tvProgressLevel);
        tvProgressExp   = findViewById(R.id.tvProgressExp);

        TextView tvWelcomeName = findViewById(R.id.tvWelcomeName);
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        if (tvWelcomeName != null) {
            tvWelcomeName.setText("Welcome, " + username + "!");
        }

        // ── 2. INSTRUMENT SELECTION ACTION LISTENERS ──
        if (cardGuitar != null) cardGuitar.setOnClickListener(v -> selectInstrument(cardGuitar, "Guitar"));
        if (cardPiano != null)  cardPiano.setOnClickListener(v -> selectInstrument(cardPiano, "Piano"));
        if (cardSax != null)    cardSax.setOnClickListener(v -> selectInstrument(cardSax, "Ukulele"));

        // ── 3. BOTTOM PROFILE NAVIGATION SHEET TRIGGER ──
        View.OnClickListener openProfile = v -> {
            ProfileSheetFragment sheet = ProfileSheetFragment.newInstance();
            sheet.show(getSupportFragmentManager(), "profile");
        };

        View profileBar = findViewById(R.id.profileBar);
        View profileIconContainer = findViewById(R.id.profileIconContainer);
        if (profileBar != null) profileBar.setOnClickListener(openProfile);
        if (profileIconContainer != null) profileIconContainer.setOnClickListener(openProfile);

        // ── 4. VIEW PAGER CAROUSEL ADAPTER (PRACTICE PAGES 1 & 2) ──
        ViewPager2 viewPager = findViewById(R.id.viewPagerPracticeModes);
        com.google.android.material.tabs.TabLayout tabIndicator = findViewById(R.id.tabIndicator);

        PracticeModesAdapter adapter = new PracticeModesAdapter((cardId, cardView) -> {
            if (cardId == R.id.cardPracticeMode) {
                flashAndNavigate((LinearLayout) cardView, () -> startSession("PRACTICE"));
            } else if (cardId == R.id.cardFlashCards) {
                flashAndNavigate((LinearLayout) cardView, () -> startSession("FLASHCARDS"));
            } else if (cardId == R.id.cardMetronome) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    startActivity(new Intent(this, MetronomeActivity.class));
                });
            } else if (cardId == R.id.cardSongPractice) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    startActivity(new Intent(this, SongListActivity.class));
                });
            } else if (cardId == R.id.cardChordLibrary) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    SharedPreferences currentPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                    String activeInstrument = currentPrefs.getString("instrument", "Guitar");

                    Intent intent = new Intent(this, ChordLibraryActivity.class);
                    intent.putExtra("selected_instrument", activeInstrument);
                    startActivity(intent);
                });
            } else if (cardId == R.id.cardVideoTranslator) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    startActivity(new Intent(this, VideoTranslatorActivity.class));
                });
            }
        });

        if (viewPager != null) {
            viewPager.setAdapter(adapter);
            if (tabIndicator != null) {
                new com.google.android.material.tabs.TabLayoutMediator(tabIndicator, viewPager,
                        (tab, position) -> { /* Custom Dot indicator configuration */ }
                ).attach();
            }
        }

        // ── 5. ENGINE DECORATOR LAYER SHADOWS ──
        setupCustomShadows();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyInstrumentHighlight();
        applyProgressDisplay();
    }

    // ── INTERACTIVE ROUTER ENGINES ──────────────────────────────────────────

    private void startSession(String mode) {
        if (selectedInstrument.isEmpty()) {
            Toast.makeText(this, "Please select an instrument first!", Toast.LENGTH_SHORT).show();
            return;
        }

        Class<?> targetActivity;
        switch (selectedInstrument.toUpperCase()) {
            case "GUITAR":
                targetActivity = GuitarActivity.class;
                break;
            case "UKULELE":
                targetActivity = UkuleleActivity.class;
                break;
            default:
                targetActivity = PianoActivity.class;
                break;
        }

        Intent intent = new Intent(this, targetActivity);
        intent.putExtra("SESSION_MODE", mode);
        startActivity(intent);
    }

    private void selectInstrument(LinearLayout card, String instrumentName) {
        if (selectedInstrumentCard != null) {
            selectedInstrumentCard.setBackgroundResource(R.drawable.bg_instrument_normal);
        }
        card.setBackgroundResource(R.drawable.bg_instrument_selected);
        selectedInstrumentCard = card;

        selectedInstrument = instrumentName;

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        prefs.edit().putString("instrument", instrumentName).apply();
        String username = prefs.getString("username", "");
        if (!username.isEmpty()) {
            String currentGoal = prefs.getString("dailyGoal", "20 mins");
            myDb.updateUserDetails(username, instrumentName, currentGoal);
        }
    }

    private void applyInstrumentHighlight() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (cardGuitar != null) cardGuitar.setBackgroundResource(R.drawable.bg_instrument_normal);
        if (cardPiano != null)  cardPiano.setBackgroundResource(R.drawable.bg_instrument_normal);
        if (cardSax != null)    cardSax.setBackgroundResource(R.drawable.bg_instrument_normal);
        selectedInstrumentCard = null;

        String savedInstrument = "Guitar";
        Cursor cursor = myDb.getUserData(username);
        if (cursor != null && cursor.moveToFirst()) {
            int col = cursor.getColumnIndex("INSTRUMENT");
            if (col != -1) {
                String dbInstrument = cursor.getString(col);
                if (dbInstrument != null && !dbInstrument.isEmpty()) {
                    savedInstrument = dbInstrument;
                }
            }
            cursor.close();
        }

        prefs.edit().putString("instrument", savedInstrument).apply();
        selectedInstrument = savedInstrument;

        switch (savedInstrument) {
            case "Piano":
                if (cardPiano != null) {
                    cardPiano.setBackgroundResource(R.drawable.bg_instrument_selected);
                    selectedInstrumentCard = cardPiano;
                }
                break;
            case "Ukulele":
                if (cardSax != null) {
                    cardSax.setBackgroundResource(R.drawable.bg_instrument_selected);
                    selectedInstrumentCard = cardSax;
                }
                break;
            default:
                if (cardGuitar != null) {
                    cardGuitar.setBackgroundResource(R.drawable.bg_instrument_selected);
                    selectedInstrumentCard = cardGuitar;
                }
                break;
        }
    }

    private void applyProgressDisplay() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        if (username.isEmpty()) return;

        int[] expLevel = myDb.getExpAndLevel(username);
        int exp   = expLevel[0];
        int level = expLevel[1];

        if (progressGuitar != null) progressGuitar.setProgress(exp);
        if (tvProgressLevel != null) tvProgressLevel.setText("Level " + level);
        if (tvProgressExp != null) tvProgressExp.setText(exp + " / 100 XP to Level " + (level + 1));
    }

    private void flashAndNavigate(LinearLayout card, Runnable navigateTo) {
        card.setBackgroundResource(R.drawable.bg_mode_selected);
        card.postDelayed(() -> {
            navigateTo.run();
            card.setBackgroundResource(R.drawable.bg_mode_normal);
        }, 200);
    }

    private void setupCustomShadows() {
        View profileBar = findViewById(R.id.profileBar);
        if (profileBar != null) {
            profileBar.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    float radius = 40f * view.getResources().getDisplayMetrics().density;
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight() + (int) radius, radius);
                }
            });
            profileBar.setClipToOutline(false);
        }

        View profileCircleBg = findViewById(R.id.profileCircleBg);
        if (profileCircleBg != null) {
            profileCircleBg.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            profileCircleBg.setClipToOutline(false);
        }
    }
}