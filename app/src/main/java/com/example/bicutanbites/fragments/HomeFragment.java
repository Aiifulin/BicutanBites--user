package com.example.bicutanbites.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recycler;
    private ProductAdapter adapter;
    private List<Product> originalProducts = new ArrayList<>();
    private boolean isGrid = false;

    private EditText etSearch;
    private ImageButton btnList, btnGrid;
    private SwipeRefreshLayout swipeRefresh;

    private FloatingActionButton fabCheckout;

    private TextView btnCatAll, btnCatBurger, btnCatPasta, btnCatBeverage;
    private String activeCategory = "All";

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
        btnCatAll = view.findViewById(R.id.btnCatAll);
        btnCatBurger = view.findViewById(R.id.btnCatBurger);
        btnCatPasta = view.findViewById(R.id.btnCatPasta);
        btnCatBeverage = view.findViewById(R.id.btnCatBeverage);


        // 1. Setup recycler first (CREATES adapter)
        setupRecycler();

        // 2. Load products → then adapter.setItems() works safely
        loadProductsFromFirebase();

        // 3. NOW it's safe to install category buttons
        setupCategoryButtons();

        // 4. Other UI setups
        setupToggles();
        setupSearch();
        setupSwipe();


        return view;
    }

    private void setupCategoryButtons() {

        View.OnClickListener listener = v -> {
            resetCategoryStyles();

            TextView tv = (TextView) v;
            activeCategory = tv.getText().toString();

            tv.setBackgroundResource(R.drawable.category_chip_selected);
            tv.setTextColor(Color.WHITE);

            applyCategoryFilter();
        };

        btnCatAll.setOnClickListener(listener);
        btnCatBurger.setOnClickListener(listener);
        btnCatPasta.setOnClickListener(listener);
        btnCatBeverage.setOnClickListener(listener);

        // Default select ALL
        btnCatAll.performClick();
    }

    private void applyCategoryFilter() {

        if (activeCategory.equals("All")) {
            adapter.setItems(originalProducts);
            return;
        }

        List<Product> filtered = new ArrayList<>();

        for (Product p : originalProducts) {
            if (p.getCategory() != null &&
                    p.getCategory().equalsIgnoreCase(activeCategory)) {
                filtered.add(p);
            }
        }

        adapter.setItems(filtered);
    }

    private void resetCategoryStyles() {
        btnCatAll.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatBurger.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatPasta.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatBeverage.setBackgroundResource(R.drawable.category_chip_bg);

        btnCatAll.setTextColor(Color.BLACK);
        btnCatBurger.setTextColor(Color.BLACK);
        btnCatPasta.setTextColor(Color.BLACK);
        btnCatBeverage.setTextColor(Color.BLACK);
    }

    private void setupRecycler() {
        adapter = new ProductAdapter(
                getContext(),
                new ArrayList<>(originalProducts),
                isGrid,
                product -> {
                    Toast.makeText(getContext(),
                            product.getTitle() + " added to cart", Toast.LENGTH_SHORT).show();
                }
        );

        recycler.setAdapter(adapter);

        if (isGrid) {
            recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        }
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
            loadProductsFromFirebase();
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

    private void loadProductsFromFirebase() {
        swipeRefresh.setRefreshing(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference menuRef = db.collection("menu_items");

        originalProducts.clear();

        menuRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {

                    // Extract Firestore fields
                    String id = doc.getId();
                    String name = doc.getString("name");
                    String desc = doc.getString("description");
                    String imageUrl = doc.getString("imageUrl");
                    Double price = doc.getDouble("price");
                    String category = doc.getString("category");

                    // Convert to Product
                    Product p = new Product(
                            id,
                            name,
                            desc,
                            imageUrl,
                            price != null ? price : 0,
                            category
                    );

                    originalProducts.add(p);
                }

                adapter.setItems(originalProducts);

            } else {
                Toast.makeText(getContext(),
                        "Failed to load menu items", Toast.LENGTH_SHORT).show();
            }
            swipeRefresh.setRefreshing(false);
        });
    }

}
