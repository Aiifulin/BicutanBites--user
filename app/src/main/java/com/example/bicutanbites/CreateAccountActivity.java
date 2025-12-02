package com.example.bicutanbites;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date; // Import Date
import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccountActivity";

    private EditText nameInput, emailInput, passwordInput;
    private Button registerButton;
    private ImageButton backButton;
    private ImageView profilePreview;

    private Uri selectedImageUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int PICK_IMAGE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profilePreview = findViewById(R.id.profile_preview);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        registerButton = findViewById(R.id.register_button);
        backButton = findViewById(R.id.back_button);

        profilePreview.setOnClickListener(v -> openGallery());
        registerButton.setOnClickListener(v -> registerUser());
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            profilePreview.setImageURI(selectedImageUri);
        }
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String uid = user.getUid();
                    if (selectedImageUri != null) {
                        // Image was selected, save it locally
                        String localImagePath = saveImageToInternalStorage(selectedImageUri, uid);
                        saveUserToFirestore(uid, name, email, localImagePath);
                    } else {
                        // No image selected
                        saveUserToFirestore(uid, name, email, null);
                    }
                }
            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(this,
                        "Registration failed: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String saveImageToInternalStorage(Uri uri, String userId) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            // Create a file in the app's private directory
            File file = new File(getDir("profile_images", Context.MODE_PRIVATE), userId + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Image saved locally to: " + file.getAbsolutePath());
            return file.getAbsolutePath(); // Return the path

        } catch (Exception e) {
            Log.e(TAG, "Failed to save image locally", e);
            Toast.makeText(this, "Failed to save profile image.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void saveUserToFirestore(String uid, String name, String email, String imagePath) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("profileImage", imagePath);

        // Initialize empty fields to avoid null errors later
        userData.put("address", "");
        userData.put("phoneNumber", "");

        // Add the DateCreated field (Saves current timestamp)
        userData.put("dateCreated", new Date());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}