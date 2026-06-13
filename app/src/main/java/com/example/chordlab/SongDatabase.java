package com.example.chordlab;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SongDatabase {

    // Reads the file from the assets folder
    private static String loadJSONFromAsset(Context context) {
        try {
            InputStream is = context.getAssets().open("songs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Converts the JSON text into a list of SongData objects
    public static List<SongData> getAllSongs(Context context) {
        List<SongData> songList = new ArrayList<>();
        try {
            String json = loadJSONFromAsset(context);
            if (json == null) return songList;

            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject songObj = jsonArray.getJSONObject(i);
                SongData song = new SongData(songObj.getString("title"), songObj.getString("artist"));

                JSONObject instrumentsObj = songObj.getJSONObject("instruments");
                String[] instrumentNames = {"Guitar", "Ukulele", "Piano"};

                for (String instName : instrumentNames) {
                    if (instrumentsObj.has(instName)) {
                        JSONObject instDetails = instrumentsObj.getJSONObject(instName);
                        song.instruments.put(instName, new SongData.InstrumentDetails(
                                instDetails.getString("key"),
                                instDetails.getString("specs"),
                                instDetails.getString("content")
                        ));
                    }
                }
                songList.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songList;
    }

    // Helper to find a single song by its title
    public static SongData getSongByTitle(Context context, String title) {
        List<SongData> allSongs = getAllSongs(context);
        for (SongData song : allSongs) {
            if (song.title.equalsIgnoreCase(title)) {
                return song;
            }
        }
        return null; // Song not found
    }
}