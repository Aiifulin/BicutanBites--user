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
    private List<Order> activeOrdersList = new ArrayList<>();
    private ListenerRegistration firestoreListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        recyclerActiveOrders = view.findViewById(R.id.recycler_active_orders);
        tvNoOrders = view.findViewById(R.id.tv_no_orders);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        recyclerActiveOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActiveOrderAdapter(getContext(), activeOrdersList);
        recyclerActiveOrders.setAdapter(adapter);

        loadActiveOrders();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadActiveOrders();
        });

        return view;
    }

    private void loadActiveOrders() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        // CORRECT: Statuses that should be visible in the Active Orders list
        // This is the Firestore filter. If the DB status changes to Completed,
        // the item should drop out of this listener's result set.
        List<String> activeStatuses = Arrays.asList("Pending", "Being Made", "Being Delivered");

        firestoreListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userID", uid)
                .whereIn("status", activeStatuses)
                .orderBy("orderedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
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

                            // NEW DEFENSIVE CHECK: This should not be needed if Firestore works,
                            // but it ensures that no completed/cancelled order makes it into the list.
                            if ("Completed".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                                continue; // Skip and do not add to active list
                            }

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
        // Crucial: Remove the listener when the view is destroyed
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}