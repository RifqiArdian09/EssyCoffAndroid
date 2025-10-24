package com.example.essycoff.model;

import java.util.Date;

public class Order {
    private String id;
    private String order_number;
    private String customer_name;
    private double subtotal;
    private double cash;
    private double change;
    private String user_id;
    private Date created_at;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrder_number() { return order_number; }
    public void setOrder_number(String order_number) { this.order_number = order_number; }

    public String getCustomer_name() { return customer_name; }
    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getCash() { return cash; }
    public void setCash(double cash) { this.cash = cash; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public Date getCreated_at() { return created_at; }
    public void setCreated_at(Date created_at) { this.created_at = created_at; }
}

