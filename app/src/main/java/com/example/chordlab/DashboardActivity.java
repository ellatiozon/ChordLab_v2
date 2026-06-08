package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout selectedInstrumentCard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ── 1. UPDATE WELCOME TEXT ──
        TextView tvWelcomeName = findViewById(R.id.tvWelcomeName);
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        if (tvWelcomeName != null) {
            tvWelcomeName.setText("Welcome, " + username + "!");
        }

        // ── 2. INSTRUMENT CARDS ──
        LinearLayout cardGuitar = findViewById(R.id.cardGuitar);
        LinearLayout cardPiano  = findViewById(R.id.cardPiano);
        LinearLayout cardSax    = findViewById(R.id.cardSax);

        // Pre-select the saved instrument
        String savedInstrument = prefs.getString("instrument", "Guitar");
        switch (savedInstrument) {
            case "Piano":
                selectedInstrumentCard = cardPiano;
                cardPiano.setBackgroundResource(R.drawable.bg_instrument_selected);
                cardGuitar.setBackgroundResource(R.drawable.bg_instrument_normal);
                cardSax.setBackgroundResource(R.drawable.bg_instrument_normal);
                break;
            case "Ukulele":
                selectedInstrumentCard = cardSax;
                cardSax.setBackgroundResource(R.drawable.bg_instrument_selected);
                cardGuitar.setBackgroundResource(R.drawable.bg_instrument_normal);
                cardPiano.setBackgroundResource(R.drawable.bg_instrument_normal);
                break;
            default: // Guitar
                selectedInstrumentCard = cardGuitar;
                cardGuitar.setBackgroundResource(R.drawable.bg_instrument_selected);
                cardPiano.setBackgroundResource(R.drawable.bg_instrument_normal);
                cardSax.setBackgroundResource(R.drawable.bg_instrument_normal);
                break;
        }

        cardGuitar.setOnClickListener(v -> selectInstrument(cardGuitar, "Guitar"));
        cardPiano.setOnClickListener(v  -> selectInstrument(cardPiano,  "Piano"));
        cardSax.setOnClickListener(v    -> selectInstrument(cardSax,    "Ukulele"));

        // ── 3. PROFILE BUTTON (BAR AND ICON CONTAINER) ──
        View.OnClickListener openProfile = v -> {
            ProfileSheetFragment sheet = ProfileSheetFragment.newInstance();
            sheet.show(getSupportFragmentManager(), "profile");
        };

        findViewById(R.id.profileBar).setOnClickListener(openProfile);
        findViewById(R.id.profileIconContainer).setOnClickListener(openProfile);

        // ── 4. VIEW PAGER & PRACTICE MODE CARDS (FLASH RESTORED) ──
        ViewPager2 viewPager = findViewById(R.id.viewPagerPracticeModes);
        com.google.android.material.tabs.TabLayout tabIndicator = findViewById(R.id.tabIndicator);

        PracticeModesAdapter adapter = new PracticeModesAdapter((cardId, cardView) -> {
            if (cardId == R.id.cardPracticeMode) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    // startActivity(new Intent(this, PracticeModeActivity.class));
                });
            } else if (cardId == R.id.cardMetronome) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    startActivity(new Intent(this, MetronomeActivity.class));
                });
            } else if (cardId == R.id.cardFlashCards) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    // startActivity(new Intent(this, FlashCardsActivity.class));
                });
            } else if (cardId == R.id.cardSongPractice) {
                flashAndNavigate((LinearLayout) cardView, () -> {
                    startActivity(new Intent(this, SongListActivity.class)); // <-- Update this!
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

        if (viewPager != null && adapter != null) {
            viewPager.setAdapter(adapter);

            if (tabIndicator != null) {
                new com.google.android.material.tabs.TabLayoutMediator(tabIndicator, viewPager,
                        (tab, position) -> {
                            // Left empty intentionally for dot indicators
                        }
                ).attach();
            }
        }

        // ── 5. SHADOWS ──
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

    // ── HELPERS ─────────────────────────────────────────────────────────────

    private void selectInstrument(LinearLayout card, String instrumentName) {
        if (selectedInstrumentCard != null) {
            selectedInstrumentCard.setBackgroundResource(R.drawable.bg_instrument_normal);
        }
        card.setBackgroundResource(R.drawable.bg_instrument_selected);
        selectedInstrumentCard = card;

        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .putString("instrument", instrumentName)
                .apply();
    }

    // Restored flash logic for the main features!
    private void flashAndNavigate(LinearLayout card, Runnable navigateTo) {
        card.setBackgroundResource(R.drawable.bg_mode_selected);
        card.postDelayed(() -> {
            navigateTo.run();
            card.setBackgroundResource(R.drawable.bg_mode_normal);
        }, 200);
    }
}