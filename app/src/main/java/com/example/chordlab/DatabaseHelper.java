package com.example.chordlab;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChordLab.db";
    private static final String TABLE_NAME = "users";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "USERNAME";
    private static final String COL_3 = "EMAIL";
    private static final String COL_4 = "PASSWORD";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, USERNAME TEXT, EMAIL TEXT, PASSWORD TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Method to insert a new user
    public boolean insertUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, username);
        contentValues.put(COL_3, email);
        contentValues.put(COL_4, password);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1; // returns true if inserted successfully
    }

    // Add this inside your DatabaseHelper class
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE USERNAME=? AND PASSWORD=?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Cursor getUserData(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE USERNAME=?", new String[]{username});
    }

    // Check if Username exists
    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE USERNAME = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Check if Email exists
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE EMAIL = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}