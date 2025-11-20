package com.example.bicutanbites.models;

public class Order {
    private String orderId;
    private String date;
    private double totalPrice;
    private String status;

    public Order(String orderId, String date, double totalPrice, String status) {
        this.orderId = orderId;
        this.date = date;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDate() {
        return date;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }
}
