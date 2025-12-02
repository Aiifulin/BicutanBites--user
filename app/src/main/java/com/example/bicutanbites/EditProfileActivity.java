package com.example.bicutanbites;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText editName, editEmail, editAddress;
    private Button saveButton, changePasswordButton;
    private ImageView profileImage;

    private FirebaseAuth mAuth;
    private DocumentReference userDocRef;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageSelection
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());

        profileImage = findViewById(R.id.profile_image);
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editAddress = findViewById(R.id.edit_address);
        saveButton = findViewById(R.id.save_button);
        changePasswordButton = findViewById(R.id.change_password_button);

        loadUserData();

        profileImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        saveButton.setOnClickListener(v -> saveProfileChanges());
        changePasswordButton.setOnClickListener(v -> showReauthenticateDialog());
    }

    private void loadUserData() {
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String address = documentSnapshot.getString("address");
                String imagePath = documentSnapshot.getString("profileImage");

                editName.setText(name);
                editEmail.setText(email);
                editAddress.setText(address);

                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                    Glide.with(this)
                            .load(imageFile)
                            .signature(new ObjectKey(imageFile.lastModified()))
                            .into(profileImage);
                }
            }
        });
    }

    private void handleImageSelection(Uri uri) {
        if (uri != null) {
            selectedImageUri = uri;
            // Use Glide to prevent memory crashes with large images
            Glide.with(this).load(uri).into(profileImage);
        }
    }

    private void saveProfileChanges() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("address", address);

        if (selectedImageUri != null) {
            String imagePath = saveImageToInternalStorage(selectedImageUri, mAuth.getCurrentUser().getUid());
            if (imagePath != null) {
                updates.put("profileImage", imagePath);
            }
        }

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void showReauthenticateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_reauthenticate, null);
        builder.setView(dialogView);

        TextInputEditText currentPassword = dialogView.findViewById(R.id.current_password);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button confirmButton = dialogView.findViewById(R.id.confirm_button);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(v -> {
            String password = currentPassword.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show();
                return;
            }
            reauthenticateUser(password, dialog);
        });

        dialog.show();
    }

    private void reauthenticateUser(String password, Dialog parentDialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                parentDialog.dismiss();
                showChangePasswordDialog();
            } else {
                Toast.makeText(EditProfileActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        TextInputEditText newPassword = dialogView.findViewById(R.id.new_password);
        TextInputEditText confirmNewPassword = dialogView.findViewById(R.id.confirm_new_password);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button saveButton = dialogView.findViewById(R.id.save_button);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> {
            String newPass = newPassword.getText().toString();
            String confirmPass = confirmNewPassword.getText().toString();

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill out both fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePassword(newPass, dialog);
        });

        dialog.show();
    }

    private void updatePassword(String newPassword, Dialog parentDialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                parentDialog.dismiss();
                Toast.makeText(EditProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String saveImageToInternalStorage(Uri uri, String userId) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getDir("profile_images", Context.MODE_PRIVATE), userId + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

            outputStream.close();
            inputStream.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to save profile image.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
