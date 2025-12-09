package com.example.bicutanbites;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bicutanbites.adapters.OrderHistoryAdapter;
import com.example.bicutanbites.models.Order;
import com.example.bicutanbites.models.OrderItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private OrderHistoryAdapter adapter;
    private List<Order> orderHistory = new ArrayList<>();
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        recyclerOrders = findViewById(R.id.recycler_orders);
        emptyView = findViewById(R.id.empty_view);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // Setup Recycler (No cancel listener needed anymore)
        adapter = new OrderHistoryAdapter(this, orderHistory, null);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(adapter);

        loadCompletedOrders();
    }

    private void loadCompletedOrders() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userID", uid)
                .whereIn("status", java.util.Arrays.asList("Completed", "Cancelled")) // <--- UPDATED
                .orderBy("orderedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    orderHistory.clear();
                    if (value.isEmpty()) {
                        emptyView.setVisibility(android.view.View.VISIBLE);
                        recyclerOrders.setVisibility(android.view.View.GONE);
                    } else {
                        emptyView.setVisibility(android.view.View.GONE);
                        recyclerOrders.setVisibility(android.view.View.VISIBLE);
                    }

                    for (DocumentSnapshot doc : value) {
                        // ... (Parsing logic remains the same) ...
                        String id = doc.getString("orderId");
                        Date date = doc.getDate("orderedAt");
                        String status = doc.getString("status");
                        Double totalObj = doc.getDouble("total");
                        double total = totalObj != null ? totalObj : 0.0;
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
                        orderHistory.add(new Order(id, date, status, total, items, note));
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}