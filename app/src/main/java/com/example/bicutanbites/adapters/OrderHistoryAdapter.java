package com.example.bicutanbites.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private final OrderActionListener listener; // NEW LISTENER

    // 1. Define Interface
    public interface OrderActionListener {
        void onCancelOrder(Order order);
    }

    // 2. Update Constructor to accept listener
    public OrderHistoryAdapter(Context context, List<Order> orders, OrderActionListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
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

        holder.orderId.setText("ID: " + order.getOrderId());
        holder.orderStatus.setText(order.getStatus());
        holder.orderTotal.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotal()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        if (order.getOrderDate() != null) {
            holder.orderDate.setText(dateFormat.format(order.getOrderDate()));
        }

        // Color code the status
        setStatusColor(holder.orderStatus, order.getStatus());

        // Note Visibility
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            holder.orderNote.setVisibility(View.VISIBLE);
            holder.orderNote.setText("Note: " + order.getNote());
        } else {
            holder.orderNote.setVisibility(View.GONE);
        }

        // --- 3. CANCEL BUTTON LOGIC ---
        // Only show if status is "Pending"
        if ("Pending".equalsIgnoreCase(order.getStatus())) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> listener.onCancelOrder(order));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        // Inner Recycler
        OrderItemAdapter itemAdapter = new OrderItemAdapter(context, order.getItems());
        holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.innerRecyclerView.setAdapter(itemAdapter);
    }

    private void setStatusColor(TextView view, String status) {
        // Reset default
        view.setBackgroundResource(R.drawable.category_chip_bg);

        switch (status) {
            case "Pending":
                view.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                break;
            case "Being Made":
                view.setBackgroundColor(Color.parseColor("#2196F3")); // Blue
                break;
            case "Being Delivered":
                view.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                break;
            case "Cancelled":
                view.setBackgroundColor(Color.parseColor("#F44336")); // Red
                break;
            default:
                view.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderStatus, orderDate, orderTotal, orderNote;
        Button btnCancel; // NEW BUTTON
        RecyclerView innerRecyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderStatus = itemView.findViewById(R.id.order_status);
            orderDate = itemView.findViewById(R.id.order_date);
            orderTotal = itemView.findViewById(R.id.order_total);
            orderNote = itemView.findViewById(R.id.order_note);
            innerRecyclerView = itemView.findViewById(R.id.recycler_order_items);

            // Find the button
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
        }
    }
}