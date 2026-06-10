package com.example.chordlab;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class DailyGoalSheetFragment extends BottomSheetDialogFragment {

    private static final String[][] GOAL_POOL = {
            {"Warm Up Goals",           "5"},
            {"Practice C Major Chord",  "10"},
            {"Flash Card Review",       "10"},
            {"Metronome Drill",         "5"},
            {"Scale Run Practice",      "10"},
            {"Chord Transition Drill",  "10"},
            {"Finger Stretching",       "5"},
            {"Rhythm Clapping",         "5"},
            {"Song Section Practice",   "15"},
            {"Ear Training",            "10"},
            {"Sight Reading",           "10"},
            {"Improvisation Exercise",  "10"},
    };

    private static final int TASKS_PER_DAY = 4;

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView tvProgressPercent, tvMinutesPracticed;
    private TextView tvCurrentStreak, tvBestStreak;
    private CircularProgressView circularProgress;
    private LinearLayout taskContainer;

    // ── State ────────────────────────────────────────────────────────────────
    private List<String[]> todayTasks;   // [name, minutes]
    private boolean[] taskCompleted;
    private SharedPreferences prefs;
    private String todayKey;
    private int totalGoalMinutes;

    public static DailyGoalSheetFragment newInstance() {
        return new DailyGoalSheetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_daily_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences("DailyGoalPrefs", Context.MODE_PRIVATE);
        todayKey = getTodayKey();

        // ── Bind views from inflated layout ──
        tvProgressPercent  = view.findViewById(R.id.tvProgressPercent);
        tvMinutesPracticed = view.findViewById(R.id.tvMinutesPracticed);
        circularProgress   = view.findViewById(R.id.circularProgress);
        taskContainer      = view.findViewById(R.id.taskContainer);
        tvCurrentStreak    = view.findViewById(R.id.tvCurrentStreak);
        tvBestStreak       = view.findViewById(R.id.tvBestStreak);

        // ── BACK BUTTON: REDIRECTS BACK TO PROFILE MENU ──
        View btnBack = view.findViewById(R.id.btnBackDailyGoal);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dismiss(); // Dismisses the Daily Goal sheet smoothly

                // Instantly slides the Profile menu fragment back up!
                ProfileSheetFragment profileSheet = ProfileSheetFragment.newInstance();
                profileSheet.show(getParentFragmentManager(), "profile_sheet");
            });
        }

        // ── Load streak ──
        int currentStreak = prefs.getInt("currentStreak", 0);
        int bestStreak    = prefs.getInt("bestStreak",    0);
        tvCurrentStreak.setText(String.valueOf(currentStreak));
        tvBestStreak.setText(String.valueOf(bestStreak));

        // ── Load or generate today's tasks ──
        todayTasks       = loadOrGenerateTasks();
        taskCompleted    = loadCompletionState();
        totalGoalMinutes = getTotalMinutes();

        // ── Render tasks ──
        renderTasks();
        updateProgress();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private List<String[]> loadOrGenerateTasks() {
        List<String[]> tasks = new ArrayList<>();
        String savedTasks = prefs.getString(todayKey + "_tasks", null);

        if (savedTasks != null) {
            String[] parts = savedTasks.split(",");
            for (String part : parts) {
                String[] pair = part.split("\\|");
                if (pair.length == 2) tasks.add(pair);
            }
        } else {
            List<String[]> pool = new ArrayList<>();
            for (String[] goal : GOAL_POOL) pool.add(goal);
            Collections.shuffle(pool, new Random());

            for (int i = 0; i < TASKS_PER_DAY && i < pool.size(); i++) {
                tasks.add(pool.get(i));
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(tasks.get(i)[0]).append("|").append(tasks.get(i)[1]);
                if (i < tasks.size() - 1) sb.append(",");
            }
            prefs.edit().putString(todayKey + "_tasks", sb.toString()).apply();
        }
        return tasks;
    }

    private boolean[] loadCompletionState() {
        boolean[] completed = new boolean[todayTasks.size()];
        Set<String> completedSet = prefs.getStringSet(todayKey + "_completed", new HashSet<>());
        for (int i = 0; i < todayTasks.size(); i++) {
            completed[i] = completedSet.contains(String.valueOf(i));
        }
        return completed;
    }

    private void saveCompletionState() {
        Set<String> completedSet = new HashSet<>();
        for (int i = 0; i < taskCompleted.length; i++) {
            if (taskCompleted[i]) completedSet.add(String.valueOf(i));
        }
        prefs.edit().putStringSet(todayKey + "_completed", completedSet).apply();
    }

    private void renderTasks() {
        taskContainer.removeAllViews();

        for (int i = 0; i < todayTasks.size(); i++) {
            final int index = i;
            String[] task   = todayTasks.get(i);
            String name     = task[0];
            int minutes     = Integer.parseInt(task[1]);
            boolean done    = taskCompleted[i];

            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_task, taskContainer, false);

            TextView tvName   = itemView.findViewById(R.id.tvTaskName);
            TextView tvDetail = itemView.findViewById(R.id.tvTaskDetail);
            ImageView ivCheck = itemView.findViewById(R.id.ivTaskCheck);

            tvName.setText(name);
            tvDetail.setText(minutes + " min " + (done ? "Completed" : "Remaining"));

            tvName.setTextColor(done
                    ? requireContext().getColor(R.color.accent_pink)
                    : requireContext().getColor(R.color.text_dark));

            ivCheck.setImageResource(done
                    ? R.drawable.ic_check_done
                    : R.drawable.ic_check_empty);

            itemView.setOnClickListener(v -> {
                taskCompleted[index] = !taskCompleted[index];
                saveCompletionState();
                updateStreakIfAllDone();
                renderTasks();
                updateProgress();
            });

            taskContainer.addView(itemView);
        }
    }

    private void updateProgress() {
        int completedMinutes = 0;
        for (int i = 0; i < taskCompleted.length; i++) {
            if (taskCompleted[i]) {
                completedMinutes += Integer.parseInt(todayTasks.get(i)[1]);
            }
        }

        int percent = totalGoalMinutes == 0 ? 0
                : (int) ((completedMinutes / (float) totalGoalMinutes) * 100);
        percent = Math.min(100, percent);

        circularProgress.setProgress(percent);
        tvProgressPercent.setText(percent + " %");
        tvMinutesPracticed.setText(completedMinutes + " / " + totalGoalMinutes + " minutes practiced");
    }

    private void updateStreakIfAllDone() {
        boolean allDone = true;
        for (boolean b : taskCompleted) {
            if (!b) { allDone = false; break; }
        }

        if (allDone) {
            String lastCompleted = prefs.getString("lastCompletedDay", "");
            if (!lastCompleted.equals(todayKey)) {
                int streak = prefs.getInt("currentStreak", 0) + 1;
                int best   = Math.max(prefs.getInt("bestStreak", 0), streak);
                prefs.edit()
                        .putInt("currentStreak",    streak)
                        .putInt("bestStreak",        best)
                        .putString("lastCompletedDay", todayKey)
                        .apply();

                if (tvCurrentStreak != null) tvCurrentStreak.setText(String.valueOf(streak));
                if (tvBestStreak != null) tvBestStreak.setText(String.valueOf(best));
            }
        }
    }

    private int getTotalMinutes() {
        int total = 0;
        for (String[] task : todayTasks) total += Integer.parseInt(task[1]);
        return total;
    }

    private String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}