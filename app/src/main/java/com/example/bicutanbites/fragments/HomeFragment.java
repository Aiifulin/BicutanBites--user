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

import com.example.bicutanbites.CartHandler;
import com.example.bicutanbites.CheckoutActivity;
import com.example.bicutanbites.R;
import com.example.bicutanbites.models.Product;
import com.example.bicutanbites.ui.ProductAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private View btnCartContainer; // The FrameLayout container for the cart
    private TextView txtCartBadge; // The badge text view

    private SwipeRefreshLayout swipeRefresh;
    private CartHandler cartHandler;

    private TextView btnCatAll, btnCatSandwiches, btnCatSizzling, btnCatPastaNoodles,
            btnCatChickenMeats, btnCatAppetizers, btnCatNoodleSoup, btnCatFilipino,
            btnCatDesserts, btnCatBeverages;
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

        // Init Cart Views
        btnCartContainer = view.findViewById(R.id.cartButton); // The FrameLayout
        txtCartBadge = view.findViewById(R.id.txtCartBadge);   // The Badge TextView

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        btnCatAll = view.findViewById(R.id.btnCatAll);
        btnCatSandwiches = view.findViewById(R.id.btnCatSandwiches);
        btnCatSizzling = view.findViewById(R.id.btnCatSizzling);
        btnCatPastaNoodles = view.findViewById(R.id.btnCatPastaNoodles);
        btnCatChickenMeats = view.findViewById(R.id.btnCatChickenMeats);
        btnCatAppetizers = view.findViewById(R.id.btnCatAppetizers);
        btnCatNoodleSoup = view.findViewById(R.id.btnCatNoodleSoup);
        btnCatFilipino = view.findViewById(R.id.btnCatFilipino);
        btnCatDesserts = view.findViewById(R.id.btnCatDesserts);
        btnCatBeverages = view.findViewById(R.id.btnCatBeverages);

        // Initialize CartHandler
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            cartHandler = new CartHandler(userId);
        }

        // 1. Setup recycler
        setupRecycler();

        // 2. Load products
        loadProductsFromFirebase();

        // 3. Setup Cart Badge Listener (NEW)
        setupCartBadge();

        // 4. Setup Categories
        setupCategoryButtons();

        // 5. Other UI setups
        setupToggles();
        setupSearch();
        setupSwipe();

        // Cart button click listener
        // We put the listener on the Container so clicking anywhere on the icon/badge works
        btnCartContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CheckoutActivity.class);
            startActivity(intent);
        });

        return view;
    }

    // --- NEW METHOD: Real-time Cart Badge ---
    private void setupCartBadge() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            txtCartBadge.setVisibility(View.GONE);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("cart")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    int totalQty = 0;
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            Long qty = doc.getLong("qty");
                            if (qty != null) {
                                totalQty += qty.intValue();
                            }
                        }
                    }

                    if (totalQty > 0) {
                        txtCartBadge.setVisibility(View.VISIBLE);
                        txtCartBadge.setText(String.valueOf(totalQty));
                    } else {
                        txtCartBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupCategoryButtons() {
        View.OnClickListener listener = v -> {
            resetCategoryStyles();
            TextView tv = (TextView) v;

            // Map the button's ID to the exact Firestore category string (using '&')
            int id = v.getId();
            if (id == R.id.btnCatAll) {
                activeCategory = "All";
            } else if (id == R.id.btnCatSandwiches) {
                activeCategory = "Sandwiches & Rolls"; // Matches Firestore Key
            } else if (id == R.id.btnCatSizzling) {
                activeCategory = "Sizzling Specials";
            } else if (id == R.id.btnCatPastaNoodles) {
                activeCategory = "Pasta & Noodles"; // Matches Firestore Key
            } else if (id == R.id.btnCatChickenMeats) {
                activeCategory = "Chicken & Meats"; // Matches Firestore Key
            } else if (id == R.id.btnCatAppetizers) {
                activeCategory = "Appetizers/Sides"; // Matches Firestore Key
            } else if (id == R.id.btnCatNoodleSoup) {
                activeCategory = "Noodle Soup";
            } else if (id == R.id.btnCatFilipino) {
                activeCategory = "Filipino Classics";
            } else if (id == R.id.btnCatDesserts) {
                activeCategory = "Desserts";
            } else if (id == R.id.btnCatBeverages) {
                activeCategory = "Beverages";
            }

            tv.setBackgroundResource(R.drawable.category_chip_selected);
            tv.setTextColor(Color.WHITE);

            applyCategoryFilter();
        };

        // Apply the listener to all category buttons
        btnCatAll.setOnClickListener(listener);
        btnCatSandwiches.setOnClickListener(listener);
        btnCatSizzling.setOnClickListener(listener);
        btnCatPastaNoodles.setOnClickListener(listener);
        btnCatChickenMeats.setOnClickListener(listener);
        btnCatAppetizers.setOnClickListener(listener);
        btnCatNoodleSoup.setOnClickListener(listener);
        btnCatFilipino.setOnClickListener(listener);
        btnCatDesserts.setOnClickListener(listener);
        btnCatBeverages.setOnClickListener(listener);

        // Set 'All' as the default selected category on load
        btnCatAll.setBackgroundResource(R.drawable.category_chip_selected);
        btnCatAll.setTextColor(Color.WHITE);
    }

    private void applyCategoryFilter() {
        if (originalProducts == null || originalProducts.isEmpty()) {
            return;
        }

        if (activeCategory.equals("All")) {
            adapter.setItems(originalProducts);
            return;
        }

        List<Product> filtered = new ArrayList<>();

        for (Product p : originalProducts) {
            if (p.getCategory() != null &&
                    p.getCategory().toLowerCase().contains(activeCategory.toLowerCase())) {
                filtered.add(p);
            }
        }

        adapter.setItems(filtered);
    }

    private void resetCategoryStyles() {
        btnCatAll.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatSandwiches.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatSizzling.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatPastaNoodles.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatChickenMeats.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatAppetizers.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatNoodleSoup.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatFilipino.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatDesserts.setBackgroundResource(R.drawable.category_chip_bg);
        btnCatBeverages.setBackgroundResource(R.drawable.category_chip_bg);


        btnCatAll.setTextColor(Color.BLACK);
        btnCatSandwiches.setTextColor(Color.BLACK);
        btnCatSizzling.setTextColor(Color.BLACK);
        btnCatPastaNoodles.setTextColor(Color.BLACK);
        btnCatChickenMeats.setTextColor(Color.BLACK);
        btnCatAppetizers.setTextColor(Color.BLACK);
        btnCatNoodleSoup.setTextColor(Color.BLACK);
        btnCatFilipino.setTextColor(Color.BLACK);
        btnCatDesserts.setTextColor(Color.BLACK);
        btnCatBeverages.setTextColor(Color.BLACK);
    }

    private void setupRecycler() {
        adapter = new ProductAdapter(
                getContext(),
                new ArrayList<>(originalProducts),
                isGrid,
                product -> {
                    if (cartHandler != null) {
                        cartHandler.addToCart(product);
                        Toast.makeText(getContext(),
                                product.getTitle() + " added to cart", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "You must be logged in to add items", Toast.LENGTH_SHORT).show();
                    }
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
                    String id = doc.getId();
                    String name = doc.getString("name");
                    String desc = doc.getString("description");
                    String imageUrl = doc.getString("imageUrl");
                    Double price = doc.getDouble("price");
                    String category = doc.getString("category");
                    // NEW: Fetch the availability status (it's a Boolean in Firestore)
                    Boolean available = doc.getBoolean("available");

                    Product p = new Product(
                            id,
                            name,
                            desc,
                            imageUrl,
                            price != null ? price : 0,
                            category,
                            // NEW: Pass the status to the Product constructor
                            available != null ? available : true
                    );

                    originalProducts.add(p);
                }

                // FIXED: Call this AFTER the loop finishes
                applyCategoryFilter();

            } else {
                Toast.makeText(getContext(),
                        "Failed to load menu items", Toast.LENGTH_SHORT).show();
            }
            swipeRefresh.setRefreshing(false);
        });
    }
}