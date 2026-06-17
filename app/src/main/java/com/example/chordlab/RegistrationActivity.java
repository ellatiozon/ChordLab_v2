package com.example.chordlab;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class RegistrationActivity extends AppCompatActivity {

    DatabaseHelper myDb;
    private FirebaseAuth mAuth; // Firebase Authentication Reference

    EditText etUser, etEmail, etPass, etConfirmPass;
    Button btnNext;
    ImageView ivTogglePassword, ivToggleConfirmPassword, btnBack;
    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

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

        // Initialize Firebase Auth
        mAuth         = FirebaseAuth.getInstance();
        myDb          = new DatabaseHelper(this);

        etUser        = findViewById(R.id.et_username);
        etEmail       = findViewById(R.id.et_email);
        etPass        = findViewById(R.id.et_password);
        etConfirmPass = findViewById(R.id.et_confirm_password);
        btnNext       = findViewById(R.id.btnNext);
        btnBack       = findViewById(R.id.btnBackReg);

        addData();
        btnBack.setOnClickListener(v -> finish());
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

            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── Online Authentication Focus via Firebase ──
            btnNext.setEnabled(false); // Prevent double taps during network calls

            mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    btnNext.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Online authentication passed! Local caching fallback sync:
                        boolean isInserted = myDb.insertUser(user, email, pass);

                        if (isInserted) {
                            // Save session matching old local variables
                            SessionManager session = new SessionManager(this);
                            session.saveSession(user, email);

                            getSharedPreferences("UserSession", MODE_PRIVATE)
                                    .edit()
                                    .putString("username", user)
                                    .putString("email", email)
                                    .putBoolean("detailsComplete", false)
                                    .apply();

                            Toast.makeText(this, "Welcome to ChordLab, " + user + "!", Toast.LENGTH_LONG).show();

                            // New user → go to Details page
                            Intent intent = new Intent(RegistrationActivity.this, DetailsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Local synchronization profile failed.", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        // Error handling matching your custom conditions
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Email is already registered!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
        });

        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        ivToggleConfirmPassword = findViewById(R.id.iv_toggle_confirm_password);

        // Toggle for Main Password
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPass.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_on);
            }
            if (etPass.getText() != null) etPass.setSelection(etPass.getText().length());
        });

        // Toggle for Confirm Password
        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                etConfirmPass.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etConfirmPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_on);
            }
            if (etConfirmPass.getText() != null) etConfirmPass.setSelection(etConfirmPass.getText().length());
        });
    }
}