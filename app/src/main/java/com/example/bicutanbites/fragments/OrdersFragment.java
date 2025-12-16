package com.example.bicutanbites.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bicutanbites.R;
import com.example.bicutanbites.adapters.ActiveOrderAdapter;
import com.example.bicutanbites.models.Order;
import com.example.bicutanbites.models.OrderItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerActiveOrders;
    private TextView tvNoOrders;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ActiveOrderAdapter adapter;
    private final List<Order> activeOrdersList = new ArrayList<>();
    private ListenerRegistration firestoreListener; // Manages the real-time connection

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        // Initialize UI components
        recyclerActiveOrders = view.findViewById(R.id.recycler_active_orders);
        tvNoOrders = view.findViewById(R.id.tv_no_orders);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        // Setup RecyclerView
        recyclerActiveOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActiveOrderAdapter(getContext(), activeOrdersList);
        recyclerActiveOrders.setAdapter(adapter);

        // Start loading data
        loadActiveOrders();

        // Setup refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::loadActiveOrders);

        return view;
    }

    private void loadActiveOrders() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Remove previous listener before starting a new one
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        // Define the statuses that count as "active" for the user interface
        List<String> activeStatuses = Arrays.asList("Pending", "Being Made", "Being Delivered");

        // Establish the real-time Firestore listener
        firestoreListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userID", uid)
                .whereIn("status", activeStatuses) // Filter for active statuses
                .orderBy("orderedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (error != null || value == null) return;

                    activeOrdersList.clear();

                    // Toggle visibility of empty state vs. recycler view
                    if (value.isEmpty()) {
                        tvNoOrders.setVisibility(View.VISIBLE);
                        recyclerActiveOrders.setVisibility(View.GONE);
                    } else {
                        tvNoOrders.setVisibility(View.GONE);
                        recyclerActiveOrders.setVisibility(View.VISIBLE);

                        // Process incoming documents
                        for (DocumentSnapshot doc : value) {
                            String id = doc.getString("orderId");
                            Date date = doc.getDate("orderedAt");
                            String status = doc.getString("status");

                            // Firestore listener ensures only active statuses come through,
                            // but defensive checks ensure logic consistency.
                            if ("Completed".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                                continue;
                            }

                            Double total = doc.getDouble("total");
                            String note = doc.getString("note");

                            // Deserialize nested item list
                            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) doc.get("items");
                            List<OrderItem> items = new ArrayList<>();
                            if (rawItems != null) {
                                for (Map<String, Object> m : rawItems) {
                                    items.add(new OrderItem(
                                            (String) m.get("name"),
                                            ((Number) m.get("qty")).intValue(),
                                            ((Number) m.get("price")).doubleValue(),
                                            (String) m.get("imageUrl")
                                    ));
                                }
                            }
                            activeOrdersList.add(new Order(id, date, status, total != null ? total : 0.0, items, note));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Crucial: Remove the listener when the view is destroyed to prevent leaks
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}