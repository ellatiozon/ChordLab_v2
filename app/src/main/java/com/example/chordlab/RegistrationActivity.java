package com.example.chordlab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegistrationActivity extends AppCompatActivity {

    DatabaseHelper myDb;
    EditText etUser, etEmail, etPass, etConfirmPass;
    Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registration), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        myDb          = new DatabaseHelper(this);
        etUser        = findViewById(R.id.et_username);
        etEmail       = findViewById(R.id.et_email);
        etPass        = findViewById(R.id.et_password);
        etConfirmPass = findViewById(R.id.et_confirm_password);
        btnNext       = findViewById(R.id.btnNext);

        addData();
    }

    public void addData() {
        btnNext.setOnClickListener(v -> {
            String user        = etUser.getText().toString().trim();
            String email       = etEmail.getText().toString().trim();
            String pass        = etPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            // ── Validation ──
            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (myDb.checkUsernameExists(user)) {
                Toast.makeText(this, "Username is already taken!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (myDb.checkEmailExists(email)) {
                Toast.makeText(this, "Email is already registered!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── Insert into database ──
            boolean isInserted = myDb.insertUser(user, email, pass);

            if (isInserted) {
                // Save session
                SessionManager session = new SessionManager(this);
                session.saveSession(user, email);

                // Save username and email to UserSession
                // detailsComplete is NOT set here — new user must complete details first
                getSharedPreferences("UserSession", MODE_PRIVATE)
                        .edit()
                        .putString("username", user)
                        .putString("email",    email)
                        .apply();

                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_LONG).show();

                // New user → go to Details page
                Intent intent = new Intent(RegistrationActivity.this, DetailsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            } else {
                Toast.makeText(this, "Registration Failed. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }
}