package com.example.bicutanbites.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bicutanbites.R;
import com.example.bicutanbites.models.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface OnAddClickListener {
        void onAddClicked(Product product);
    }

    private Context context;
    private List<Product> items;
    private List<Product> fullList; // For search
    private boolean isGrid;
    private OnAddClickListener listener;

    public ProductAdapter(Context context, List<Product> items, boolean isGrid,
                          OnAddClickListener listener) {
        this.context = context;
        this.items = items;
        this.fullList = new ArrayList<>(items);
        this.isGrid = isGrid;
        this.listener = listener;
    }

    public void setViewMode(boolean grid) {
        this.isGrid = grid;
        notifyDataSetChanged();
    }

    // -------------------------------- FILTER --------------------------------
    public void filter(String text) {
        items.clear();
        if (text.isEmpty()) {
            items.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for (Product p : fullList) {
                if (p.getTitle().toLowerCase().contains(text)) {
                    items.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isGrid ? 1 : 0;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = (viewType == 1)
                ? R.layout.item_product_grid
                : R.layout.item_product_list;

        View v = LayoutInflater.from(context).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Product p = items.get(position);

        holder.title.setText(p.getTitle());
        // Updated to use PHP symbol
        holder.price.setText("₱" + p.getPrice());

        // NEW: Set the category text dynamically
        if (holder.tvCategory != null) {
            holder.tvCategory.setText(p.getCategory());
        }

        if (holder.desc != null) {
            holder.desc.setText(p.getDescription());
        }

        Glide.with(context)
                .load(p.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .centerCrop()
                .into(holder.img);

        // --- FIXED LOGIC: Check availability and handle UI state ---
        if (p.isItemAvailable()) {
            // Product is available: Set everything to normal
            holder.itemView.setAlpha(1.0f);
            holder.btnAdd.setEnabled(true);
            holder.btnAdd.setText("Add");

            // Re-set the normal click listener
            holder.btnAdd.setOnClickListener(v -> {
                listener.onAddClicked(p);
            });
            // Clear any general click listener on the item view (if set when unavailable)
            holder.itemView.setOnClickListener(null);
        } else {
            // Product is NOT available: Gray out and disable interaction
            holder.itemView.setAlpha(0.5f); // Gray out the entire item view
            holder.btnAdd.setEnabled(false); // Disable the add button
            holder.btnAdd.setText("Sold Out");

            // Clear the listener for the button
            holder.btnAdd.setOnClickListener(null);

            // Add a friendly message if the user taps the grayed-out item
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(context, p.getTitle() + " is currently sold out.", Toast.LENGTH_SHORT).show();
            });
        }
        // --- END FIXED LOGIC ---
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    // -------------------------------- VIEW HOLDER --------------------------------
    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title, desc, price, tvCategory;
        Button btnAdd;

        public VH(View v) {
            super(v);
            img = v.findViewById(R.id.imgProduct);
            title = v.findViewById(R.id.tvTitle);
            desc = v.findViewById(R.id.tvDescription);
            price = v.findViewById(R.id.tvPrice);
            btnAdd = v.findViewById(R.id.btnAdd);
            tvCategory = v.findViewById(R.id.tvCategory);
            // Removed: tvSoldOut initialization
        }
    }

    public void setItems(List<Product> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }
}