package com.example.bicutanbites;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationsActivity extends AppCompatActivity {

    private SwitchCompat switchOrderUpdates;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // 1. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Initialize Switch
        switchOrderUpdates = findViewById(R.id.switch_order_updates);

        // 3. Load Saved State (Default is true)
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isEnabled = sharedPreferences.getBoolean("order_notifications", true);
        switchOrderUpdates.setChecked(isEnabled);

        // 4. Handle Toggle Changes
        switchOrderUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 1. Save locally (Keep this)
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("order_notifications", isChecked);
            editor.apply();

            // 2. Sync to Firebase (Add this)
            updateNotificationPreferenceInFirebase(isChecked); // <--- NEW

            String status = isChecked ? "Enabled" : "Disabled";
            Toast.makeText(NotificationsActivity.this, "Order updates " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateNotificationPreferenceInFirebase(boolean isEnabled) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Update the user's document
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("receiveOrderNotifications", isEnabled)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to sync setting", Toast.LENGTH_SHORT).show()
                );
    }
}