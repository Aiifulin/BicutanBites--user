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
        holder.price.setText("₱" + p.getPrice());

        if (holder.desc != null) {
            holder.desc.setText(p.getDescription());
        }

        Glide.with(context)
                .load(p.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .centerCrop()
                .into(holder.img);

        holder.btnAdd.setOnClickListener(v -> {
            listener.onAddClicked(p);
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    // -------------------------------- VIEW HOLDER --------------------------------
    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title, desc, price;
        Button btnAdd;

        public VH(View v) {
            super(v);
            img = v.findViewById(R.id.imgProduct);
            title = v.findViewById(R.id.tvTitle);
            desc = v.findViewById(R.id.tvDescription);
            price = v.findViewById(R.id.tvPrice);
            btnAdd = v.findViewById(R.id.btnAdd);
        }
    }

    public void setItems(List<Product> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }


}
