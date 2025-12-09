package com.example.bicutanbites.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
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
import com.example.bicutanbites.models.OrderItem; // Make sure this is imported
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List; // Explicit List import
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

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

        holder.tvDate.setText(order.getOrderDate() != null ? sdf.format(order.getOrderDate()) : "");
        holder.tvOrderId.setText(order.getOrderId());
        holder.tvStatus.setText(order.getStatus());

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

        // Cancel Button Logic
        if ("Pending".equalsIgnoreCase(order.getStatus())) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(true);
            holder.btnCancel.setText("Cancel Order");

            holder.btnCancel.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    showCancelDialog(order, currentPos);
                }
            });
        }
        else if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
            // Visual state during the 2-second delay
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(false);
            holder.btnCancel.setText("Cancelling...");
            holder.btnCancel.setTextColor(android.graphics.Color.GRAY);
        }
        else {
            holder.btnCancel.setVisibility(View.GONE);
        }
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
            performDelayedCancel(order, position); // Correctly call the new method
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performDelayedCancel(Order order, int position) {
        // 1. UPDATE VISUALLY FIRST
        order.setStatus("Cancelled");
        notifyItemChanged(position); // Updates the UI immediately

        // 2. WAIT 2 SECONDS, THEN UPDATE DATABASE
        new Handler().postDelayed(() -> {
            FirebaseFirestore.getInstance().collection("orders")
                    .document(order.getOrderId())
                    .update("status", "Cancelled")
                    .addOnSuccessListener(a -> {
                        Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show();
                    });
        }, 2000);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvOrderId, tvStatus, tvSubtotal, tvTotal, tvNote;
        RecyclerView recyclerItems;
        Button btnCancel;

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
        }
    }
}
