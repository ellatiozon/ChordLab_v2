package com.example.chordlab;

import java.util.HashMap;
import java.util.Map;

public class SongData {
    public String title;
    public String artist;
    public Map<String, InstrumentDetails> instruments = new HashMap<>();

    public SongData(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    public static class InstrumentDetails {
        public String key;
        public String specs;
        public String content;

        public InstrumentDetails(String key, String specs, String content) {
            this.key = key;
            this.specs = specs;
            this.content = content;
        }
    }
}