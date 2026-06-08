package com.example.chordlab;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PracticeModesAdapter extends RecyclerView.Adapter<PracticeModesAdapter.PageViewHolder> {

    // Define the click callback interface
    public interface OnCardClickListener {
        void onCardClick(int cardId, View cardView);
    }

    private final OnCardClickListener listener;

    // Constructor accepts the interface callback instance
    public PracticeModesAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    public static class PageViewHolder extends RecyclerView.ViewHolder {
        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? R.layout.item_practice_page_one : R.layout.item_practice_page_two;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        View view = holder.itemView;

        if (position == 0) {
            setupClick(view, R.id.cardPracticeMode);
            setupClick(view, R.id.cardMetronome);
            setupClick(view, R.id.cardFlashCards);
            setupClick(view, R.id.cardSongPractice);
        } else {
            setupClick(view, R.id.cardChordLibrary);
            setupClick(view, R.id.cardVideoTranslator);
        }
    }

    // Helper function to safely find views and attach callbacks
    private void setupClick(View parentView, int viewId) {
        View card = parentView.findViewById(viewId);
        if (card != null) {
            card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCardClick(viewId, v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}