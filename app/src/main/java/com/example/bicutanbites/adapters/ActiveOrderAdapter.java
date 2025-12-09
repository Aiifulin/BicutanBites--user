package com.example.bicutanbites.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bicutanbites.R;
import com.example.bicutanbites.models.Order;
import com.example.bicutanbites.models.OrderItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ActiveOrderAdapter extends RecyclerView.Adapter<ActiveOrderAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orders;

    public ActiveOrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        String status = order.getStatus();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

        holder.tvDate.setText(order.getOrderDate() != null ? sdf.format(order.getOrderDate()) : "");
        holder.tvOrderId.setText(order.getOrderId());
        holder.tvStatus.setText(status);

        // --- Status Color Change Logic ---
        int statusColor;

        // Use Integer colors from resources for broader compatibility
        if ("Pending".equalsIgnoreCase(status)) {
            statusColor = context.getResources().getColor(android.R.color.holo_orange_dark);
        } else if ("Being Made".equalsIgnoreCase(status)) {
            statusColor = context.getResources().getColor(android.R.color.holo_blue_dark);
        } else if ("Being Delivered".equalsIgnoreCase(status)) {
            statusColor = context.getResources().getColor(android.R.color.holo_purple);
        } else if ("Completed".equalsIgnoreCase(status)) {
            statusColor = context.getResources().getColor(android.R.color.holo_green_dark);
        } else {
            // Default or Cancelled
            statusColor = context.getResources().getColor(android.R.color.darker_gray);
        }

        holder.tvStatus.setTextColor(statusColor);
        // --- End Status Color Change Logic ---

        // Prices
        double deliveryFee = 50.00;
        double subtotal = order.getTotal() - deliveryFee;
        holder.tvSubtotal.setText(String.format(Locale.getDefault(), "₱%.2f", subtotal));
        holder.tvTotal.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotal()));

        // Note
        if (order.getNote() != null && !order.getNote().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText("Note: " + order.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // Nested Items
        OrderItemAdapter itemAdapter = new OrderItemAdapter(context, order.getItems());
        holder.recyclerItems.setLayoutManager(new LinearLayoutManager(context));
        holder.recyclerItems.setAdapter(itemAdapter);


        // --- Cancel Button Gray Out and Disclaimer Logic ---
        if ("Pending".equalsIgnoreCase(status)) {
            // Active state
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(true);
            holder.btnCancel.setText("Cancel Order");
            holder.tvDisclaimer.setVisibility(View.GONE);

            // Set button appearance to normal/primary color (R.color.brand_orange must exist)
            holder.btnCancel.setBackgroundTintList(context.getResources().getColorStateList(R.color.brand_orange));

            holder.btnCancel.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    showCancelDialog(order, currentPos);
                }
            });
        }
        else {
            // Inactive/Grayed-out state (Being Made, Being Delivered, etc.)
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(false);
            holder.btnCancel.setText("Cancel Order");

            // Set button appearance to gray
            holder.btnCancel.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.darker_gray));
            holder.tvDisclaimer.setVisibility(View.VISIBLE); // Show disclaimer

            holder.btnCancel.setOnClickListener(null); // Remove click listener
        }
        // --- End Cancel Button Logic ---
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private void showCancelDialog(Order order, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_cancel_order, null);
        builder.setView(dialogView);

        Button btnYes = dialogView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_dialog_no);

        AlertDialog dialog = builder.create();

        btnYes.setOnClickListener(v -> {
            performCancel(order, position);
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performCancel(Order order, int position) {
        FirebaseFirestore.getInstance().collection("orders")
                .document(order.getOrderId())
                .update("status", "Cancelled")
                .addOnSuccessListener(a -> {
                    Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to cancel order.", Toast.LENGTH_LONG).show();
                });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvOrderId, tvStatus, tvSubtotal, tvTotal, tvNote;
        RecyclerView recyclerItems;
        Button btnCancel;
        TextView tvDisclaimer; // NEW: Disclaimer TextView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_header_date);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
            tvTotal = itemView.findViewById(R.id.tv_total_final);
            tvNote = itemView.findViewById(R.id.tv_order_note);
            recyclerItems = itemView.findViewById(R.id.recycler_order_summary);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
            tvDisclaimer = itemView.findViewById(R.id.tv_cancel_disclaimer); // NEW
        }
    }
}