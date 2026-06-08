package com.example.chordlab;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProgressReportSheetFragment extends BottomSheetDialogFragment {

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

        // TODO: You can find your progress bars here later to update percentages
        // e.g., ProgressBar pb = view.findViewById(R.id.your_progress_bar_id);

        // ── USER REQUESTED CHANGE: REDIRECTS BACK TO PROFILE MENU ──
        // Moved from onStart() to onViewCreated() so the 'view' variable can be accessed properly.
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