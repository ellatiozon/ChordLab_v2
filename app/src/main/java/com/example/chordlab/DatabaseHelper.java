package com.example.chordlab;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Random;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChordLab.db";
    private static final String TABLE_NAME = "users";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "USERNAME";
    private static final String COL_3 = "EMAIL";
    private static final String COL_4 = "PASSWORD";

    // ─── NEW TASKS TABLE CONSTANTS ───
    private static final String TABLE_TASKS = "daily_tasks";
    private static final String TASKS_COL_ID = "ID";
    private static final String TASKS_COL_USERNAME = "USERNAME";
    private static final String TASKS_COL_DATE = "TASK_DATE";
    private static final String TASKS_COL_TYPE = "TASK_TYPE"; // TOTAL_TIME, CORRECT_CHORDS, SPECIFIC_CHORD, SPECIFIC_NOTE, FLASHCARD_TIME
    private static final String TASKS_COL_TARGET = "TARGET_VALUE";
    private static final String TASKS_COL_CURRENT = "CURRENT_VALUE";
    private static final String TASKS_COL_COMPLETED = "IS_COMPLETED"; // 0 = false, 1 = true

    // ─── NEW INDIVIDUAL PROGRESSION TRACKING COLUMNS ───
    private static final String COL_TOTAL_MINUTES = "TOTAL_PRACTICE_MINUTES";
    private static final String COL_GUITAR_CHORDS = "GUITAR_CHORDS_LEARNED";
    private static final String COL_UKULELE_CHORDS = "UKULELE_CHORDS_LEARNED";
    private static final String COL_PIANO_CHORDS = "PIANO_CHORDS_LEARNED";
    private static final String COL_PIANO_NOTES = "PIANO_NOTES_LEARNED";

    // Flags to check which activities were triggered (Saved as INTEGER: 0 = false, 1 = true)
    private static final String COL_EXPLORED_GUITAR = "EXPLORED_GUITAR";
    private static final String COL_EXPLORED_UKULELE = "EXPLORED_UKULELE";
    private static final String COL_EXPLORED_PIANO = "EXPLORED_PIANO";

    // Bump version from 4 to 5 to handle individual progress columns upgrade smoothly
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "USERNAME TEXT, " +
                "EMAIL TEXT, " +
                "PASSWORD TEXT, " +
                "INSTRUMENT TEXT, " +
                "DAILY_GOAL TEXT, " +
                "EXP INTEGER DEFAULT 0, " +
                "LEVEL INTEGER DEFAULT 1, " +
                "CHORDS_LEARNED INTEGER DEFAULT 0, " +
                COL_TOTAL_MINUTES + " INTEGER DEFAULT 0, " +
                COL_GUITAR_CHORDS + " INTEGER DEFAULT 0, " +
                COL_UKULELE_CHORDS + " INTEGER DEFAULT 0, " +
                COL_PIANO_CHORDS + " INTEGER DEFAULT 0, " +
                COL_PIANO_NOTES + " INTEGER DEFAULT 0, " +
                COL_EXPLORED_GUITAR + " INTEGER DEFAULT 0, " +
                COL_EXPLORED_UKULELE + " INTEGER DEFAULT 0, " +
                COL_EXPLORED_PIANO + " INTEGER DEFAULT 0)");

        // Create tasks table for new database installations
        createTasksTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN EXP INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN LEVEL INTEGER DEFAULT 1");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN CHORDS_LEARNED INTEGER DEFAULT 0");
        }
        if (oldVersion < 4) {
            createTasksTable(db);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_TOTAL_MINUTES + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_GUITAR_CHORDS + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_UKULELE_CHORDS + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_PIANO_CHORDS + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_PIANO_NOTES + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_EXPLORED_GUITAR + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_EXPLORED_UKULELE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_EXPLORED_PIANO + " INTEGER DEFAULT 0");
        }
    }

    private void createTasksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TASKS + " (" +
                TASKS_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TASKS_COL_USERNAME + " TEXT, " +
                TASKS_COL_DATE + " TEXT, " +
                TASKS_COL_TYPE + " TEXT, " +
                TASKS_COL_TARGET + " TEXT, " +
                TASKS_COL_CURRENT + " INTEGER DEFAULT 0, " +
                TASKS_COL_COMPLETED + " INTEGER DEFAULT 0)");
    }

    public boolean insertUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, username);
        contentValues.put(COL_3, email);
        contentValues.put(COL_4, password);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE USERNAME=? AND PASSWORD=?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String getUserEmail(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String email = "";
        Cursor cursor = db.rawQuery("SELECT " + COL_3 + " FROM " + TABLE_NAME + " WHERE " + COL_2 + "=?", new String[]{username});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                email = cursor.getString(0);
            }
            cursor.close();
        }
        return email;
    }

    // ─── ADDED HELPER: FETCH USERNAME VIA EMAIL KEY ───
    public String getUsernameByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String username = "";
        Cursor cursor = db.rawQuery("SELECT " + COL_2 + " FROM " + TABLE_NAME + " WHERE " + COL_3 + "=?", new String[]{email});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                username = cursor.getString(0);
            }
            cursor.close();
        }
        return username;
    }

    public Cursor getUserData(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE USERNAME=?", new String[]{username});
    }

    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE USERNAME = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE EMAIL = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean updateUserDetails(String username, String instrument, String goal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("instrument", instrument);
        contentValues.put("daily_goal", goal);
        int result = db.update("users", contentValues, "username = ?", new String[]{username});
        return result > 0;
    }

    public int[] getExpAndLevel(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT EXP, LEVEL FROM " + TABLE_NAME + " WHERE USERNAME=?",
                new String[]{username}
        );
        int[] result = {0, 1};
        if (cursor != null && cursor.moveToFirst()) {
            result[0] = cursor.getInt(cursor.getColumnIndexOrThrow("EXP"));
            result[1] = cursor.getInt(cursor.getColumnIndexOrThrow("LEVEL"));
            cursor.close();
        }
        return result;
    }

    public int[] addExp(String username, int expToAdd) {
        int[] current = getExpAndLevel(username);
        int exp   = current[0] + expToAdd;
        int level = current[1];
        int didLevelUp = 0;

        while (exp >= 100) {
            exp -= 100;
            level++;
            didLevelUp = 1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("EXP", exp);
        cv.put("LEVEL", level);
        db.update(TABLE_NAME, cv, "USERNAME=?", new String[]{username});

        return new int[]{exp, level, didLevelUp};
    }

    public String getUserTierStatus(String username) {
        int[] expAndLevel = getExpAndLevel(username);
        int level = expAndLevel[1];

        if (level >= 1 && level <= 10) return "Beginner I";
        else if (level >= 11 && level <= 20) return "Beginner II";
        else if (level >= 21 && level <= 30) return "Beginner III";
        else if (level >= 31 && level <= 40) return "Intermediate I";
        else if (level >= 41 && level <= 50) return "Intermediate II";
        else if (level >= 51 && level <= 60) return "Intermediate III";
        else if (level >= 61 && level <= 70) return "Advanced I";
        else if (level >= 71 && level <= 80) return "Advanced II";
        else if (level >= 81) return "Advanced III";
        return "Beginner I";
    }

    public String getFullLevelAndTierString(String username) {
        int[] expAndLevel = getExpAndLevel(username);
        int level = expAndLevel[1];
        String tier = getUserTierStatus(username);
        return "Level " + level + " - " + tier;
    }

    public int getChordsLearned(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        int chords = 0;
        Cursor cursor = db.rawQuery("SELECT CHORDS_LEARNED FROM " + TABLE_NAME + " WHERE USERNAME=?", new String[]{username});
        if (cursor != null && cursor.moveToFirst()) {
            chords = cursor.getInt(0);
            cursor.close();
        }
        return chords;
    }

    public void incrementChordsLearned(String username) {
        int currentChords = getChordsLearned(username);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("CHORDS_LEARNED", currentChords + 1);
        db.update(TABLE_NAME, cv, "USERNAME=?", new String[]{username});
    }

    // ============================================================
    // ─── NEW METHODS: INSTRUMENT SPECIFIC PROGRESSION TRACKING ───
    // ============================================================

    public void addPracticeMinutes(String username, int minutes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COL_TOTAL_MINUTES + " = " + COL_TOTAL_MINUTES + " + ? WHERE USERNAME = ?", new Object[]{minutes, username});
    }

    public void markInstrumentExplored(String username, String instrumentType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (instrumentType.equalsIgnoreCase("Guitar")) {
            cv.put(COL_EXPLORED_GUITAR, 1);
        } else if (instrumentType.equalsIgnoreCase("Ukulele")) {
            cv.put(COL_EXPLORED_UKULELE, 1);
        } else if (instrumentType.equalsIgnoreCase("Piano")) {
            cv.put(COL_EXPLORED_PIANO, 1);
        }
        db.update(TABLE_NAME, cv, "USERNAME=?", new String[]{username});
    }

    public void incrementSpecificInstrumentStat(String username, String columnName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + columnName + " = " + columnName + " + 1 WHERE USERNAME = ?", new Object[]{username});
    }

    // Safe Column Accessors
    public String getGuitarCol() { return COL_GUITAR_CHORDS; }
    public String getUkuleleCol() { return COL_UKULELE_CHORDS; }
    public String getPianoChordsCol() { return COL_PIANO_CHORDS; }
    public String getPianoNotesCol() { return COL_PIANO_NOTES; }

    public ProgressData getUserProgress(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        ProgressData data = new ProgressData();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE USERNAME=?", new String[]{username});

        if (cursor != null && cursor.moveToFirst()) {
            data.totalPracticeMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_MINUTES));
            data.guitarChords = cursor.getInt(cursor.getColumnIndexOrThrow(COL_GUITAR_CHORDS));
            data.ukuleleChords = cursor.getInt(cursor.getColumnIndexOrThrow(COL_UKULELE_CHORDS));
            data.pianoChords = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PIANO_CHORDS));
            data.pianoNotes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PIANO_NOTES));

            int expG = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EXPLORED_GUITAR));
            int expU = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EXPLORED_UKULELE));
            int expP = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EXPLORED_PIANO));
            data.instrumentsExploredCount = expG + expU + expP;

            cursor.close();
        }
        return data;
    }

    public static class ProgressData {
        public int totalPracticeMinutes = 0;
        public int guitarChords = 0;
        public int ukuleleChords = 0;
        public int pianoChords = 0;
        public int pianoNotes = 0;
        public int instrumentsExploredCount = 0;
    }

    // ============================================
    // ─── TASK SYSTEM METHODS ───
    // ============================================

    public void generateDailyTasksIfMissing(String username, String dateStr) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TASKS + " WHERE " + TASKS_COL_USERNAME + "=? AND " + TASKS_COL_DATE + "=?", new String[]{username, dateStr});

        if (cursor != null && cursor.getCount() == 0) {
            cursor.close();
            SQLiteDatabase writeDb = this.getWritableDatabase();
            Random r = new Random();

            String tierString = getUserTierStatus(username).toLowerCase();

            int totalTimeGoal = 20;
            int correctChordsGoal = 5;
            int flashcardTimeGoal = 3;

            if (tierString.contains("intermediate")) {
                totalTimeGoal = 30;
                correctChordsGoal = 10;
                flashcardTimeGoal = 5;
            } else if (tierString.contains("advanced")) {
                totalTimeGoal = 40;
                correctChordsGoal = 15;
                flashcardTimeGoal = 8;
            }

            String[] specificChords = {"A Major", "A Minor", "C Major", "D Major", "D Minor", "E Minor", "G Major"};
            String chosenChord = specificChords[r.nextInt(specificChords.length)];

            insertTaskRow(writeDb, username, dateStr, "TOTAL_TIME", String.valueOf(totalTimeGoal));
            insertTaskRow(writeDb, username, dateStr, "CORRECT_CHORDS", String.valueOf(correctChordsGoal));
            insertTaskRow(writeDb, username, dateStr, "SPECIFIC_CHORD", chosenChord);
            insertTaskRow(writeDb, username, dateStr, "FLASHCARD_TIME", String.valueOf(flashcardTimeGoal));
        } else if (cursor != null) {
            cursor.close();
        }
    }

    private void insertTaskRow(SQLiteDatabase db, String username, String date, String type, String target) {
        ContentValues cv = new ContentValues();
        cv.put(TASKS_COL_USERNAME, username);
        cv.put(TASKS_COL_DATE, date);
        cv.put(TASKS_COL_TYPE, type);
        cv.put(TASKS_COL_TARGET, target);
        cv.put(TASKS_COL_CURRENT, 0);
        cv.put(TASKS_COL_COMPLETED, 0);
        db.insert(TABLE_TASKS, null, cv);
    }

    public void trackTaskProgress(String username, String dateStr, String type, int progressIncrement, String chordPlayed) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TASKS + " WHERE " + TASKS_COL_USERNAME + "=? AND " + TASKS_COL_DATE + "=? AND " + TASKS_COL_TYPE + "=?", new String[]{username, dateStr, type});

        if (cursor != null && cursor.moveToFirst()) {
            int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(TASKS_COL_COMPLETED));
            if (isCompleted == 1) {
                cursor.close();
                return;
            }

            int currentVal = cursor.getInt(cursor.getColumnIndexOrThrow(TASKS_COL_CURRENT));
            String targetVal = cursor.getString(cursor.getColumnIndexOrThrow(TASKS_COL_TARGET));
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(TASKS_COL_ID));

            int newVal = currentVal;
            boolean processCompletion = false;

            if (type.equals("SPECIFIC_CHORD") || type.equals("SPECIFIC_NOTE")) {
                if (chordPlayed != null && targetVal.equalsIgnoreCase(chordPlayed)) {
                    newVal = 1;
                    processCompletion = true;
                }
            } else {
                newVal += progressIncrement;
                if (newVal >= Integer.parseInt(targetVal)) {
                    processCompletion = true;
                }
            }

            ContentValues cv = new ContentValues();
            cv.put(TASKS_COL_CURRENT, newVal);
            if (processCompletion) {
                cv.put(TASKS_COL_COMPLETED, 1);
                addExp(username, 15);
            }

            db.update(TABLE_TASKS, cv, TASKS_COL_ID + "=?", new String[]{String.valueOf(id)});
            cursor.close();
        }
    }

    public Cursor getDailyTasksCursor(String username, String dateStr) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TASKS + " WHERE " + TASKS_COL_USERNAME + "=? AND " + TASKS_COL_DATE + "=?", new String[]{username, dateStr});
    }
}