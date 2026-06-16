package com.example.chordlab;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class ProgressReportSheetFragment extends BottomSheetDialogFragment {

    private DatabaseHelper myDb;

    public static ProgressReportSheetFragment newInstance() {
        return new ProgressReportSheetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Reuses your existing XML progress report layout!
        return inflater.inflate(R.layout.activity_progress_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        myDb = new DatabaseHelper(requireContext());

        // ── 1. GET ACTIVE SESSION USERNAME ──
        SharedPreferences prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        // ── 2. BIND LAYOUT UI ELEMENTS ──
        TextView tvTotalMinutes = view.findViewById(R.id.tvTotalPracticeMinutes);

        TextView tvChordsPercentText = view.findViewById(R.id.tvChordsLearnedPercent);
        ProgressBar pbChordsLearned = view.findViewById(R.id.chordsLearned);

        TextView tvInstrumentsPercentText = view.findViewById(R.id.tvInstrumentsExploredPercent);
        ProgressBar pbInstrumentsExplored = view.findViewById(R.id.instrumentsExplored);

        TextView tvGuitarPercentText = view.findViewById(R.id.tvGuitarProgressPercent);
        ProgressBar pbGuitarProgress = view.findViewById(R.id.guitarProgress);

        TextView tvUkulelePercentText = view.findViewById(R.id.tvUkuleleProgressPercent);
        ProgressBar pbUkuleleProgress = view.findViewById(R.id.ukuleleProgress);

        TextView tvPianoPercentText = view.findViewById(R.id.tvPianoProgressPercent);
        ProgressBar pbPianoProgress = view.findViewById(R.id.pianoProgress);

        // ── 3. QUERY PERSISTENT STORAGE AND COMPUTE PERCENTAGES ──
        if (!username.isEmpty()) {
            DatabaseHelper.ProgressData progress = myDb.getUserProgress(username);

            // Set Total Practice Minutes Text
            if (tvTotalMinutes != null) {
                tvTotalMinutes.setText(String.format(Locale.getDefault(), "%d mins", progress.totalPracticeMinutes));
            }

            // A. Chords Learned Tracker (Overall total benchmarked against an aggregate cap like 30)
            int totalChordsBenchmark = 30;
            int totalChordsLearned = progress.guitarChords + progress.ukuleleChords + progress.pianoChords;
            int overallChordsPercent = Math.min(100, (totalChordsLearned * 100) / totalChordsBenchmark);
            if (pbChordsLearned != null) pbChordsLearned.setProgress(overallChordsPercent);
            if (tvChordsPercentText != null) tvChordsPercentText.setText(String.format(Locale.getDefault(), "%d%%", overallChordsPercent));

            // B. Instruments Explored (Out of 3 maximum total: Guitar, Ukulele, Piano)
            int instrumentsPercent = Math.min(100, (progress.instrumentsExploredCount * 100) / 3);
            if (pbInstrumentsExplored != null) pbInstrumentsExplored.setProgress(instrumentsPercent);
            if (tvInstrumentsPercentText != null) tvInstrumentsPercentText.setText(String.format(Locale.getDefault(), "%d%%", instrumentsPercent));

            // C. Guitar Milestone Progress (Assuming a standard pool milestone of 10 target elements)
            int guitarPercent = Math.min(100, (progress.guitarChords * 100) / 10);
            if (pbGuitarProgress != null) pbGuitarProgress.setProgress(guitarPercent);
            if (tvGuitarPercentText != null) tvGuitarPercentText.setText(String.format(Locale.getDefault(), "%d%%", guitarPercent));

            // D. Ukulele Milestone Progress (Assuming a standard pool milestone of 10 target elements)
            int ukulelePercent = Math.min(100, (progress.ukuleleChords * 100) / 10);
            if (pbUkuleleProgress != null) pbUkuleleProgress.setProgress(ukulelePercent);
            if (tvUkulelePercentText != null) tvUkulelePercentText.setText(String.format(Locale.getDefault(), "%d%%", ukulelePercent));

            // E. Piano Composite Milestone Progress (Combines notes + chords learned, benchmarked out of 15)
            int pianoCombinedLearned = progress.pianoChords + progress.pianoNotes;
            int pianoPercent = Math.min(100, (pianoCombinedLearned * 100) / 15);
            if (pbPianoProgress != null) pbPianoProgress.setProgress(pianoPercent);
            if (tvPianoPercentText != null) tvPianoPercentText.setText(String.format(Locale.getDefault(), "%d%%", pianoPercent));
        }

        // ── USER REQUESTED CHANGE: REDIRECTS BACK TO PROFILE MENU ──
        View btnBack = view.findViewById(R.id.btnBackProgressReport);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dismiss(); // Dismisses this progress report sheet cleanly

                // Instantly slides the Profile menu fragment back up!
                ProfileSheetFragment profileSheet = ProfileSheetFragment.newInstance();
                profileSheet.show(getParentFragmentManager(), "profile_sheet");
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ── FORCE THE SHEET TO POP UP TALLER (FULLY EXPANDED) ──
        Dialog dialog = getDialog();
        if (dialog != null) {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                // Tells Android to slide open all the way immediately
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Prevents it from getting stuck halfway if the user drags it slightly
                behavior.setSkipCollapsed(true);
            }
        }
    }
}