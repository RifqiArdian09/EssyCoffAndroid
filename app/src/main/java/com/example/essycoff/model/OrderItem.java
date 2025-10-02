package com.example.essycoff.model;

public class OrderItem {
    private String order_id;
    private String product_id;
    private int qty;
    private double price;

    // Getters and Setters
    public String getOrder_id() { return order_id; }
    public void setOrder_id(String order_id) { this.order_id = order_id; }

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
