package com.example.chordlab;

public class Song {
    private String title;
    private String artist;
    private String difficulty;
    private String key;
    private String content; // Holds the chords and lyrics text

    public Song(String title, String artist, String difficulty, String key, String content) {
        this.title = title;
        this.artist = artist;
        this.difficulty = difficulty;
        this.key = key;
        this.content = content;
    }

    // Getters (used by your RecyclerView Adapter to display data)
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDifficulty() { return difficulty; }
    public String getKey() { return key; }
    public String getContent() { return content; }
}