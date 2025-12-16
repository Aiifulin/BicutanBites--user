package com.example.bicutanbites.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.bicutanbites.EditProfileActivity;
import com.example.bicutanbites.LoginActivity;
import com.example.bicutanbites.NotificationsActivity;
import com.example.bicutanbites.OrderHistoryActivity;
import com.example.bicutanbites.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";

    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private FirebaseAuth auth;
    private DocumentReference userDocRef; // Firestore reference for current user's document
    private String currentUid;

    public AccountFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Initialize UI components
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        TextView logoutBtn = view.findViewById(R.id.logout_button);

        LinearLayout personalInfoBtn = view.findViewById(R.id.personal_info_btn);
        LinearLayout myOrdersBtn = view.findViewById(R.id.my_orders_btn);
        LinearLayout notificationsBtn = view.findViewById(R.id.notifications_btn);

        auth = FirebaseAuth.getInstance();

        // Check for authenticated user and navigate to login if none exists
        if (auth.getCurrentUser() == null) {
            navigateToLogin();
            return view;
        }

        // Initialize Firestore reference using current user's UID
        currentUid = auth.getCurrentUser().getUid();
        userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUid);

        // --- Setup Navigation Listeners ---
        personalInfoBtn.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));

        // Navigate to Order History Activity
        myOrdersBtn.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), OrderHistoryActivity.class));
            }
        });

        // Navigate to Notifications Activity
        notificationsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });

        // Handle Logout
        logoutBtn.setOnClickListener(v -> logoutUser());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data every time the fragment becomes visible/active
        loadUserProfile();
    }

    // Fetches user data (name, email, profile image path) from Firestore
    private void loadUserProfile() {
        userDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String localImagePath = doc.getString("profileImage");

                profileName.setText(name);
                profileEmail.setText(email);

                // Load profile image using Glide, prioritizing local file cache
                if (localImagePath != null && !localImagePath.isEmpty()) {
                    File imageFile = new File(localImagePath);
                    if (imageFile.exists()) {
                        // Use file last modified time as a signature to ensure fresh load if file changes
                        Glide.with(this)
                                .load(imageFile)
                                .signature(new ObjectKey(imageFile.lastModified()))
                                .placeholder(R.drawable.ic_account_circle)
                                .error(R.drawable.ic_account_circle)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_account_circle);
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_account_circle);
                }

            } else {
                Log.w(TAG, "User document does not exist.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to load user info", e));
    }

    // Signs out the user and navigates to the Login screen
    private void logoutUser() {
        auth.signOut();
        Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    // Navigates to LoginActivity and finishes the current activity to clear the back stack
    private void navigateToLogin() {
        if (getActivity() != null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        }
    }
}