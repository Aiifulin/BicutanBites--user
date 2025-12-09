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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import this

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
    private SwipeRefreshLayout swipeRefreshLayout; // Declare SwipeRefreshLayout

    private ActiveOrderAdapter adapter;
    private List<Order> activeOrdersList = new ArrayList<>();
    private ListenerRegistration firestoreListener; // To handle restarts

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        recyclerActiveOrders = view.findViewById(R.id.recycler_active_orders);
        tvNoOrders = view.findViewById(R.id.tv_no_orders);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh); // Bind View

        recyclerActiveOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActiveOrderAdapter(getContext(), activeOrdersList);
        recyclerActiveOrders.setAdapter(adapter);

        // Load initially
        loadActiveOrders();

        // Handle "Swipe to Refresh"
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadActiveOrders(); // Reloads the data
        });

        return view;
    }

    private void loadActiveOrders() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // If we are refreshing, remove the old listener first to avoid duplicates
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        List<String> activeStatuses = Arrays.asList("Pending", "Being Made", "Being Delivered");

        firestoreListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userID", uid)
                .whereIn("status", activeStatuses)
                .orderBy("orderedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    // Stop the refresh animation once data arrives
                    swipeRefreshLayout.setRefreshing(false);

                    if (error != null || value == null) return;

                    activeOrdersList.clear();

                    if (value.isEmpty()) {
                        tvNoOrders.setVisibility(View.VISIBLE);
                        recyclerActiveOrders.setVisibility(View.GONE);
                    } else {
                        tvNoOrders.setVisibility(View.GONE);
                        recyclerActiveOrders.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : value) {
                            String id = doc.getString("orderId");
                            Date date = doc.getDate("orderedAt");
                            String status = doc.getString("status");

                            // Because of whereIn query, we don't need manual status filtering anymore.
                            // If status changes to 'Cancelled', Firestore removes it from 'value' automatically.

                            Double total = doc.getDouble("total");
                            String note = doc.getString("note");

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
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}