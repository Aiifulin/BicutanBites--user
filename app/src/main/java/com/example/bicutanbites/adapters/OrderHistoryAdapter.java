package com.example.bicutanbites.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bicutanbites.R;
import com.example.bicutanbites.models.Order;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orders;

    public OrderHistoryAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.orderId.setText(order.getOrderId());
        holder.orderStatus.setText(order.getStatus());
        holder.orderTotal.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotal()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        if (order.getOrderDate() != null) {
            holder.orderDate.setText(dateFormat.format(order.getOrderDate()));
        }

        // --- NEW: Handle Note Visibility ---
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            holder.orderNote.setVisibility(View.VISIBLE);
            holder.orderNote.setText("Note: " + order.getNote());
        } else {
            holder.orderNote.setVisibility(View.GONE);
        }

        // Inner Recycler
        OrderItemAdapter itemAdapter = new OrderItemAdapter(context, order.getItems());
        holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.innerRecyclerView.setAdapter(itemAdapter);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderStatus, orderDate, orderTotal, orderNote; // Added orderNote
        RecyclerView innerRecyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderStatus = itemView.findViewById(R.id.order_status);
            orderDate = itemView.findViewById(R.id.order_date);
            orderTotal = itemView.findViewById(R.id.order_total);
            innerRecyclerView = itemView.findViewById(R.id.recycler_order_items);

            // Link the new view
            orderNote = itemView.findViewById(R.id.order_note);
        }
    }
}