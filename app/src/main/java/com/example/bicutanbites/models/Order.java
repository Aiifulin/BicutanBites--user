package com.example.bicutanbites.models;

import java.util.Date;
import java.util.List;

public class Order {
    private String orderId;
    private Date orderDate;
    private String status;
    private double total;
    private List<OrderItem> items;
    private String note; // <--- NEW FIELD


    // Update Constructor
    public Order(String orderId, Date orderDate, String status, double total, List<OrderItem> items, String note) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.status = status;
        this.total = total;
        this.items = items;
        this.note = note;
    }

    public void setStatus(String status) { //used to edit status
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public Date getOrderDate() { return orderDate; }
    public String getStatus() { return status; }
    public double getTotal() { return total; }
    public List<OrderItem> getItems() { return items; }
    public String getNote() { return note; } // <--- NEW GETTER
}