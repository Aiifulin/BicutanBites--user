package com.example.bicutanbites.fragments;

import android.app.AlertDialog;
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

import com.example.bicutanbites.EditProfileActivity;
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
    private final List<CheckoutItem> cartItems = new ArrayList<>();
    private double totalPrice = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        // Initialize UI components
        recyclerCart = view.findViewById(R.id.recycler_cart);
        tvTotal = view.findViewById(R.id.tv_total_price);
        btnPlaceOrder = view.findViewById(R.id.btn_place_order);
        etNotes = view.findViewById(R.id.et_order_note);
        rgPayment = view.findViewById(R.id.rg_payment_method);

        emptyCartView = view.findViewById(R.id.empty_cart_view);
        cartContentView = view.findViewById(R.id.cart_content_view);
        cartBottomBar = view.findViewById(R.id.cart_bottom_bar);

        setupRecyclerView();
        loadCartItems(); // Start real-time listener for cart data

        btnPlaceOrder.setOnClickListener(v -> checkAddressAndPlaceOrder());
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        return view;
    }

    private void setupRecyclerView() {
        // Setup adapter with listeners for cart item actions
        adapter = new CheckoutAdapter(getContext(), cartItems, new CheckoutAdapter.CartActionListener() {
            @Override
            public void onIncrease(CheckoutItem item) {
                updateQuantity(item, 1);
            }

            @Override
            public void onDecrease(CheckoutItem item) {
                updateQuantity(item, -1);
            }

            @Override
            public void onDelete(CheckoutItem item) {
                showDeleteConfirmation(item);
            }
        });

        recyclerCart.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerCart.setAdapter(adapter);
    }

    // Displays confirmation dialog before deleting an item
    private void showDeleteConfirmation(CheckoutItem item) {
        if (getContext() == null) return;

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove " + item.getName() + " from your cart?")
                .setPositiveButton("Remove", (d, which) -> {
                    deleteItemFromFirestore(item);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Customizes dialog button styling
        android.widget.Button positiveBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negativeBtn = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);

        if (positiveBtn != null) {
            positiveBtn.setBackground(null);
            positiveBtn.setTextColor(android.graphics.Color.RED);
        }
        if (negativeBtn != null) {
            negativeBtn.setBackground(null);
            negativeBtn.setTextColor(android.graphics.Color.DKGRAY);
        }
    }

    // Removes item document from the user's cart collection in Firestore
    private void deleteItemFromFirestore(CheckoutItem item) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || item.getDocumentId() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("cart")
                .document(item.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show()
                );
    }

    // Updates the quantity of a cart item directly in Firestore
    private void updateQuantity(CheckoutItem item, int change) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || item.getDocumentId() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        int newQty = item.getQty() + change;

        if (newQty < 1) return; // Must have at least one item

        db.collection("users").document(uid).collection("cart")
                .document(item.getDocumentId())
                .update("qty", newQty);
    }

    // Establishes a real-time listener to the user's cart collection
    private void loadCartItems() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).collection("cart")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    cartItems.clear();
                    totalPrice = 0.0;

                    // Rebuild cart list and recalculate total price
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

                    // --- TOGGLE VISIBILITY ---
                    if (cartItems.isEmpty()) {
                        emptyCartView.setVisibility(View.VISIBLE);
                        cartContentView.setVisibility(View.GONE);
                        cartBottomBar.setVisibility(View.GONE);
                    } else {
                        emptyCartView.setVisibility(View.GONE);
                        cartContentView.setVisibility(View.VISIBLE);
                        cartBottomBar.setVisibility(View.VISIBLE);
                        btnPlaceOrder.setEnabled(true);
                    }
                });
    }

    // --- ORDER PLACEMENT VALIDATION AND EXECUTION ---

    private void checkAddressAndPlaceOrder() {
        if (cartItems.isEmpty() || FirebaseAuth.getInstance().getCurrentUser() == null) return;

        btnPlaceOrder.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Fetch user's address and phone number
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String address = documentSnapshot.getString("address");
                        String phone = documentSnapshot.getString("phoneNumber");

                        // 2. Perform validation checks
                        if (address == null || address.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
                            showAddressDialog("Missing Information", "You must provide your Home Address and Phone Number in your profile.");
                            btnPlaceOrder.setEnabled(true);
                        } else if (!isAddressValid(address)) {
                            showAddressDialog("Invalid Address", "Your address is too short or vague. Please update your full address in your profile.");
                            btnPlaceOrder.setEnabled(true);
                        } else {
                            // 3. If valid, proceed to place order
                            performPlaceOrder(db, uid, address, phone);
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

    // Displays a dialog prompting the user to update their profile information
    private void showAddressDialog(String title, String message) {
        if (getContext() == null) return;

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Update Profile", (d, which) -> {
                    Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Customizes button colors after dialog is shown
        Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveBtn != null) {
            positiveBtn.setBackground(null);
            positiveBtn.setTextColor(android.graphics.Color.BLACK);
        }
        if (negativeBtn != null) {
            negativeBtn.setBackground(null);
            negativeBtn.setTextColor(android.graphics.Color.DKGRAY);
        }
    }

    private void performPlaceOrder(FirebaseFirestore db, String uid, String deliveryAddress, String contactPhone) {
        // Collect order details
        String note = etNotes.getText().toString().trim();
        int selectedId = rgPayment.getCheckedRadioButtonId();
        RadioButton selectedBtn = rgPayment.findViewById(selectedId);
        String paymentMethod = (selectedBtn != null) ? selectedBtn.getText().toString() : "Cash";

        // Create Order document data map
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
        orderMap.put("deliveryAddress", deliveryAddress); // Include address/phone in the order snapshot
        orderMap.put("contactPhone", contactPhone);

        WriteBatch batch = db.batch();
        // 1. Set the new order document
        batch.set(db.collection("orders").document(orderId), orderMap);

        // 2. Delete all items in the user's cart (transactional cleanup)
        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    // Commit both operations (write order and delete cart)
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Order Placed Successfully!", Toast.LENGTH_LONG).show();
                        if (getActivity() != null) getActivity().finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to place order.", Toast.LENGTH_SHORT).show();
                        btnPlaceOrder.setEnabled(true);
                    });
                });
    }

    // Simple validation rules to ensure the address is reasonably detailed
    private boolean isAddressValid(String address) {
        if (address == null) return false;
        String trimmed = address.trim();

        // Rules: must be descriptive (length, contain number/letter)
        if (trimmed.length() < 10) return false;
        if (!trimmed.matches(".*\\d.*")) return false; // Contains digit (house number)
        if (!trimmed.matches(".*[a-zA-Z].*")) return false; // Contains letters (street name)

        return true;
    }
}