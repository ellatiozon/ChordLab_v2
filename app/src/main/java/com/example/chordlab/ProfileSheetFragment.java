package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProfileSheetFragment extends BottomSheetDialogFragment {

    public static ProfileSheetFragment newInstance() {
        return new ProfileSheetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_sheet, container, false);
    }

    // ─── REPLACE EVERYTHING FROM HERE DOWN TO THE END OF THE FILE ───
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize your SessionManager instead of raw SharedPreferences
        SessionManager sessionManager = new SessionManager(requireContext());

        // 2. Pull the values dynamically using your SessionManager methods
        String username   = sessionManager.getUsername();
        String email      = sessionManager.getEmail();

        // Note: If instrument and dailyGoal are stored in a different file, keep this block.
        SharedPreferences legacyPrefs = requireActivity()
                .getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String instrument = legacyPrefs.getString("instrument", "Guitar");

        // 3. Populate views with the dynamic data
        TextView tvUsername    = view.findViewById(R.id.tvUsername);
        TextView tvEmail       = view.findViewById(R.id.tvEmail);
        TextView tvInstrument  = view.findViewById(R.id.tvInstrumentStatus);

        tvUsername.setText(username.isEmpty() ? "Username" : username);
        tvEmail.setText(email.isEmpty() ? "username@gmail.com" : email);
        tvInstrument.setText(instrument);

        // ── Button listeners ──
        view.findViewById(R.id.btnSettings).setOnClickListener(v ->
                Toast.makeText(getContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnFaqs).setOnClickListener(v ->
                Toast.makeText(getContext(), "FAQs coming soon!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnSupport).setOnClickListener(v ->
                Toast.makeText(getContext(), "Support coming soon!", Toast.LENGTH_SHORT).show());

        // ── REDIRECT TO PROGRESS REPORT ──
        view.findViewById(R.id.cardProgressReport).setOnClickListener(v -> {
            dismiss();
            ProgressReportSheetFragment progressSheet = ProgressReportSheetFragment.newInstance();
            progressSheet.show(getParentFragmentManager(), "progress_report");
        });

        // ── REDIRECT TO DAILY GOAL ──
        view.findViewById(R.id.cardDailyGoal).setOnClickListener(v -> {
            dismiss();
            DailyGoalSheetFragment dailyGoalSheet = DailyGoalSheetFragment.newInstance();
            dailyGoalSheet.show(getParentFragmentManager(), "daily_goal");
        });

        // ── Log Out (Cleaned up to use your SessionManager properly) ──
        view.findViewById(R.id.btnLogOut).setOnClickListener(v -> {
            // Clear the SessionManager session ("ChordLabSession" file)
            sessionManager.clearSession();

            // Also clear legacy file if you stored instrument data there
            legacyPrefs.edit().clear().apply();

            dismiss();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}