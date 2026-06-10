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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Load data from SharedPreferences ──
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);

        String username   = prefs.getString("username",   "Username");
        String email      = prefs.getString("email",      "username@gmail.com");
        String instrument = prefs.getString("instrument", "Guitar");
        String dailyGoal  = prefs.getString("dailyGoal",  "20 mins");

        // ── Populate views ──
        TextView tvUsername    = view.findViewById(R.id.tvUsername);
        TextView tvEmail       = view.findViewById(R.id.tvEmail);
        TextView tvInstrument  = view.findViewById(R.id.tvInstrumentStatus);

        tvUsername.setText(username);
        tvEmail.setText(email);
        tvInstrument.setText(instrument);

        // ── Button listeners ──
        view.findViewById(R.id.btnSettings).setOnClickListener(v ->
                Toast.makeText(getContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnFaqs).setOnClickListener(v ->
                Toast.makeText(getContext(), "FAQs coming soon!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnSupport).setOnClickListener(v ->
                Toast.makeText(getContext(), "Support coming soon!", Toast.LENGTH_SHORT).show());


        // ── REDIRECT TO PROGRESS REPORT (SLIDING SHEET) ──
        view.findViewById(R.id.cardProgressReport).setOnClickListener(v -> {
            dismiss(); // Slide down the profile sheet smoothly

            // Slide up the brand new taller Progress Report sheet!
            ProgressReportSheetFragment progressSheet = ProgressReportSheetFragment.newInstance();
            progressSheet.show(getParentFragmentManager(), "progress_report");
        });


        // ── REDIRECT TO DAILY GOAL (FIXED FOR SLIDING SHEET) ──
        view.findViewById(R.id.cardDailyGoal).setOnClickListener(v -> {
            dismiss(); // Slide down the profile sheet smoothly

            // Slide up the brand new taller Daily Goal sheet!
            DailyGoalSheetFragment dailyGoalSheet = DailyGoalSheetFragment.newInstance();
            dailyGoalSheet.show(getParentFragmentManager(), "daily_goal");
        });

        // ── Log Out ──
        view.findViewById(R.id.btnLogOut).setOnClickListener(v -> {
            // Clear SharedPreferences (instrument, dailyGoal, etc.)
            prefs.edit().clear().apply();

            // Clear SessionManager (isLoggedIn, username, email)
            new SessionManager(requireContext()).clearSession();

            dismiss();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}