package com.example.bicutanbites.utils;

import com.example.bicutanbites.models.FoodItem;
import com.example.bicutanbites.models.Order;

import java.util.ArrayList;
import java.util.List;

public class MockData {

    public static List<FoodItem> getFoodItems() {
        List<FoodItem> items = new ArrayList<>();
        items.add(new FoodItem("Beef Tapa", "Classic Filipino breakfast with garlic rice and egg", 120.0,
                "https://picsum.photos/400?random=1"));
        items.add(new FoodItem("Chicken Adobo", "Savory chicken stewed in soy sauce and vinegar", 100.0,
                "https://picsum.photos/400?random=2"));
        items.add(new FoodItem("Sinigang na Baboy", "Pork in sour tamarind broth with vegetables", 140.0,
                "https://picsum.photos/400?random=3"));
        items.add(new FoodItem("Pancit Canton", "Stir-fried noodles with vegetables and meat", 90.0,
                "https://picsum.photos/400?random=4"));
        items.add(new FoodItem("Burger Steak", "Juicy burger patties with mushroom gravy", 110.0,
                "https://picsum.photos/400?random=5"));
        items.add(new FoodItem("Halo-Halo", "Sweet dessert mix with shaved ice and milk", 80.0,
                "https://picsum.photos/400?random=6"));
        return items;
    }

    public static List<Order> getOrders() {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order("ORD-001", "2025-10-01", 320.0, "Delivered"));
        orders.add(new Order("ORD-002", "2025-10-03", 150.0, "Preparing"));
        orders.add(new Order("ORD-003", "2025-10-05", 480.0, "Delivered"));
        orders.add(new Order("ORD-004", "2025-10-10", 200.0, "Cancelled"));
        return orders;
    }
}
