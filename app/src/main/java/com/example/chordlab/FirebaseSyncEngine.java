package com.example.chordlab;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncEngine {

    private static final String TAG = "FirebaseSyncEngine";
    private final DatabaseHelper dbHelper;
    private final DatabaseReference mDatabase;

    public FirebaseSyncEngine(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        // Points to the root node of your Firebase Realtime Database JSON tree
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void syncUserToCloud(String username) {
        Log.d(TAG, "DIAGNOSTIC: syncUserToCloud called for username: [" + username + "]");

        if (username == null || username.isEmpty() || username.equalsIgnoreCase("User")) {
            Log.w(TAG, "DIAGNOSTIC STOP: Username is null, empty, or equals default 'User'. Sync aborted!");
            return;
        }

        try {
            // PIPELINE CHECK 1: Extracting user rows
            Map<String, Object> userDataMap = new HashMap<>();
            Cursor userCursor = dbHelper.getUserData(username);

            if (userCursor != null && userCursor.moveToFirst()) {
                int columnCount = userCursor.getColumnCount();
                Log.d(TAG, "DIAGNOSTIC: Found user in SQLite. Compiling " + columnCount + " data columns.");
                for (int i = 0; i < columnCount; i++) {
                    String colName = userCursor.getColumnName(i);
                    if (colName.equalsIgnoreCase("PASSWORD")) continue;

                    if (userCursor.getType(i) == Cursor.FIELD_TYPE_INTEGER) {
                        userDataMap.put(colName, userCursor.getInt(i));
                    } else {
                        userDataMap.put(colName, userCursor.getString(i));
                    }
                }
                userCursor.close();
            } else {
                if (userCursor != null) userCursor.close();
                Log.e(TAG, "DIAGNOSTIC STOP: DatabaseHelper returned 0 rows for username: [" + username + "]. Does this user exist in SQLite?");
                return;
            }

            // PIPELINE CHECK 2: Collecting tasks
            List<Map<String, Object>> dailyTasksList = new ArrayList<>();
            SQLiteDatabase readableDb = dbHelper.getReadableDatabase();
            Cursor taskCursor = readableDb.rawQuery("SELECT * FROM daily_tasks WHERE USERNAME=?", new String[]{username});
            if (taskCursor != null) {
                Log.d(TAG, "DIAGNOSTIC: Found " + taskCursor.getCount() + " tasks in SQLite for this user.");
                while (taskCursor.moveToNext()) {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("TASK_DATE", taskCursor.getString(taskCursor.getColumnIndexOrThrow("TASK_DATE")));
                    taskMap.put("TASK_TYPE", taskCursor.getString(taskCursor.getColumnIndexOrThrow("TASK_TYPE")));
                    taskMap.put("TARGET_VALUE", taskCursor.getString(taskCursor.getColumnIndexOrThrow("TARGET_VALUE")));
                    taskMap.put("CURRENT_VALUE", taskCursor.getInt(taskCursor.getColumnIndexOrThrow("CURRENT_VALUE")));
                    taskMap.put("IS_COMPLETED", taskCursor.getInt(taskCursor.getColumnIndexOrThrow("IS_COMPLETED")));
                    dailyTasksList.add(taskMap);
                }
                taskCursor.close();
            }

            // Master Payload Map
            Map<String, Object> masterSyncDocument = new HashMap<>();
            masterSyncDocument.put("USERNAME", username);
            masterSyncDocument.put("userData", userDataMap);
            masterSyncDocument.put("dailyTasks", dailyTasksList);
            masterSyncDocument.put("lastSyncedAt", System.currentTimeMillis());

            Log.d(TAG, "DIAGNOSTIC: Sending payload to Firebase Realtime Database now...");

            // PIPELINE CHECK 3: Push payload under the path root/users/{username}
            mDatabase.child("users").child(username)
                    .setValue(masterSyncDocument)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "DIAGNOSTIC SUCCESS!!! Data has officially landed inside your Firebase JSON tree."))
                    .addOnFailureListener(e -> Log.e(TAG, "DIAGNOSTIC CRITICAL RUNTIME ERROR: Sync transmission failed: " + e.getMessage(), e));

        } catch (Exception e) {
            Log.e(TAG, "DIAGNOSTIC CRITICAL LOCAL ERROR: Failed compiling payload: " + e.getMessage(), e);
        }
    }
}