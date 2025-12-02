package com.example.bicutanbites;

import android.content.Context;
import android.widget.Toast;

import com.example.bicutanbites.models.Product;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class CartHandler {

    private final String userId;
    private final FirebaseFirestore db;

    public CartHandler(String userId) {
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();
    }

    public void addToCart(Product product) {
        // Point to users -> [userID] -> cart
        CollectionReference cartRef = db.collection("users")
                .document(userId)
                .collection("cart");

        // Check if item already exists to avoid duplicates
        cartRef.whereEqualTo("name", product.getTitle())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            // OPTION A: Item exists, increment quantity
                            DocumentReference docRef = snapshot.getDocuments().get(0).getReference();
                            Long currentQty = snapshot.getDocuments().get(0).getLong("qty");
                            long newQty = (currentQty != null ? currentQty : 0) + 1;

                            docRef.update("qty", newQty);
                        } else {
                            // OPTION B: Item is new, add it
                            Map<String, Object> cartItem = new HashMap<>();
                            cartItem.put("name", product.getTitle());
                            // Ensure we store price as double
                            try {
                                // If Product stores price as string, parse it.
                                // If Product stores as double, just use product.getPrice()
                                cartItem.put("price", Double.parseDouble(String.valueOf(product.getPrice())));
                            } catch (Exception e) {
                                cartItem.put("price", 0.0);
                            }
                            cartItem.put("imageUrl", product.getImageUrl());
                            cartItem.put("qty", 1);

                            cartRef.add(cartItem);
                        }
                    }
                });
    }
}