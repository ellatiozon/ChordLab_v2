package com.example.chordlab;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongListActivity extends AppCompatActivity {

    private List<SongData> originalSongList;
    private SongAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 1. Load songs from JSON
        originalSongList = SongDatabase.getAllSongs(this);

        // 2. SORT ALPHABETICALLY BY TITLE
        Collections.sort(originalSongList, (song1, song2) ->
                song1.title.compareToIgnoreCase(song2.title));

        // 3. Set up RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerViewSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create adapter with a shallow copy of the sorted list
        adapter = new SongAdapter(new ArrayList<>(originalSongList), song -> {
            Intent intent = new Intent(this, SongPracticeActivity.class);
            intent.putExtra("SONG_TITLE", song.title);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // 4. SET UP SEARCH LOGIC
        SearchView searchView = findViewById(R.id.searchViewSongs);
        searchView.setQueryHint("Search title or artist...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // We handle filtering in real-time below
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    // ─── FILTER LOGIC ──────────────────────────────────────────────────────
    private void filter(String text) {
        List<SongData> filteredList = new ArrayList<>();

        for (SongData song : originalSongList) {
            // Check if title OR artist matches what the user is typing
            if (song.title.toLowerCase().contains(text.toLowerCase()) ||
                    song.artist.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(song);
            }
        }

        // Push the filtered results directly to the adapter
        adapter.updateList(filteredList);
    }

    // ─── RECYCLERVIEW ADAPTER ──────────────────────────────────────────────
    public static class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

        private List<SongData> songs;
        private final OnSongClickListener listener;

        public interface OnSongClickListener {
            void onSongClick(SongData song);
        }

        public SongAdapter(List<SongData> songs, OnSongClickListener listener) {
            this.songs = songs;
            this.listener = listener;
        }

        // Helper method to clear old items and display searched items safely
        public void updateList(List<SongData> newList) {
            this.songs = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            SongData song = songs.get(position);
            holder.tvTitle.setText(song.title);
            holder.tvArtist.setText(song.artist);

            holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        static class SongViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvArtist;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvSongTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
            }
        }
    }
}