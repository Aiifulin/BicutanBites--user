package com.example.bicutanbites.models;

public class OrderItem {
    private String productName;
    private int quantity;
    private double price;
    private String imageUrl;

    public OrderItem(String productName, int quantity, double price, String imageUrl) {
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}