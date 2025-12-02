package com.example.bicutanbites.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        setupRecyclerView();
        loadCartItems();

        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        return view;
    }

    private void setupRecyclerView() {
        // Implement the Interface directly here
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
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (item.getDocumentId() == null) return;

        int newQty = item.getQty() + change;

        if (newQty <= 0) {
            // Delete item if quantity goes to 0
            db.collection("users").document(uid).collection("cart")
                    .document(item.getDocumentId())
                    .delete();
        } else {
            // Update quantity
            db.collection("users").document(uid).collection("cart")
                    .document(item.getDocumentId())
                    .update("qty", newQty);
        }
        // Note: The SnapshotListener in loadCartItems will automatically refresh the UI
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
                                // Firestore @DocumentId annotation handles setting the ID,
                                // but we double check here if logic requires manual set
                                // item.setDocumentId(doc.getId());

                                cartItems.add(item);
                                totalPrice += (item.getPrice() * item.getQty());
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    tvTotal.setText(String.format(Locale.getDefault(), "₱%.2f", totalPrice));
                    btnPlaceOrder.setEnabled(!cartItems.isEmpty());
                });
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) return;

        btnPlaceOrder.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get Input Data
        String note = etNotes.getText().toString().trim();

        // Get Selected Radio Button Text
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

        // NEW FIELDS
        orderMap.put("note", note);
        orderMap.put("paymentMethod", paymentMethod);

        WriteBatch batch = db.batch();
        batch.set(db.collection("orders").document(orderId), orderMap);

        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Order Placed!", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) getActivity().finish();
                    });
                });
    }
}