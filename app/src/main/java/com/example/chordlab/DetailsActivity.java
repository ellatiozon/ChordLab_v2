package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailsActivity extends AppCompatActivity {

    // Selected values
    private String selectedInstrument = "Guitar";   // default
    private String selectedGoal       = "20 mins";  // default

    // Instrument option views
    private LinearLayout optionGuitar, optionUkulele, optionPiano;
    private LinearLayout selectedInstrumentView;

    // Goal buttons
    private Button goal10, goal20, goal30, goal60;
    private Button selectedGoalBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // ── Bind views ──
        optionGuitar  = findViewById(R.id.optionGuitar);
        optionUkulele = findViewById(R.id.optionUkulele);
        optionPiano   = findViewById(R.id.optionPiano);

        goal10 = findViewById(R.id.goal10);
        goal20 = findViewById(R.id.goal20);
        goal30 = findViewById(R.id.goal30);
        goal60 = findViewById(R.id.goal60);

        // ── Default selections ──
        selectedInstrumentView = optionGuitar;
        selectedGoalBtn        = goal20;

        // ── Instrument click listeners ──
        optionGuitar.setOnClickListener(v  -> selectInstrument(optionGuitar,  "Guitar"));
        optionUkulele.setOnClickListener(v -> selectInstrument(optionUkulele, "Ukulele"));
        optionPiano.setOnClickListener(v   -> selectInstrument(optionPiano,   "Piano"));

        // ── Goal click listeners ──
        goal10.setOnClickListener(v -> selectGoal(goal10, "10 mins"));
        goal20.setOnClickListener(v -> selectGoal(goal20, "20 mins"));
        goal30.setOnClickListener(v -> selectGoal(goal30, "30 mins"));
        goal60.setOnClickListener(v -> selectGoal(goal60, "1 hr"));

        // ── Back button ──
        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());

        // ── Save & Continue ──
        findViewById(R.id.btnSaveContinue).setOnClickListener(v -> saveAndContinue());
    }

    // ── Instrument selection ─────────────────────────────────────────────────
    private void selectInstrument(LinearLayout selected, String name) {
        // Reset previous
        if (selectedInstrumentView != null) {
            selectedInstrumentView.setBackgroundResource(R.drawable.bg_instrument_normal);
            // Reset text colors inside to pink
            updateInstrumentTextColor(selectedInstrumentView, false);
        }

        // Apply selected style
        selected.setBackgroundResource(R.drawable.bg_instrument_selected);
        updateInstrumentTextColor(selected, true);

        selectedInstrumentView = selected;
        selectedInstrument     = name;
    }

    private void updateInstrumentTextColor(LinearLayout card, boolean isSelected) {
        // The title TextView is at position 1 inside the inner LinearLayout (position 1 of card)
        try {
            LinearLayout inner = (LinearLayout) card.getChildAt(1);
            android.widget.TextView title = (android.widget.TextView) inner.getChildAt(0);
            android.widget.TextView sub   = (android.widget.TextView) inner.getChildAt(1);
            title.setTextColor(isSelected
                    ? 0xFFFFFFFF
                    : getResources().getColor(R.color.accent_pink, null));
            sub.setTextColor(isSelected
                    ? 0xFFFFD0E8
                    : getResources().getColor(R.color.text_gray, null));
        } catch (Exception ignored) {}
    }

    // ── Goal selection ───────────────────────────────────────────────────────
    private void selectGoal(Button selected, String goal) {
        if (selectedGoalBtn != null) {
            selectedGoalBtn.setBackgroundResource(R.drawable.bg_sig_normal);
        }
        selected.setBackgroundResource(R.drawable.bg_sig_selected);
        selectedGoalBtn = selected;
        selectedGoal    = goal;
    }

    // ── Save to SharedPreferences and go to Dashboard ────────────────────────
    private void saveAndContinue() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        prefs.edit()
                .putString("instrument", selectedInstrument)
                .putString("dailyGoal",  selectedGoal)
                .putBoolean("detailsComplete", true)  // ← add this
                .apply();

        Toast.makeText(this, "Saved! Let's go 🎵", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}