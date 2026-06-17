package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    DatabaseHelper myDb;
    private FirebaseAuth mAuth; // Firebase Authentication Reference

    EditText etUser, etPass;
    Button btnSignIn;
    SessionManager session;
    ImageView ivTogglePassword;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        myDb   = new DatabaseHelper(this);
        session = new SessionManager(this);

        // 1. Auto-login check: Go straight to Dashboard if already logged in
        if (session.isLoggedIn() || mAuth.getCurrentUser() != null) {
            goToDashboard();
            return;
        }

        TextView tvSignUp = findViewById(R.id.tvSignUpLink);
        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class)));

        etUser    = findViewById(R.id.et_login_username);
        etPass    = findViewById(R.id.et_login_password);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(v -> {
            String inputUserOrEmail = etUser.getText().toString().trim();
            String pass             = etPass.getText().toString().trim();

            if (inputUserOrEmail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine if the input is an email or a username
            String targetEmail = "";
            String targetUsername = "";

            if (inputUserOrEmail.contains("@")) {
                targetEmail = inputUserOrEmail;
                // Fetch username locally via the email lookup helper we added earlier
                targetUsername = myDb.getUsernameByEmail(targetEmail);
                if (targetUsername.isEmpty()) {
                    // Fallback to email prefix if not cached locally yet
                    targetUsername = targetEmail.split("@")[0];
                }
            } else {
                targetUsername = inputUserOrEmail;
                targetEmail = myDb.getUserEmail(targetUsername);
            }

            if (targetEmail.isEmpty()) {
                Toast.makeText(this, "User details not found locally. Please use email.", Toast.LENGTH_SHORT).show();
                return;
            }

            // ─── FIX: Explicit final copies to pass safely inside the lambda ───
            final String finalUsername = targetUsername;
            final String finalEmail = targetEmail;

            btnSignIn.setEnabled(false); // Prevent multiple simultaneous connection taps

            // ── Online Authentication Focus via Firebase ──
            mAuth.signInWithEmailAndPassword(finalEmail, pass)
                .addOnCompleteListener(this, task -> {
                    btnSignIn.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Online authentication passed! Sync session configurations using safe final values:
                        session.saveSession(finalUsername, finalEmail);

                        // Save username and email to UserSession for app-wide use
                        SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                        userPrefs.edit()
                                .putString("username", finalUsername)
                                .putString("email", finalEmail)
                                .apply();

                        // Restore saved details to the current session
                        SharedPreferences detailsPrefs = getSharedPreferences("DetailsPrefs", MODE_PRIVATE);
                        String instrument = detailsPrefs.getString(finalUsername + "_instrument", "Guitar");
                        String dailyGoal  = detailsPrefs.getString(finalUsername + "_dailyGoal",  "20 mins");

                        userPrefs.edit()
                                .putString("instrument", instrument)
                                .putString("dailyGoal",  dailyGoal)
                                .apply();

                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                        // Go straight to Dashboard
                        goToDashboard();
                    } else {
                        Toast.makeText(this, "Invalid Credentials: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;

            if (isPasswordVisible) {
                etPass.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_on);
            }

            if (etPass.getText() != null) {
                etPass.setSelection(etPass.getText().length());
            }
        });
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}