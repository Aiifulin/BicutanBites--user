package com.example.bicutanbites.fragments;

import android.app.AlertDialog; // Import for the dialog
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bicutanbites.EditProfileActivity; // Import this so we can redirect user
import com.example.bicutanbites.R;
import com.example.bicutanbites.adapters.CheckoutAdapter;
import com.example.bicutanbites.models.CheckoutItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartFragment extends Fragment {

    private RecyclerView recyclerCart;
    private View emptyCartView;
    private View cartContentView;
    private View cartBottomBar;
    private TextView tvTotal;
    private Button btnPlaceOrder;
    private EditText etNotes;
    private RadioGroup rgPayment;

    private CheckoutAdapter adapter;
    private List<CheckoutItem> cartItems = new ArrayList<>();
    private double totalPrice = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        recyclerCart = view.findViewById(R.id.recycler_cart);
        tvTotal = view.findViewById(R.id.tv_total_price);
        btnPlaceOrder = view.findViewById(R.id.btn_place_order);
        etNotes = view.findViewById(R.id.et_order_note);
        rgPayment = view.findViewById(R.id.rg_payment_method);

        emptyCartView = view.findViewById(R.id.empty_cart_view);
        cartContentView = view.findViewById(R.id.cart_content_view);
        cartBottomBar = view.findViewById(R.id.cart_bottom_bar);

        setupRecyclerView();
        loadCartItems();

        btnPlaceOrder.setOnClickListener(v -> checkAddressAndPlaceOrder());
        ImageButton btnBack = view.findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> {
            // If this fragment is inside CheckoutActivity, this will close it
            // and return the user to the Home screen.
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        return view;
    }

    private void setupRecyclerView() {
        adapter = new CheckoutAdapter(getContext(), cartItems, new CheckoutAdapter.CartActionListener() {
            @Override
            public void onIncrease(CheckoutItem item) {
                updateQuantity(item, 1);
            }

            @Override
            public void onDecrease(CheckoutItem item) {
                updateQuantity(item, -1);
            }
        });

        recyclerCart.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerCart.setAdapter(adapter);
    }

    private void updateQuantity(CheckoutItem item, int change) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (item.getDocumentId() == null) return;

        int newQty = item.getQty() + change;

        if (newQty < 1) return; // Prevent going below 1

        db.collection("users").document(uid).collection("cart")
                .document(item.getDocumentId())
                .update("qty", newQty);
    }

    private void loadCartItems() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).collection("cart")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    cartItems.clear();
                    totalPrice = 0.0;

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            CheckoutItem item = doc.toObject(CheckoutItem.class);
                            if (item != null) {
                                cartItems.add(item);
                                totalPrice += (item.getPrice() * item.getQty());
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    tvTotal.setText(String.format(Locale.getDefault(), "₱%.2f", totalPrice));

                    // --- TOGGLE VISIBILITY LOGIC ---
                    if (cartItems.isEmpty()) {
                        // Cart is Empty: Show "Empty View", Hide everything else
                        emptyCartView.setVisibility(View.VISIBLE);
                        cartContentView.setVisibility(View.GONE);
                        cartBottomBar.setVisibility(View.GONE);
                    } else {
                        // Cart has Items: Hide "Empty View", Show everything
                        emptyCartView.setVisibility(View.GONE);
                        cartContentView.setVisibility(View.VISIBLE);
                        cartBottomBar.setVisibility(View.VISIBLE);
                        btnPlaceOrder.setEnabled(true);
                    }
                });
    }

    // --- NEW LOGIC STARTS HERE ---

    private void checkAddressAndPlaceOrder() {
        if (cartItems.isEmpty()) return;

        btnPlaceOrder.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String address = documentSnapshot.getString("address");

                        // --- UPDATED VALIDATION LOGIC ---
                        if (address == null || address.trim().isEmpty()) {
                            // CASE 1: Completely Empty
                            showAddressDialog("Missing Address",
                                    "You do not have a delivery address saved.");
                            btnPlaceOrder.setEnabled(true);
                        }
                        else if (!isAddressValid(address)) {
                            // CASE 2: Invalid Format (Too short or missing numbers)
                            showAddressDialog("Invalid Address",
                                    "Your address is too short or vague. Please include your House/Unit Number and Street Name.");
                            btnPlaceOrder.setEnabled(true);
                        }
                        else {
                            // CASE 3: Valid -> Place Order
                            performPlaceOrder(db, uid, address);
                        }
                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        btnPlaceOrder.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPlaceOrder.setEnabled(true);
                });
    }

    // Updated to accept Title and Message
    private void showAddressDialog(String title, String message) {
        if (getContext() == null) return;

        // 1. Create the dialog but don't show it yet
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Update Profile", (d, which) -> {
                    Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .create();

        // 2. Show the dialog
        dialog.show();

        // 3. Customize the buttons (Must be done AFTER dialog.show())

        // Import: android.widget.Button and android.graphics.Color
        Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // A. Remove the Orange Background (Reset to transparent)
        positiveBtn.setBackground(null);
        negativeBtn.setBackground(null);

        // B. Change the Text Color to Black/Grey (instead of Orange)
        positiveBtn.setTextColor(android.graphics.Color.BLACK);
        negativeBtn.setTextColor(android.graphics.Color.DKGRAY);
    }

    private void performPlaceOrder(FirebaseFirestore db, String uid, String deliveryAddress) {
        // Get Input Data
        String note = etNotes.getText().toString().trim();

        int selectedId = rgPayment.getCheckedRadioButtonId();
        RadioButton selectedBtn = rgPayment.findViewById(selectedId);
        String paymentMethod = (selectedBtn != null) ? selectedBtn.getText().toString() : "Cash";

        // Create Order Map
        String orderId = db.collection("orders").document().getId();
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", orderId);
        orderMap.put("userID", uid);
        orderMap.put("orderedAt", new Date());
        orderMap.put("status", "Pending");
        orderMap.put("total", totalPrice);
        orderMap.put("items", cartItems);
        orderMap.put("note", note);
        orderMap.put("paymentMethod", paymentMethod);

        // Save the address in the order too, in case the user changes it later!
        orderMap.put("deliveryAddress", deliveryAddress);

        WriteBatch batch = db.batch();
        batch.set(db.collection("orders").document(orderId), orderMap);

        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Order Placed Successfully!", Toast.LENGTH_LONG).show();
                        if (getActivity() != null) getActivity().finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to place order.", Toast.LENGTH_SHORT).show();
                        btnPlaceOrder.setEnabled(true);
                    });
                });
    }
    private boolean isAddressValid(String address) {
        if (address == null) return false;
        String trimmed = address.trim();

        // Rule 1: Must be at least 10 characters long
        // (e.g. "123 Main St" is 11 chars. "Taguig" is only 6 chars, which is too vague for delivery)
        if (trimmed.length() < 10) return false;

        // Rule 2: Must contain at least one digit (House number or Zip code)
        if (!trimmed.matches(".*\\d.*")) return false;

        // Rule 3: Must contain at least one letter (Prevents random phone numbers like "091234567")
        if (!trimmed.matches(".*[a-zA-Z].*")) return false;

        return true;
    }
}