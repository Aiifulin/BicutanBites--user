package com.example.bicutanbites.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    private Context context;
    private List<Product> items;
    private boolean isGrid;
    private OnAddClickListener addListener;

    public ProductAdapter(Context context, List<Product> items, boolean isGrid, OnAddClickListener addListener) {
        this.context = context;
        this.items = new ArrayList<>(items);
        this.isGrid = isGrid;
        this.addListener = addListener;
    }

    // ---------------------- VIEW HOLDER ----------------------
    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title;
        TextView desc;      // NOTE: May be null in grid mode
        TextView price;
        Button btnAdd;

        public VH(View v) {
            super(v);
            img = v.findViewById(R.id.imgProduct);
            title = v.findViewById(R.id.tvTitle);
            price = v.findViewById(R.id.tvPrice);
            btnAdd = v.findViewById(R.id.btnAdd);

            // Some layouts (grid) don't have description field
            desc = v.findViewById(R.id.tvDescription);
        }
    }

    // ---------------------- VIEW TYPE LOGIC ----------------------
    @Override
    public int getItemViewType(int position) {
        return isGrid ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    // ---------------------- INFLATE CORRECT LAYOUT ----------------------
    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {

        int layout = (viewType == VIEW_TYPE_GRID)
                ? R.layout.item_product_grid
                : R.layout.item_product_list;

        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    // ---------------------- BIND DATA ----------------------
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final Product p = items.get(position);

        holder.title.setText(p.getTitle());
        holder.price.setText(String.format("₱%.0f", p.getPrice()));

        // Only bind description in list layout
        if (!isGrid && holder.desc != null) {
            holder.desc.setText(p.getDescription());
        }

        // Glide image load
        Glide.with(context)
                .load(p.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(holder.img);

        holder.btnAdd.setOnClickListener(v -> {
            if (addListener != null) addListener.onAddClicked(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------------------- EXTERNAL CONTROLS ----------------------
    public void setItems(List<Product> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    public void setViewMode(boolean grid) {
        this.isGrid = grid;
        notifyDataSetChanged();
    }

    public List<Product> getAllItems() {
        return items;
    }
}
