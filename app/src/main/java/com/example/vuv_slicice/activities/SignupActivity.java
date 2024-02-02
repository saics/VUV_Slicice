package com.example.vuv_slicice.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.vuv_slicice.R;
import com.example.vuv_slicice.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, usernameEditText, passwordEditText;
    private Button signupButton;
    private TextView loginRedirectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeUI();
        setupListeners();
    }

    private void initializeUI() {
        nameEditText = findViewById(R.id.signup_name);
        emailEditText = findViewById(R.id.signup_email);
        usernameEditText = findViewById(R.id.signup_username);
        passwordEditText = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
    }

    private void setupListeners() {
        signupButton.setOnClickListener(view -> attemptSignup());
        loginRedirectText.setOnClickListener(view -> navigateToLoginActivity());
    }

    private void attemptSignup() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInputs(name, email, username, password)) return;

        signupButton.setEnabled(false);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    signupButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        saveUserData(name, email, username);
                    } else {
                        Toast.makeText(SignupActivity.this, "Registracije nije uspjela: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String name, String email, String username, String password) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Ime i prezime ne smiju biti prazni", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email nije ispravan", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "Korisničko ime ne smije biti prazno", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Lozinka mora imati barem 6 znakova", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private void saveUserData(String name, String email, String username) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        User newUser = new User(name, email, username, false);

        usersRef.child(uid).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Registracija uspješna!", Toast.LENGTH_SHORT).show();
                        navigateToLoginActivity();
                    } else {
                        Toast.makeText(SignupActivity.this, "Registracija nije uspjela: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
