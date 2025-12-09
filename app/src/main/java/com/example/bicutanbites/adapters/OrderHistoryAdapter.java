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
import java.util.TimeZone;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orders;

    // Listener is no longer strictly needed for history, but we keep the structure simple
    // You can remove it entirely if you want a cleaner cleanup

    public OrderHistoryAdapter(Context context, List<Order> orders, Object listenerIgnored) {
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

        holder.orderId.setText("ID: " + order.getOrderId());
        holder.orderStatus.setText(order.getStatus());
        holder.orderTotal.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotal()));

        // TimeZone Fix
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        if (order.getOrderDate() != null) {
            holder.orderDate.setText(dateFormat.format(order.getOrderDate()));
        }

        // Color coding
        setStatusColor(holder.orderStatus, order.getStatus());

        // Note Visibility
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            holder.orderNote.setVisibility(View.VISIBLE);
            holder.orderNote.setText("Note: " + order.getNote());
        } else {
            holder.orderNote.setVisibility(View.GONE);
        }

        // --- CHANGE: Always Hide Cancel Button in History ---
        // Since this adapter is now only for History (Completed/Cancelled),
        // we never show the cancel button.
        holder.btnCancel.setVisibility(View.GONE);

        // Inner Recycler
        OrderItemAdapter itemAdapter = new OrderItemAdapter(context, order.getItems());
        holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.innerRecyclerView.setAdapter(itemAdapter);
    }

    private void setStatusColor(TextView view, String status) {
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
            default: // Includes "Completed"
                view.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderStatus, orderDate, orderTotal, orderNote;
        Button btnCancel;
        RecyclerView innerRecyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderStatus = itemView.findViewById(R.id.order_status);
            orderDate = itemView.findViewById(R.id.order_date);
            orderTotal = itemView.findViewById(R.id.order_total);
            orderNote = itemView.findViewById(R.id.order_note);
            innerRecyclerView = itemView.findViewById(R.id.recycler_order_items);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
        }
    }
}