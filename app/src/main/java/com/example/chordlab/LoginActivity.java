package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    DatabaseHelper myDb;
    EditText etUser, etPass;
    Button btnSignIn;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);

        // Auto-login check
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
                goToDashboard();
            }
            finish();
            return;
        }

        TextView tvSignUp = findViewById(R.id.tvSignUpLink);
        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class)));

        myDb      = new DatabaseHelper(this);
        etUser    = findViewById(R.id.et_login_username);
        etPass    = findViewById(R.id.et_login_password);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (myDb.checkUser(user, pass)) {
                // Save session
                session.saveSession(user, "");

                // Save username to UserSession
                SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                userPrefs.edit().putString("username", user).apply();

                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                // Check if this user has completed details before
                SharedPreferences detailsPrefs = getSharedPreferences("DetailsPrefs", MODE_PRIVATE);
                boolean detailsComplete = detailsPrefs.getBoolean(user + "_detailsComplete", false);

                if (!detailsComplete) {
                    startActivity(new Intent(this, DetailsActivity.class));
                } else {
                    // Restore their saved instrument and goal
                    String instrument = detailsPrefs.getString(user + "_instrument", "Guitar");
                    String dailyGoal  = detailsPrefs.getString(user + "_dailyGoal",  "20 mins");
                    userPrefs.edit()
                            .putString("instrument", instrument)
                            .putString("dailyGoal",  dailyGoal)
                            .apply();
                    goToDashboard();
                }
                finish();

            } else {
                Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}