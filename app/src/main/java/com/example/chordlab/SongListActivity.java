package com.example.chordlab;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SongListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        // 1. Setup Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 2. Prepare our scalable data (Just 3 for now, add thousands later!)
        List<Song> songList = new ArrayList<>();
        songList.add(new Song("Tenzionado", "Soapdish"));
        songList.add(new Song("A Thousand Miles", "Vanessa Carlton")); // Fixed the artist for you 😉
        songList.add(new Song("Perfect", "Ed Sheeran")); // <-- Added Perfect here!
        songList.add(new Song("Happy Birthday", "Miscellaneous"));

        // 3. Setup the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerViewSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Pass the list to the adapter, and define what happens when a card is clicked
        SongAdapter adapter = new SongAdapter(songList, song -> {
            Intent intent = new Intent(this, SongPracticeActivity.class);
            // This attaches the clicked song's title to the intent!
            intent.putExtra("SONG_TITLE", song.title);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    // ─── DATA MODEL ────────────────────────────────────────────────────────
    public static class Song {
        String title;
        String artist;

        public Song(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }
    }

    // ─── RECYCLERVIEW ADAPTER ──────────────────────────────────────────────
    public static class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

        private final List<Song> songs;
        private final OnSongClickListener listener;

        public interface OnSongClickListener {
            void onSongClick(Song song);
        }

        public SongAdapter(List<Song> songs, OnSongClickListener listener) {
            this.songs = songs;
            this.listener = listener;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            Song song = songs.get(position);
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