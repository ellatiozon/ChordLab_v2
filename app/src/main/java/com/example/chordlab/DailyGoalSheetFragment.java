package com.example.chordlab;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyGoalSheetFragment extends BottomSheetDialogFragment {

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView tvProgressPercent, tvMinutesPracticed;
    private TextView tvCurrentStreak, tvBestStreak;
    private CircularProgressView circularProgress;
    private LinearLayout taskContainer;

    // ── State ────────────────────────────────────────────────────────────────
    private List<String[]> todayTasks;   // [taskTitle, targetValue, taskType, currentValue, isCompleted]
    private SharedPreferences prefs;
    private String todayKey;
    private String currentUsername;
    private DatabaseHelper dbHelper;

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

        dbHelper = new DatabaseHelper(requireContext());
        prefs = requireContext().getSharedPreferences("DailyGoalPrefs", Context.MODE_PRIVATE);
        todayKey = getTodayKey();

        // Retrieve active user session
        SharedPreferences sessionPrefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        currentUsername = sessionPrefs.getString("username", "");

        // ── Bind views from inflated layout ──
        tvProgressPercent  = view.findViewById(R.id.tvProgressPercent);
        tvMinutesPracticed = view.findViewById(R.id.tvMinutesPracticed);
        circularProgress   = view.findViewById(R.id.circularProgress);
        taskContainer      = view.findViewById(R.id.taskContainer);
        tvCurrentStreak    = view.findViewById(R.id.tvCurrentStreak);
        tvBestStreak       = view.findViewById(R.id.tvBestStreak);

        // ── BACK BUTTON ──
        View btnBack = view.findViewById(R.id.btnBackDailyGoal);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dismiss();
                ProfileSheetFragment profileSheet = ProfileSheetFragment.newInstance();
                profileSheet.show(getParentFragmentManager(), "profile_sheet");
            });
        }

        // ── Load streak ──
        int currentStreak = prefs.getInt("currentStreak", 0);
        int bestStreak    = prefs.getInt("bestStreak",    0);
        tvCurrentStreak.setText(String.valueOf(currentStreak));
        tvBestStreak.setText(String.valueOf(bestStreak));

        // ── Load tasks from SQLite ──
        loadTasksFromDatabase();

        // ── Render and update progress ──
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

    private void loadTasksFromDatabase() {
        todayTasks = new ArrayList<>();
        if (currentUsername.isEmpty()) return;

        // Ensure user's unique daily requirements are allocated for today
        dbHelper.generateDailyTasksIfMissing(currentUsername, todayKey);

        Cursor cursor = dbHelper.getDailyTasksCursor(currentUsername, todayKey);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("TASK_TYPE"));
                String target = cursor.getString(cursor.getColumnIndexOrThrow("TARGET_VALUE"));
                String current = cursor.getString(cursor.getColumnIndexOrThrow("CURRENT_VALUE"));
                String completed = cursor.getString(cursor.getColumnIndexOrThrow("IS_COMPLETED"));

                String taskTitle = "";
                switch (type) {
                    case "TOTAL_TIME":
                        taskTitle = "Practice Time (" + target + " mins total)";
                        break;
                    case "CORRECT_CHORDS":
                        taskTitle = "Get " + target + " Chords Correct";
                        break;
                    case "SPECIFIC_CHORD":
                        taskTitle = "Play " + target + " Once";
                        break;
                    case "FLASHCARD_TIME":
                        taskTitle = "Use Flashcards for " + target + " mins";
                        break;
                }
                todayTasks.add(new String[]{taskTitle, target, type, current, completed});
            }
            cursor.close();
        }
    }

    private void renderTasks() {
        taskContainer.removeAllViews();

        for (int i = 0; i < todayTasks.size(); i++) {
            String[] task = todayTasks.get(i);
            String title = task[0];
            String target = task[1];
            String type = task[2];
            String current = task[3];
            boolean done = "1".equals(task[4]);

            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_task, taskContainer, false);

            TextView tvName   = itemView.findViewById(R.id.tvTaskName);
            TextView tvDetail = itemView.findViewById(R.id.tvTaskDetail);
            ImageView ivCheck = itemView.findViewById(R.id.ivTaskCheck);

            tvName.setText(title);

            if (type.equals("SPECIFIC_CHORD")) {
                tvDetail.setText(done ? "Completed" : "Incomplete");
            } else {
                String unit = (type.contains("TIME")) ? " mins" : " chords";
                tvDetail.setText(current + " / " + target + unit + (done ? " (Completed)" : ""));
            }

            tvName.setTextColor(done
                    ? requireContext().getColor(R.color.accent_pink)
                    : requireContext().getColor(R.color.text_dark));

            ivCheck.setImageResource(done
                    ? R.drawable.ic_check_done
                    : R.drawable.ic_check_empty);

            // Clicks no longer hardcode manual overrides to remain synchronized with detection activities
            taskContainer.addView(itemView);
        }
    }

    private void updateProgress() {
        int totalTasks = todayTasks.size();
        if (totalTasks == 0) return;

        int completedCount = 0;
        for (String[] task : todayTasks) {
            if ("1".equals(task[4])) {
                completedCount++;
            }
        }

        int percent = (int) ((completedCount / (float) totalTasks) * 100);
        percent = Math.min(100, percent);

        circularProgress.setProgress(percent);
        tvProgressPercent.setText(percent + " %");
        tvMinutesPracticed.setText(completedCount + " / " + totalTasks + " goals completed");

        if (completedCount == totalTasks) {
            updateStreak();
        }
    }

    private void updateStreak() {
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

    private String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}