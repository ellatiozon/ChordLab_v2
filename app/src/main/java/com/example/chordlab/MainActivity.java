package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);

        // If already logged in, route correctly
        if (session.isLoggedIn()) {
            SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String username = userPrefs.getString("username", "");

            SharedPreferences detailsPrefs = getSharedPreferences("DetailsPrefs", MODE_PRIVATE);
            boolean detailsComplete = detailsPrefs.getBoolean(username + "_detailsComplete", false);

            if (!detailsComplete) {
                startActivity(new Intent(this, DetailsActivity.class));
            } else {
                // Restore instrument and goal for this user
                String instrument = detailsPrefs.getString(username + "_instrument", "Guitar");
                String dailyGoal  = detailsPrefs.getString(username + "_dailyGoal",  "20 mins");
                userPrefs.edit()
                        .putString("instrument", instrument)
                        .putString("dailyGoal",  dailyGoal)
                        .apply();
                startActivity(new Intent(this, DashboardActivity.class));
            }
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        btnGetStarted.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));
    }
}