package com.example.bicutanbites.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Changed from ImageView
import android.widget.TextView;
import android.widget.ImageView;

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
    private final CartActionListener listener; // Interface listener

    // 1. Define Interface
    public interface CartActionListener {
        void onIncrease(CheckoutItem item);
        void onDecrease(CheckoutItem item);
    }

    // 2. Update Constructor
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

        String name = item.getName();
        String imageUrl = item.getImageUrl();
        int qty = item.getQty();
        double price = item.getPrice();

        holder.productName.setText(name);
        holder.productQuantity.setText(String.valueOf(qty)); // Just the number now
        holder.productPrice.setText(String.format(Locale.getDefault(), "₱%.2f", price * qty));

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(holder.productImage);

        // 3. Set Click Listeners
        holder.btnIncrease.setOnClickListener(v -> listener.onIncrease(item));
        holder.btnDecrease.setOnClickListener(v -> listener.onDecrease(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productQuantity, productPrice;
        ImageButton btnIncrease, btnDecrease; // Add buttons

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productQuantity = itemView.findViewById(R.id.product_quantity);
            productPrice = itemView.findViewById(R.id.product_price);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
        }
    }
}