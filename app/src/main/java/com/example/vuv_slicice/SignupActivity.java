package com.example.vuv_slicice;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        nameEditText = findViewById(R.id.signup_name);
        emailEditText = findViewById(R.id.signup_email);
        usernameEditText = findViewById(R.id.signup_username);
        passwordEditText = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        signupButton.setOnClickListener(view -> signupUser());

        loginRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Finish the SignupActivity to prevent going back to it
        });
    }

    private void signupUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // TODO: Add Firebase Authentication logic for signup
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Signup successful
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        // TODO: Add user data to Realtime Database
                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
                        String uid = user.getUid();

                        // Create a new user object with the provided name and email
                        User newUser = new User(name, email, username);

                        // Add the new user to the "users" node using the UID as the key
                        usersRef.child(uid).setValue(newUser);

                        // Handle successful signup
                        Toast.makeText(SignupActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();

                        // Navigate to the LoginActivity
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish(); // Finish the SignupActivity so that pressing back won't go back to it
                    } else {
                        // If signup fails, display a message to the user.
                        Toast.makeText(SignupActivity.this, "Registration failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

