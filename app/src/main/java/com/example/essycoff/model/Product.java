package com.example.essycoff.model;

public class Product {
    private String id;
    private String name;
    private double price;
    private int stock;
    private String image_url;

    public Product() {}

    public Product(String name, double price, int stock, String image_url) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.image_url = image_url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }
}