package com.example.bicutanbites.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bicutanbites.R;
import com.example.bicutanbites.models.CheckoutItem;

import java.util.List;
import java.util.Locale;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.ViewHolder> {

    private final Context context;
    private final List<CheckoutItem> items;
    private final CartActionListener listener;

    // Interface to communicate cart actions (increase, decrease, delete) back to the hosting activity/fragment
    public interface CartActionListener {
        void onIncrease(CheckoutItem item);
        void onDecrease(CheckoutItem item);
        void onDelete(CheckoutItem item);
    }

    public CheckoutAdapter(Context context, List<CheckoutItem> items, CartActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckoutItem item = items.get(position);

        // Bind product details and calculate total price for the quantity
        holder.productName.setText(item.getName());
        holder.productQuantity.setText(String.valueOf(item.getQty()));
        holder.productPrice.setText(String.format(Locale.getDefault(), "₱%.2f", item.getPrice() * item.getQty()));

        // Load image using Glide
        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.productImage);

        // Attach listeners to trigger cart actions via the interface
        holder.btnIncrease.setOnClickListener(v -> listener.onIncrease(item));
        holder.btnDecrease.setOnClickListener(v -> listener.onDecrease(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder class to hold references to all cart item views
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productQuantity, productPrice;
        TextView btnIncrease, btnDecrease;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productQuantity = itemView.findViewById(R.id.product_quantity);
            productPrice = itemView.findViewById(R.id.product_price);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}