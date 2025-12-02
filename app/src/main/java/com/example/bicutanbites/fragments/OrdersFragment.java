package com.example.bicutanbites.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bicutanbites.R;
import com.example.bicutanbites.adapters.OrderHistoryAdapter;
import com.example.bicutanbites.models.Order;
import com.example.bicutanbites.models.OrderItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // Import this
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerOrders;
    private OrderHistoryAdapter adapter;
    private List<Order> orderHistory = new ArrayList<>();
    private TextView emptyView;

    // We need this to stop listening when the user leaves the screen
    private ListenerRegistration firestoreListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        recyclerOrders = view.findViewById(R.id.recycler_orders);
        emptyView = view.findViewById(R.id.empty_view);

        setupRecyclerView();

        // We call this here to start listening
        loadOrdersRealTime();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter(getContext(), orderHistory);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerOrders.setAdapter(adapter);
    }

    private void loadOrdersRealTime() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerOrders.setVisibility(View.GONE);
            return;
        }

        String currentUserId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // CHANGED: .get() to .addSnapshotListener()
        firestoreListener = db.collection("orders")
                .whereEqualTo("userID", currentUserId)
                .orderBy("orderedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (value == null) return;

                    orderHistory.clear();

                    if (value.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerOrders.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerOrders.setVisibility(View.VISIBLE);
                    }

                    for (DocumentSnapshot doc : value) {
                        String id = doc.getId();
                        Date date = doc.getDate("orderedAt");
                        String status = doc.getString("status");

                        // Add safety check for total price
                        Double totalObj = doc.getDouble("total");
                        double total = totalObj != null ? totalObj : 0.0;
                        String note = doc.getString("note");

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) doc.get("items");
                        List<OrderItem> items = new ArrayList<>();

                        if (rawItems != null) {
                            for (Map<String, Object> m : rawItems) {
                                String name = (String) m.get("name");
                                String image = (String) m.get("imageUrl");

                                Number qtyNum = (Number) m.get("qty");
                                int qty = qtyNum != null ? qtyNum.intValue() : 0;

                                Number priceNum = (Number) m.get("price");
                                double price = priceNum != null ? priceNum.doubleValue() : 0.0;

                                items.add(new OrderItem(name, qty, price, image));
                            }
                        }

                        // Updated to include total price in constructor
                        orderHistory.add(new Order(id, date, status, total, items, note));
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // STOP listening when the fragment is destroyed to save battery/data
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}