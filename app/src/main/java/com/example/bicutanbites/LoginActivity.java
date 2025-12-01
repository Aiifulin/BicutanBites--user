package com.example.bicutanbites;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;


public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Find UI elements
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);

        // Login button event
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            if (firebaseUser != null) {
                                String uid = firebaseUser.getUid();

                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener(document -> {

                                            if (document.exists()) {
                                                String name = document.getString("name");
                                                String emailStored = document.getString("email");

                                                // You can pass user data to next page if needed
                                                Intent intent = new Intent(this, HomeActivity.class);
                                                intent.putExtra("userId", uid);
                                                intent.putExtra("userName", name);
                                                intent.putExtra("userEmail", emailStored);

                                                Toast.makeText(this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();

                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(this,
                                                        "User record not found in Firestore!",
                                                        Toast.LENGTH_LONG).show();
                                            }

                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this,
                                                        "Firestore error: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show()
                                        );
                            }

                        } else {
                            Toast.makeText(this,
                                    "Invalid credentials: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        Button createAccountBtn = findViewById(R.id.create_account_button);

        createAccountBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateAccountActivity.class));
        });
    }



//    @Override
//    protected void onStart() {
//        super.onStart();
//        // Auto-login if already authenticated
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null) {
//            startActivity(new Intent(this, HomeActivity.class));
//            finish();
//        }
//    }
}
