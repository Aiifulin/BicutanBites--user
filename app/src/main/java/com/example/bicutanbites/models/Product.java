package com.example.bicutanbites.models;

public class Product {
    private String id;
    private String title;
    private String category;
    private String description;
    private String imageUrl;
    private double price;

    // Empty constructor needed by Firestore
    public Product() { }

    public Product(String id, String title, String description, String imageUrl, double price, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.category = category;
    }
    public String getCategory() { return category; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public double getPrice() { return price; }

    public void setId(String id) { this.id = id; }
}
