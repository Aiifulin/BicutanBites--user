package com.example.bicutanbites.models;

import com.google.firebase.firestore.DocumentId; // Import this

public class CheckoutItem {
    @DocumentId // This annotation automatically fills this field with the Firestore ID
    private String documentId;

    private String name;
    private String imageUrl;
    private int qty;
    private double price;

    public CheckoutItem() {}

    public CheckoutItem(String name, String imageUrl, int qty, double price) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.qty = qty;
        this.price = price;
    }

    public String getDocumentId() { return documentId; } // Getter for ID
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }
}