package com.example.bicutanbites.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bicutanbites.R;
import com.example.bicutanbites.models.Product;
import com.example.bicutanbites.ui.ProductAdapter;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recycler;
    private ProductAdapter adapter;
    private List<Product> originalProducts = new ArrayList<>();
    private boolean isGrid = true;

    private EditText etSearch;
    private ImageButton btnList, btnGrid;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragmenthome, container, false);

        // Init views
        recycler = view.findViewById(R.id.recyclerProducts);
        etSearch = view.findViewById(R.id.etSearch);
        btnList = view.findViewById(R.id.btnList);
        btnGrid = view.findViewById(R.id.btnGrid);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Load data & setup RecyclerView
        loadSampleData();
        setupRecycler();
        setupToggles();
        setupSearch();
        setupSwipe();

        return view;
    }

    private void setupRecycler() {
        adapter = new ProductAdapter(getContext(), new ArrayList<>(originalProducts), isGrid, product ->
                Toast.makeText(getContext(), product.getTitle() + " added", Toast.LENGTH_SHORT).show()
        );
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
    }

    private void setupToggles() {
        updateToggleVisuals();
        btnList.setOnClickListener(v -> {
            if (isGrid) {
                isGrid = false;
                switchToList();
            }
        });

        btnGrid.setOnClickListener(v -> {
            if (!isGrid) {
                isGrid = true;
                switchToGrid();
            }
        });
    }

    private void updateToggleVisuals() {
        btnList.setAlpha(isGrid ? 0.6f : 1f);
        btnGrid.setAlpha(isGrid ? 1f : 0.6f);
    }

    private void switchToGrid() {
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter.setViewMode(true);
        updateToggleVisuals();
    }

    private void switchToList() {
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.setViewMode(false);
        updateToggleVisuals();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterProducts(s.toString());
            }
        });
    }

    private void setupSwipe() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadSampleData();
            adapter.setItems(originalProducts);
            swipeRefresh.setRefreshing(false);
        });
    }

    private void filterProducts(String q) {
        if (q == null || q.trim().isEmpty()) {
            adapter.setItems(originalProducts);
            return;
        }
        String query = q.toLowerCase();
        List<Product> filtered = new ArrayList<>();
        for (Product p : originalProducts) {
            if ((p.getTitle() != null && p.getTitle().toLowerCase().contains(query)) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(query))) {
                filtered.add(p);
            }
        }
        adapter.setItems(filtered);
    }

    private void loadSampleData() {
        originalProducts.clear();
        originalProducts.add(new Product("1", "Classic Burger", "Juicy beef patty with lettuce & tomato", "", 99.0));
        originalProducts.add(new Product("2", "Spicy Chicken", "Crispy spicy chicken with rice", "", 85.0));
        originalProducts.add(new Product("3", "Pandesal with Cheese", "Fresh pandesal stuffed with cheese", "", 35.0));
        originalProducts.add(new Product("4", "Halo-halo", "Refreshing mix of shaved ice & fruits", "", 70.0));
        originalProducts.add(new Product("5", "Sinigang Box", "Tamarind soup set - family size", "", 220.0));
    }
}
